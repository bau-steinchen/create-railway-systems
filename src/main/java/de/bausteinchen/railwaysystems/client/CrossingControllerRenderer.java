package de.bausteinchen.railwaysystems.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import de.bausteinchen.railwaysystems.crossing.CrossingControllerBlockEntity;
import de.bausteinchen.railwaysystems.crossing.LevelCrossingPoint;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Zeichnet die Frequenzfelder der Bahnübergangs-Steuerung und, wie beim Create-Gleisbeobachter, die
 * Markierung des gebundenen Punkts auf dem Gleis.
 */
public class CrossingControllerRenderer extends SmartBlockEntityRenderer<CrossingControllerBlockEntity> {

	public CrossingControllerRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(CrossingControllerBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
		Level level = be.getLevel();
		TrackTargetingBehaviour<LevelCrossingPoint> target = be.getEdgePoint();
		if (level == null || target == null)
			return;
		BlockPos targetPosition = target.getGlobalPosition();
		if (!(level.getBlockState(targetPosition)
			.getBlock() instanceof ITrackBlock))
			return;
		ms.pushPose();
		TransformStack.of(ms)
			.translate(targetPosition.subtract(be.getBlockPos()));
		TrackTargetingBehaviour.render(level, targetPosition, target.getTargetDirection(), target.getTargetBezier(), ms,
			buffer, light, overlay, RenderedTrackOverlayType.OBSERVER, 1);
		ms.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(CrossingControllerBlockEntity be) {
		return true;
	}

}
