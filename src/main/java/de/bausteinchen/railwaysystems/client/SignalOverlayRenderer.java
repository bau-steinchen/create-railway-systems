package de.bausteinchen.railwaysystems.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.signal.LinkedSignal;
import de.bausteinchen.railwaysystems.signal.LinkedSignalBlockEntity;
import de.bausteinchen.railwaysystems.signal.MountedSignalBlock;
import de.bausteinchen.railwaysystems.signal.SignalFeature;
import de.bausteinchen.railwaysystems.signal.SignalOptions.Zusatz;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Zeichnet die Zusatzanzeigen eines Signals (Zs3, Zs3v, Zs1/Zs7, Zs6, Kennlicht) aus den Daten
 * des Block-Entity. Die Modelle liegen unter {@code block/overlay/<signal>/<teil>} und sind wie
 * die Signalmodelle mit dem Schirm nach Norden entworfen.
 */
public class SignalOverlayRenderer extends SafeBlockEntityRenderer<LinkedSignalBlockEntity> {

	public SignalOverlayRenderer(BlockEntityRendererProvider.Context context) {}

	/**
	 * Alle Overlay-Modelle eines Signals, zum Registrieren beim Laden der Modelle.
	 */
	public static List<ResourceLocation> getModels(Block block, Set<SignalFeature> features) {
		List<String> parts = new ArrayList<>();
		for (SignalFeature feature : features) {
			switch (feature) {
				case ZS3, ZS3V -> {
					String name = feature == SignalFeature.ZS3 ? "zs3" : "zs3v";
					parts.add(name + "_box");
					for (int n = 1; n <= 9; n++)
						parts.add(name + "_" + n);
				}
				case ZUSATZ -> parts.addAll(List.of("zs1_off", "zs1_on", "zs7_off", "zs7_on"));
				case ZS6 -> parts.addAll(List.of("zs6_off", "zs6_on"));
				case KENNLICHT -> parts.addAll(List.of("kennlicht_off", "kennlicht_on"));
			}
		}
		return parts.stream()
			.map(part -> model(block, part))
			.toList();
	}

	private static ResourceLocation model(Block block, String part) {
		return RailwaySystems.asResource("block/overlay/" + BuiltInRegistries.BLOCK.getKey(block)
			.getPath() + "/" + part);
	}

	@Override
	protected void renderSafe(LinkedSignalBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		BlockState state = be.getBlockState();
		if (!(state.getBlock() instanceof LinkedSignal signal) || !state.hasProperty(MountedSignalBlock.FACING))
			return;
		Set<SignalFeature> features = signal.getFeatures();
		if (features.isEmpty())
			return;

		Block block = state.getBlock();
		boolean proceed = signal.showsProceed(state);
		VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());

		ms.pushPose();
		ms.translate(0.5, 0, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-yRotation(state.getValue(MountedSignalBlock.FACING))));
		ms.translate(-0.5, 0, -0.5);

		if (features.contains(SignalFeature.ZS3) && be.getZs3() > 0) {
			draw(ms, consumer, state, model(block, "zs3_box"), light, overlay);
			if (proceed)
				draw(ms, consumer, state, model(block, "zs3_" + be.getZs3()), LightTexture.FULL_BRIGHT, overlay);
		}
		if (features.contains(SignalFeature.ZS3V)) {
			draw(ms, consumer, state, model(block, "zs3v_box"), light, overlay);
			if (be.getZs3v() > 0)
				draw(ms, consumer, state, model(block, "zs3v_" + be.getZs3v()), LightTexture.FULL_BRIGHT, overlay);
		}
		if (features.contains(SignalFeature.ZUSATZ) && be.getZusatz() != Zusatz.NONE) {
			String name = be.getZusatz() == Zusatz.ZS1 ? "zs1" : "zs7";
			boolean lit = be.isZusatzLit();
			draw(ms, consumer, state, model(block, name + (lit ? "_on" : "_off")), lit ? LightTexture.FULL_BRIGHT : light,
				overlay);
		}
		if (features.contains(SignalFeature.ZS6) && be.hasZs6())
			draw(ms, consumer, state, model(block, proceed ? "zs6_on" : "zs6_off"),
				proceed ? LightTexture.FULL_BRIGHT : light, overlay);
		if (features.contains(SignalFeature.KENNLICHT)) {
			boolean lit = be.isSwitchedOff();
			draw(ms, consumer, state, model(block, lit ? "kennlicht_on" : "kennlicht_off"),
				lit ? LightTexture.FULL_BRIGHT : light, overlay);
		}

		ms.popPose();
	}

	/**
	 * Drehung wie im Blockstate ("y"): Nord 0°, Ost 90°, Süd 180°, West 270°, im Uhrzeigersinn.
	 */
	private static float yRotation(Direction facing) {
		return switch (facing) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
	}

	private static void draw(PoseStack ms, VertexConsumer consumer, BlockState state, ResourceLocation location,
		int light, int overlay) {
		BakedModel model = Minecraft.getInstance()
			.getModelManager()
			.getModel(ModelResourceLocation.standalone(location));
		Minecraft.getInstance()
			.getBlockRenderer()
			.getModelRenderer()
			.renderModel(ms.last(), consumer, state, model, 1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.cutout());
	}

}
