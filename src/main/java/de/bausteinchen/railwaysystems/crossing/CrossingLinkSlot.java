package de.bausteinchen.railwaysystems.crossing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Frequenzfeld der Funkverbindung an Bahnübergangs-Blöcken: auf der Rückseite, das erste über dem
 * zweiten wie beim Redstone Link.
 */
public class CrossingLinkSlot extends ValueBoxTransform.Dual {

	/** Lichtzeichen: knapp hinter dem Mast (6,5..9,5 px) */
	public static final float BEHIND_POST = 9.75f;

	private final float depth;

	public CrossingLinkSlot(boolean first) {
		this(first, BEHIND_POST);
	}

	/**
	 * @param depth Abstand der Felder von der Vorderseite in Pixeln (Modell mit Front nach Norden)
	 */
	public CrossingLinkSlot(boolean first, float depth) {
		super(first);
		this.depth = depth;
	}

	@Override
	public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
		if (!state.hasProperty(HorizontalDirectionalBlock.FACING))
			return null;
		Vec3 location = VecHelper.voxelSpace(8, isFirst() ? 11.5 : 5.5, depth);
		return VecHelper.rotateCentered(location, AngleHelper.horizontalAngle(back(state)), Axis.Y);
	}

	@Override
	public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
		TransformStack.of(ms)
			.rotateYDegrees(AngleHelper.horizontalAngle(back(state)) + 180);
	}

	private static Direction back(BlockState state) {
		return state.getValue(HorizontalDirectionalBlock.FACING)
			.getOpposite();
	}

	@Override
	public float getScale() {
		return 0.4f;
	}

}
