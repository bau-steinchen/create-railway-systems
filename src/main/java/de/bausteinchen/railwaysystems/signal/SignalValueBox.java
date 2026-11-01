package de.bausteinchen.railwaysystems.signal;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Wertefeld auf der Rückseite des Masts (untere Signalhälfte) an der Stelle {@code x}, {@code y}
 * (von hinten gesehen, in Pixeln).
 */
public class SignalValueBox extends ValueBoxTransform.Sided {

	private final float x;
	private final float y;

	public SignalValueBox(float x, float y) {
		this.x = x;
		this.y = y;
	}

	@Override
	protected Vec3 getSouthLocation() {
		// knapp hinter der Mastrückseite (Mast endet bei 9,5 px)
		return VecHelper.voxelSpace(x, y, 9.75);
	}

	@Override
	protected boolean isSideActive(BlockState state, Direction direction) {
		return state.hasProperty(MountedSignalBlock.FACING) && direction == state.getValue(MountedSignalBlock.FACING)
			.getOpposite();
	}

	@Override
	public float getScale() {
		return 0.3f;
	}

}
