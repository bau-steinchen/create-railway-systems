package de.bausteinchen.railwaysystems.signal;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Frequenzfeld der eingebauten Funkverbindung: an der Seite der oberen Signalhälfte (auch bei
 * Wandmontage erreichbar), das erste über dem zweiten wie beim Redstone Link.
 */
public class SignalLinkSlot extends ValueBoxTransform.Dual {

	public SignalLinkSlot(boolean first) {
		super(first);
	}

	@Override
	public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
		if (!state.hasProperty(MountedSignalBlock.FACING))
			return null;
		return MountedSignalBlock.sideSlot(state, true, isFirst() ? 11 : 5.5);
	}

	@Override
	public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
		TransformStack.of(ms)
			.rotateYDegrees(AngleHelper.horizontalAngle(MountedSignalBlock.slotSide(state, true)) + 180);
	}

	@Override
	public float getScale() {
		return 0.35f;
	}

}
