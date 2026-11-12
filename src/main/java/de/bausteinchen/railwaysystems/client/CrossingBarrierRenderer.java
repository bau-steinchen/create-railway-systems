package de.bausteinchen.railwaysystems.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.crossing.BarrierSide;
import de.bausteinchen.railwaysystems.crossing.CrossingBarrierBlock;
import de.bausteinchen.railwaysystems.crossing.CrossingBarrierBlockEntity;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Zeichnet den Schrankenbaum aus Segmenten von je einem Block Länge ({@link #BOOM}), um den Drehpunkt
 * vorn am Antrieb geschwenkt. Wertefeld und Funkfrequenzen zeichnet der {@link SmartBlockEntityRenderer}.
 */
public class CrossingBarrierRenderer extends SmartBlockEntityRenderer<CrossingBarrierBlockEntity> {

	/** Baumsegment: von x = 0 bis 16 px, mittig um y = 0 und z = 0 (Drehpunkt im Ursprung) */
	public static final ResourceLocation BOOM = RailwaySystems.asResource("block/bue_schrankenbaum");
	/** Endstück mit rotem Licht, am Ende des letzten Segments */
	public static final ResourceLocation BOOM_TIP = RailwaySystems.asResource("block/bue_schrankenbaum_ende");

	public CrossingBarrierRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(CrossingBarrierBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
		BlockState state = be.getBlockState();
		if (!(state.getBlock() instanceof CrossingBarrierBlock))
			return;

		// Modelle sind mit der Front nach Norden entworfen; der Betrachter vorn sieht +x als "links"
		boolean left = state.getValue(CrossingBarrierBlock.SIDE) == BarrierSide.LEFT;
		float sign = left ? 1 : -1;
		VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());

		ms.pushPose();
		ms.translate(0.5, 0, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(state.getValue(CrossingBarrierBlock.FACING)
			.getOpposite())));
		ms.translate(-0.5, 0, -0.5);
		ms.translate(0.5, CrossingBarrierBlock.PIVOT_Y / 16, CrossingBarrierBlock.PIVOT_Z / 16);
		ms.mulPose(Axis.ZP.rotationDegrees(sign * be.getAngle(partialTicks)));
		if (!left)
			// Segmente zeigen nach +x; zur rechten Seite um die senkrechte Achse durch den Drehpunkt wenden
			ms.mulPose(Axis.YP.rotationDegrees(180));

		int length = be.getLength();
		for (int i = 0; i < length; i++) {
			SignalOverlayRenderer.draw(ms, consumer, state, BOOM, light, overlay);
			ms.translate(1, 0, 0);
		}
		SignalOverlayRenderer.draw(ms, consumer, state, BOOM_TIP, light, overlay);
		ms.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(CrossingBarrierBlockEntity be) {
		return true;
	}

}
