package mrp_v2.versatileportals.client.util;

import mrp_v2.versatileportals.VersatilePortals;
import mrp_v2.versatileportals.client.renderer.tileentity.PortalControllerTileEntityRenderer;
import mrp_v2.versatileportals.util.ObjectHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD, modid = VersatilePortals.ID)
public class EventHandler
{
    @SubscribeEvent public static void clientSetup(final FMLClientSetupEvent event)
    {
        RenderTypeLookup.setRenderLayer(ObjectHolder.PORTAL_BLOCK, RenderType.getTranslucent());
        VersatilePortals.WORLD_SUPPLIER = () -> Minecraft.getInstance().world;
        ClientRegistry.bindTileEntityRenderer(ObjectHolder.PORTAL_CONTROLLER_TILE_ENTITY_TYPE,
                PortalControllerTileEntityRenderer::new);
    }
}