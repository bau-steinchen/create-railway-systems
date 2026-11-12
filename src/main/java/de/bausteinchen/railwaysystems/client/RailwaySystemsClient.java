package de.bausteinchen.railwaysystems.client;

import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.registry.ModBlockEntities;
import de.bausteinchen.railwaysystems.registry.ModBlocks;
import de.bausteinchen.railwaysystems.signal.LinkedSignal;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = RailwaySystems.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RailwaySystemsClient {

	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(ModBlockEntities.LINKED_SIGNAL.get(), SignalOverlayRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.SIGNAL_LINK.get(), SmartBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.TRAFFIC_LIGHT.get(), TrafficMastRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.CROSSWALK_MAST.get(), TrafficMastRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.CROSSING_LIGHT.get(), SmartBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.CROSSING_BARRIER.get(), CrossingBarrierRenderer::new);
	}

	@SubscribeEvent
	public static void registerOverlayModels(ModelEvent.RegisterAdditional event) {
		ModBlocks.BLOCKS.getEntries()
			.forEach(entry -> {
				if (entry.get() instanceof LinkedSignal signal)
					SignalOverlayRenderer.getModels(entry.get(), signal.getFeatures())
						.forEach(location -> event.register(ModelResourceLocation.standalone(location)));
			});
		event.register(ModelResourceLocation.standalone(SignalOverlayRenderer.FORM_ARM));
		event.register(ModelResourceLocation.standalone(CrossingBarrierRenderer.BOOM));
		event.register(ModelResourceLocation.standalone(CrossingBarrierRenderer.BOOM_TIP));
		TrafficMastRenderer.getModels()
			.forEach(location -> event.register(ModelResourceLocation.standalone(location)));
	}

}
