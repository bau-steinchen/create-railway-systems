package de.bausteinchen.railwaysystems.signal;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Wertefeld an der Seite des Schirms (untere Signalhälfte), damit es auch bei Wandmontage
 * erreichbar bleibt. {@code x} wählt die Seite (unter 8 die eine, sonst die andere), {@code y} die
 * Höhe in Pixeln.
 */
public class SignalValueBox extends ValueBoxTransform.Sided {

	private final boolean plusX;
	private final float y;

	public SignalValueBox(float x, float y) {
		this.plusX = x > 8;
		this.y = y;
	}

	@Override
	public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
		if (!state.hasProperty(MountedSignalBlock.MOUNT))
			return super.getLocalOffset(level, pos, state);
		return MountedSignalBlock.sideSlot(state, plusX, y);
	}

	@Override
	protected Vec3 getSouthLocation() {
		// nur für Blöcke ohne Montage; Signale nutzen getLocalOffset
		return VecHelper.voxelSpace(8, y, 16.25);
	}

	@Override
	protected boolean isSideActive(BlockState state, Direction direction) {
		return state.hasProperty(MountedSignalBlock.MOUNT) && direction == MountedSignalBlock.slotSide(state, plusX);
	}

	@Override
	public float getScale() {
		return 0.3f;
	}

}
