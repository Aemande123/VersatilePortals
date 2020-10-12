package mrp_v2.versatileportals.client.util;

import mrp_v2.versatileportals.VersatilePortals;
import mrp_v2.versatileportals.client.gui.screen.PortalControllerScreen;
import mrp_v2.versatileportals.client.particle.PortalControllerParticle;
import mrp_v2.versatileportals.client.particle.PortalParticle;
import mrp_v2.versatileportals.client.renderer.color.ExistingWorldControlItemColorer;
import mrp_v2.versatileportals.client.renderer.color.PortalColorer;
import mrp_v2.versatileportals.util.ObjectHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.particle.ParticleManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD, modid = VersatilePortals.ID)
public class RegistryHandler
{
    public static void init()
    {
        ScreenManager.registerFactory(ObjectHolder.PORTAL_CONTROLLER_CONTAINER_TYPE, PortalControllerScreen::new);
    }

    @SubscribeEvent public static void registerBlockColors(final ColorHandlerEvent.Block event)
    {
        event.getBlockColors()
                .register(PortalColorer.INSTANCE, ObjectHolder.PORTAL_BLOCK, ObjectHolder.PORTAL_FRAME_BLOCK,
                        ObjectHolder.PORTAL_CONTROLLER_BLOCK);
    }

    @SubscribeEvent public static void registerItemColors(final ColorHandlerEvent.Item event)
    {
        event.getItemColors()
                .register(ExistingWorldControlItemColorer.INSTANCE, ObjectHolder.EXISTING_WORLD_TELEPORT_ITEM);
    }

    @SuppressWarnings("resource") @SubscribeEvent
    public static void registerParticles(final ParticleFactoryRegisterEvent event)
    {
        ParticleManager particleManager = Minecraft.getInstance().particles;
        particleManager.registerFactory(ObjectHolder.PORTAL_PARTICLE_TYPE, PortalParticle.Factory::new);
        particleManager.registerFactory(ObjectHolder.PORTAL_CONTROLLER_PARTICLE_TYPE,
                PortalControllerParticle.Factory::new);
    }
}