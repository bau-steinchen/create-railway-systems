package de.bausteinchen.railwaysystems.traffic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block einer Straßenampel, die ein Steuergerät schalten kann.
 */
public interface TrafficLight {

	boolean isPedestrian();

	/**
	 * Position des Blocks mit Block-Entity (bei Masten der Fußblock).
	 */
	default BlockPos getMainPos(Level level, BlockPos pos, BlockState state) {
		return pos;
	}

	TrafficAspect getAspect(BlockState state);

	/**
	 * Begriff zeigen; Fußgängerampeln bilden alles außer Grün und Aus auf Rot ab.
	 */
	BlockState withAspect(BlockState state, TrafficAspect aspect);

	static void setAspect(Level level, BlockPos mainPos, TrafficAspect aspect) {
		BlockState state = level.getBlockState(mainPos);
		if (!(state.getBlock() instanceof TrafficLight light))
			return;
		BlockState newState = light.withAspect(state, aspect);
		if (newState != state)
			level.setBlock(mainPos, newState, 3);
	}

}
