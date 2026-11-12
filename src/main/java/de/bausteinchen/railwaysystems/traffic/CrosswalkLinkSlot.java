package de.bausteinchen.railwaysystems.traffic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Frequenzfeld der Funkverbindung im Zebrastreifen-Mast: auf der Rückseite des Mastfußes, das erste über
 * dem zweiten wie beim Redstone Link.
 */
public class CrosswalkLinkSlot extends ValueBoxTransform.Dual {

	public CrosswalkLinkSlot(boolean first) {
		super(first);
	}

	@Override
	public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
		if (!state.hasProperty(AbstractMastBlock.FACING))
			return null;
		// knapp hinter der Mastrückseite (Schaft 6..10 px)
		Vec3 location = VecHelper.voxelSpace(8, isFirst() ? 11.5 : 6, 10.25);
		return VecHelper.rotateCentered(location, AngleHelper.horizontalAngle(back(state)), Axis.Y);
	}

	@Override
	public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
		TransformStack.of(ms)
			.rotateYDegrees(AngleHelper.horizontalAngle(back(state)) + 180);
	}

	private static Direction back(BlockState state) {
		return state.getValue(AbstractMastBlock.FACING)
			.getOpposite();
	}

	@Override
	public float getScale() {
		return 0.4f;
	}

}
