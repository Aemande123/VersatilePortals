package mrp_v2.randomdimensions.network;

import mrp_v2.randomdimensions.RandomDimensions;
import mrp_v2.randomdimensions.block.PortalBlock;
import mrp_v2.randomdimensions.block.PortalFrameBlock;
import mrp_v2.randomdimensions.tileentity.PortalControllerTileEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.List;
import java.util.function.Supplier;

public class Packet
{

    public static class Handler
    {

        public static final String ID = "portal_color";
        private static final String PROTOCOL_VERSION = "1";
        public static SimpleChannel INSTANCE;

        private static int id = 0;

        public static void createChannel()
        {
            INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(RandomDimensions.ID, ID),
                    () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
            INSTANCE.registerMessage(id++, PortalControllerScreenClosed.class, PortalControllerScreenClosed::encode,
                    PortalControllerScreenClosed::new, PortalControllerScreenClosed::handle);
            INSTANCE.registerMessage(id++, PortalFrameUpdate.class, PortalFrameUpdate::encode, PortalFrameUpdate::new,
                    PortalFrameUpdate::handle);
        }
    }

    public static class PortalFrameUpdate
    {
        private final BlockPos pos;
        private final List<PortalBlock.Size> sizes;

        public PortalFrameUpdate(BlockPos pos, List<PortalBlock.Size> sizes)
        {
            this.pos = pos;
            this.sizes = sizes;
        }

        public PortalFrameUpdate(PacketBuffer buffer)
        {
            this.pos = buffer.readBlockPos();
            this.sizes = PortalBlock.Size.readListFromBuffer(buffer);
        }

        public void encode(PacketBuffer buffer)
        {
            buffer.writeBlockPos(this.pos);
            PortalBlock.Size.writeListToBuffer(this.sizes, buffer);
        }

        public void handle(Supplier<NetworkEvent.Context> context)
        {
            context.get().enqueueWork(() ->
            {
                World world = RandomDimensions.WORLD_SUPPLIER.get();
                if (world != null)
                {
                    if (world.isBlockLoaded(this.pos))
                    {
                        PortalFrameBlock.updatePortals(this.sizes);
                    }
                }
            });
            context.get().setPacketHandled(true);
        }
    }

    public static class PortalControllerScreenClosed
    {
        private final int portalColor;
        private final BlockPos pos;

        public PortalControllerScreenClosed(int portalColor, BlockPos pos)
        {
            this.portalColor = portalColor;
            this.pos = pos;
        }

        public PortalControllerScreenClosed(PacketBuffer buffer)
        {
            this.portalColor = buffer.readInt();
            this.pos = buffer.readBlockPos();
        }

        public void encode(PacketBuffer buffer)
        {
            buffer.writeInt(this.portalColor);
            buffer.writeBlockPos(this.pos);
        }

        @SuppressWarnings("deprecation") public void handle(Supplier<NetworkEvent.Context> context)
        {
            context.get().enqueueWork(() ->
            {
                ServerPlayerEntity player = context.get().getSender();
                if (player.world.isBlockLoaded(this.pos))
                {
                    PortalControllerTileEntity portalController =
                            (PortalControllerTileEntity) player.world.getTileEntity(this.pos);
                    if (portalController != null)
                    {
                        portalController.onScreenClosed(this.portalColor);
                    }
                }
            });
            context.get().setPacketHandled(true);
        }
    }
}
