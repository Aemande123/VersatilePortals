package mrp_v2.versatileportals.world;

import mrp_v2.versatileportals.VersatilePortals;
import mrp_v2.versatileportals.block.PortalBlock;
import mrp_v2.versatileportals.block.PortalControllerBlock;
import mrp_v2.versatileportals.block.util.PortalSize;
import mrp_v2.versatileportals.common.capabilities.IPortalDataCapability;
import mrp_v2.versatileportals.datagen.EN_USTranslationGenerator;
import mrp_v2.versatileportals.item.IPortalControlItem;
import mrp_v2.versatileportals.tileentity.PortalControllerTileEntity;
import mrp_v2.versatileportals.util.Util;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.WorldTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@EventBusSubscriber public class EventHandler
{
    public static final TranslationTextComponent noPortalController;
    public static final TranslationTextComponent noControlItem;
    public static final TranslationTextComponent teleported;
    public static final Function<Object[], TranslationTextComponent> teleportingInFunction;

    static
    {
        noPortalController = EN_USTranslationGenerator.makeTextTranslation("block.", VersatilePortals.ID,
                "." + PortalBlock.ID + ".message.noPortalController", "en_us",
                "A Portal Controller could not be found for this portal");
        teleportingInFunction = EN_USTranslationGenerator.makeFormattedTextTranslation("block.", VersatilePortals.ID,
                "." + PortalControllerBlock.ID + ".message.teleportingIn", "en_us", "Teleporting in %s...");
        teleported = EN_USTranslationGenerator.makeTextTranslation("block.", VersatilePortals.ID,
                "." + PortalControllerBlock.ID + ".message.teleported", "en_us", "Teleported");
        noControlItem = EN_USTranslationGenerator.makeTextTranslation("block.", VersatilePortals.ID,
                "." + PortalControllerBlock.ID + ".message.noControlItem", "en_us",
                "There is no control item or it is invalid");
    }

    @SubscribeEvent public static void worldTick(final WorldTickEvent event)
    {
        if (event.phase != TickEvent.Phase.START)
        {
            return;
        }
        if (event.world instanceof ServerWorld)
        {
            ServerWorld world = (ServerWorld) event.world;
            Set<Entity> relevantEntities = world.getEntities().filter(entity -> entity != null && entity.isAlive())
                    .collect(Collectors.toSet());
            for (Entity entity : relevantEntities)
            {
                IPortalDataCapability portalData = Util.getPortalData(entity);
                if (portalData.getInPortal())
                {
                    portalData.setInPortal(false);
                    if (portalData.getRemainingPortalCooldown() > 0)
                    {
                        portalData.setRemainingPortalCooldown(entity.getPortalCooldown());
                        return;
                    }
                    if (portalData.incrementInPortalTime() < entity.getMaxInPortalTime())
                    {
                        if (entity.getMaxInPortalTime() > 1)
                        {
                            if (entity instanceof ServerPlayerEntity)
                            {
                                int remainingInPortalTime = entity.getMaxInPortalTime() - portalData.getInPortalTime();
                                Util.sendMessage((ServerPlayerEntity) entity, teleportingInFunction
                                        .apply(new Object[]{Math.ceil(remainingInPortalTime / 2.0F) / 10.0F}));
                            }
                        }
                    } else
                    {
                        PortalSize portalSize = new PortalSize(world, portalData.getPortalPos(),
                                world.getBlockState(portalData.getPortalPos())
                                        .get(BlockStateProperties.HORIZONTAL_AXIS));
                        PortalControllerTileEntity controller = portalSize.getPortalController(world).getLeft();
                        if (controller == null)
                        {
                            if (entity instanceof ServerPlayerEntity)
                            {
                                Util.sendMessage((ServerPlayerEntity) entity, noPortalController);
                            }
                            return;
                        }
                        ItemStack portalControlItemStack = controller.getControlItemStack();
                        IPortalControlItem portalControlItem = null;
                        if (!portalControlItemStack.isEmpty())
                        {
                            if (portalControlItemStack.getItem() instanceof IPortalControlItem)
                            {
                                portalControlItem = (IPortalControlItem) portalControlItemStack.getItem();
                            }
                        }
                        if (portalControlItem == null)
                        {
                            if (entity instanceof ServerPlayerEntity)
                            {
                                Util.sendMessage((ServerPlayerEntity) entity, noControlItem);
                            }
                            return;
                        }
                        Entity teleportedEntity =
                                portalControlItem.teleportEntity(entity, world, portalSize, portalControlItemStack);
                        if (teleportedEntity instanceof ServerPlayerEntity)
                        {
                            Util.sendMessage((ServerPlayerEntity) teleportedEntity, teleported);
                        }
                    }
                } else
                {
                    portalData.decrementRemainingPortalCooldown();
                    portalData.setInPortalTime(0);
                }
            }
        }
    }
}
