package mrp_v2.randomdimensions.client.util;

import mrp_v2.randomdimensions.RandomDimensions;
import mrp_v2.randomdimensions.client.particle.PortalParticle;
import mrp_v2.randomdimensions.client.renderer.color.PortalColorer;
import mrp_v2.randomdimensions.util.ObjectHolder;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD, modid = RandomDimensions.ID)
public class RegistryHandler {

	@SubscribeEvent
	public static void registerBlockColors(final ColorHandlerEvent.Block event) {
		event.getBlockColors().register(PortalColorer.INSTANCE, ObjectHolder.PORTAL_BLOCK);
	}

	@SuppressWarnings("resource")
	@SubscribeEvent
	public static void registerParticles(final ParticleFactoryRegisterEvent event) {
		Minecraft.getInstance().particles.registerFactory(ObjectHolder.PORTAL_PARTICLE, PortalParticle.Factory::new);
	}
}
