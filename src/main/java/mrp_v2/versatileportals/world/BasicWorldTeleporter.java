package mrp_v2.versatileportals.world;

import mrp_v2.versatileportals.block.PortalControllerBlock;
import mrp_v2.versatileportals.block.util.PortalSize;
import mrp_v2.versatileportals.common.capabilities.IPortalDataCapability;
import mrp_v2.versatileportals.item.ExistingWorldControlItem;
import mrp_v2.versatileportals.tileentity.PortalControllerTileEntity;
import mrp_v2.versatileportals.util.ObjectHolder;
import mrp_v2.versatileportals.util.Util;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PortalInfo;
import net.minecraft.entity.Entity;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.TeleportationRepositioner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.village.PointOfInterest;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.world.DimensionType;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

public class BasicWorldTeleporter implements ITeleporter
{
    protected final ServerWorld destinationWorld;
    protected final ServerWorld originWorld;
    protected final PortalSize originPortalSize;

    public BasicWorldTeleporter(ServerWorld destinationWorld, ServerWorld originWorld, PortalSize originPortalSize)
    {
        this.destinationWorld = destinationWorld;
        this.originWorld = originWorld;
        this.originPortalSize = originPortalSize;
    }

    @Nullable @Override
    public Entity placeEntity(Entity entityIn, ServerWorld currentWorld, ServerWorld destinationWorld, float yaw,
            Function<Boolean, Entity> repositionEntity)
    {
        Entity repositionedEntity = repositionEntity.apply(false); //don't let vanilla make a portal
        IPortalDataCapability portalData = Util.getPortalData(repositionedEntity);
        portalData.setRemainingPortalCooldown(repositionedEntity.getPortalCooldown());
        portalData.setInPortalTime(0);
        return repositionedEntity;
    }

    @Nullable @Override
    public PortalInfo getPortalInfo(Entity entity, ServerWorld destWorld,
            Function<ServerWorld, PortalInfo> defaultPortalInfo)
    {
        return this.getPortalInfo(entity);
    }

    @Nullable private PortalInfo getPortalInfo(Entity entity)
    {
        WorldBorder worldborder = this.destinationWorld.getWorldBorder();
        double minX = Math.max(-2.9999872E7D, worldborder.minX() + 16.0D);
        double minZ = Math.max(-2.9999872E7D, worldborder.minZ() + 16.0D);
        double maxX = Math.min(2.9999872E7D, worldborder.maxX() - 16.0D);
        double maxZ = Math.min(2.9999872E7D, worldborder.maxZ() - 16.0D);
        double coordinateMultiplier = DimensionType.getCoordinateDifference(entity.world.getDimensionType(),
                this.destinationWorld.getDimensionType());
        BlockPos pos =
                new BlockPos(MathHelper.clamp(entity.getPosX() * coordinateMultiplier, minX, maxX), entity.getPosY(),
                        MathHelper.clamp(entity.getPosZ() * coordinateMultiplier, minZ, maxZ));
        return this.teleportExistingPortal(pos, coordinateMultiplier)
                .map((result) -> this.convertTeleportResult(result, entity))
                .orElseGet(() -> this.teleportNewPortal(pos)
                        .map((result) -> this.convertTeleportResult(result, entity))
                        .orElse(null));
    }

    private PortalInfo convertTeleportResult(TeleportationRepositioner.Result result, Entity entity)
    {
        Direction.Axis axis = this.originPortalSize.getAxis();
        Vector3d vector3d = new Vector3d(0.5D, 0.0D, 0.0D);
        return net.minecraft.block.PortalSize.func_242963_a(this.destinationWorld, result, axis, vector3d,
                entity.getSize(entity.getPose()), entity.getMotion(), entity.rotationYaw, entity.rotationPitch);
    }

    public Optional<TeleportationRepositioner.Result> teleportExistingPortal(BlockPos searchOrigin,
            double coordinateMultiplier)
    {
        PointOfInterestManager pointOfInterestManager = this.destinationWorld.getPointOfInterestManager();
        int i = coordinateMultiplier == 0.125D ? 16 : 128;
        pointOfInterestManager.ensureLoadedAndValid(this.destinationWorld, searchOrigin, i);
        Optional<PointOfInterest> optionalPointOfInterest = pointOfInterestManager.getInSquare(
                (pointOfInterestType) -> pointOfInterestType == ObjectHolder.PORTAL_POINT_OF_INTEREST_TYPE.get(),
                searchOrigin, i, PointOfInterestManager.Status.ANY)
                .sorted(Comparator.<PointOfInterest>comparingDouble(
                        (pointOfInterest) -> pointOfInterest.getPos().distanceSq(searchOrigin)).thenComparingInt(
                        (pointOfInterest) -> pointOfInterest.getPos().getY()))
                .filter((pointOfInterest) -> this.destinationWorld.getBlockState(pointOfInterest.getPos())
                        .hasProperty(BlockStateProperties.HORIZONTAL_AXIS))
                .findFirst();
        return optionalPointOfInterest.map((pointOfInterest) ->
        {
            BlockPos poiPos = pointOfInterest.getPos();
            this.destinationWorld.getChunkProvider().registerTicket(TicketType.PORTAL, new ChunkPos(poiPos), 3, poiPos);
            BlockState poiState = this.destinationWorld.getBlockState(poiPos);
            return TeleportationRepositioner.findLargestRectangle(poiPos,
                    poiState.get(BlockStateProperties.HORIZONTAL_AXIS), PortalSize.MAX_WIDTH, Direction.Axis.Y, 21,
                    (blockState2) -> this.destinationWorld.getBlockState(blockState2) == poiState);
        });
    }

    public Optional<TeleportationRepositioner.Result> teleportNewPortal(BlockPos pos)
    {
        Direction positiveAxisDir =
                Direction.getFacingFromAxis(Direction.AxisDirection.POSITIVE, this.originPortalSize.getAxis());
        double availablePortalLocDistance = -1.0D;
        BlockPos availablePortalLoc = null;
        double partiallyAvailablePortalLocDistance = -1.0D;
        BlockPos partiallyAvailablePortalLoc = null;
        WorldBorder worldBorder = this.destinationWorld.getWorldBorder();
        int maxPortalGenerationHeight = this.destinationWorld.func_234938_ad_() - 1;
        BlockPos.Mutable mutableOriginPos = pos.toMutable();
        for (BlockPos.Mutable testPos : BlockPos.func_243514_a(pos, 16, Direction.EAST, Direction.SOUTH))
        {
            int maxPortalGenerationHeightForPos = Math.min(maxPortalGenerationHeight,
                    this.destinationWorld.getHeight(Heightmap.Type.MOTION_BLOCKING, testPos.getX(), testPos.getZ()));
            if (worldBorder.contains(testPos) && worldBorder.contains(testPos.move(positiveAxisDir, 1)))
            {
                testPos.move(positiveAxisDir.getOpposite(), 1);
                for (int availableYSpaceBottom = maxPortalGenerationHeightForPos;
                     availableYSpaceBottom >= 0;
                     availableYSpaceBottom--)
                {
                    testPos.setY(availableYSpaceBottom);
                    if (this.destinationWorld.isAirBlock(testPos))
                    {
                        int availableYSpaceTop;
                        for (availableYSpaceTop = availableYSpaceBottom;
                             availableYSpaceBottom > 0 &&
                                     this.destinationWorld.isAirBlock(testPos.move(Direction.DOWN));
                             availableYSpaceBottom--)
                        {
                        }
                        if (availableYSpaceBottom + this.originPortalSize.getHeight() + 1 <= maxPortalGenerationHeight)
                        {
                            int availableYSpace = availableYSpaceTop - availableYSpaceBottom;
                            if (availableYSpace <= 0 || availableYSpace >= this.originPortalSize.getWidth() + 1)
                            {
                                testPos.setY(availableYSpaceBottom);
                                if (this.isSpaceEmpty(testPos, mutableOriginPos, positiveAxisDir, 0))
                                {
                                    double distance = pos.distanceSq(testPos);
                                    if (this.isSpaceEmpty(testPos, mutableOriginPos, positiveAxisDir, -1) &&
                                            this.isSpaceEmpty(testPos, mutableOriginPos, positiveAxisDir, 1) &&
                                            (availablePortalLocDistance == -1.0D ||
                                                    availablePortalLocDistance > distance))
                                    {
                                        availablePortalLocDistance = distance;
                                        availablePortalLoc = testPos.toImmutable();
                                    }
                                    if (availablePortalLocDistance == -1.0D &&
                                            (partiallyAvailablePortalLocDistance == -1.0D ||
                                                    partiallyAvailablePortalLocDistance > distance))
                                    {
                                        partiallyAvailablePortalLocDistance = distance;
                                        partiallyAvailablePortalLoc = testPos.toImmutable();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (availablePortalLocDistance == -1.0D && partiallyAvailablePortalLocDistance != -1.0D)
        {
            availablePortalLoc = partiallyAvailablePortalLoc;
            availablePortalLocDistance = partiallyAvailablePortalLocDistance;
        }
        int floorWidth = 2;
        int portalWidth = this.originPortalSize.getWidth();
        int portalHeight = this.originPortalSize.getHeight();
        if (availablePortalLocDistance == -1.0D)
        {
            availablePortalLoc = (new BlockPos(pos.getX(),
                    MathHelper.clamp(pos.getY(), 70, this.destinationWorld.func_234938_ad_() - 10),
                    pos.getZ())).toImmutable();
            Direction rotated = positiveAxisDir.rotateY();
            if (!worldBorder.contains(availablePortalLoc))
            {
                return Optional.empty();
            }
            for (int floorXZ = -1; floorXZ < floorWidth; floorXZ++) // clears an area and creates the floor
            {
                for (int portalXZ = 0; portalXZ < portalWidth; portalXZ++)
                {
                    for (int y = -1; y < portalHeight; y++)
                    {
                        BlockState blockState = y < 0 ?
                                ObjectHolder.PORTAL_FRAME_BLOCK.get().getDefaultState() :
                                Blocks.AIR.getDefaultState();
                        mutableOriginPos.setAndOffset(availablePortalLoc,
                                portalXZ * positiveAxisDir.getXOffset() + floorXZ * rotated.getXOffset(), y,
                                portalXZ * positiveAxisDir.getZOffset() + floorXZ * rotated.getZOffset());
                        this.destinationWorld.setBlockState(mutableOriginPos, blockState);
                    }
                }
            }
        }
        for (int xz = -1; xz < portalWidth + 1; xz++) // creates frame
        {
            for (int y = -1; y < portalHeight + 1; y++)
            {
                if (xz == -1 | xz == portalWidth || y == -1 || y == portalHeight)
                {
                    mutableOriginPos.setAndOffset(availablePortalLoc, xz * positiveAxisDir.getXOffset(), y,
                            xz * positiveAxisDir.getZOffset());
                    if (this.originPortalSize.getPortalControllerRelativePos()
                            .equals(new BlockPos(xz * positiveAxisDir.getXOffset(), y,
                                    xz * positiveAxisDir.getZOffset())))
                    {
                        this.destinationWorld.setBlockState(mutableOriginPos, ObjectHolder.PORTAL_CONTROLLER_BLOCK.get()
                                .getDefaultState()
                                .with(PortalControllerBlock.AXIS, positiveAxisDir.rotateY().getAxis()), 3);
                        PortalControllerTileEntity portalControllerTileEntity =
                                (PortalControllerTileEntity) this.destinationWorld.getTileEntity(mutableOriginPos);
                        portalControllerTileEntity.getInventory()
                                .insertItem(0, ExistingWorldControlItem.getItemForWorld(this.originWorld), false);
                        portalControllerTileEntity.setPortalColor(
                                this.originPortalSize.getPortalController(this.originWorld).getLeft().getPortalColor());
                    } else
                    {
                        this.destinationWorld.setBlockState(mutableOriginPos,
                                ObjectHolder.PORTAL_FRAME_BLOCK.get().getDefaultState(), 3);
                    }
                }
            }
        }
        BlockState portalBlockState = ObjectHolder.PORTAL_BLOCK.get()
                .getDefaultState()
                .with(BlockStateProperties.HORIZONTAL_AXIS, this.originPortalSize.getAxis());
        for (int xz = 0; xz < portalWidth; xz++) // creates portal
        {
            for (int y = 0; y < portalHeight; y++)
            {
                mutableOriginPos.setAndOffset(availablePortalLoc, xz * positiveAxisDir.getXOffset(), y,
                        xz * positiveAxisDir.getZOffset());
                this.destinationWorld.setBlockState(mutableOriginPos, portalBlockState, 18);
            }
        }
        return Optional.of(
                new TeleportationRepositioner.Result(availablePortalLoc.toImmutable(), portalWidth, portalHeight));
    }

    private boolean isSpaceEmpty(BlockPos origin, BlockPos.Mutable mutable, Direction axis, int offset)
    {
        Direction rotated = axis.rotateY();
        for (int i = -1; i < this.originPortalSize.getWidth(); i++)
        {
            for (int j = -1; j < this.originPortalSize.getHeight(); j++)
            {
                mutable.setAndOffset(origin, axis.getXOffset() * i + rotated.getXOffset() * offset, j,
                        axis.getZOffset() * i + rotated.getZOffset() * offset);
                if (j < 0 && !this.destinationWorld.getBlockState(mutable).getMaterial().isSolid())
                {
                    return false;
                }
                if (j >= 0 && !this.destinationWorld.isAirBlock(mutable))
                {
                    return false;
                }
            }
        }
        return true;
    }
}
