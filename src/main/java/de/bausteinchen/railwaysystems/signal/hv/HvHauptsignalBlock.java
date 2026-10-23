package de.bausteinchen.railwaysystems.signal.hv;

import de.bausteinchen.railwaysystems.signal.SignalInput;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * H/V-Lichthauptsignal: Hp0 / Hp1 / Hp2.
 */
public class HvHauptsignalBlock extends HvSignalBlock {

	public HvHauptsignalBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(MAIN_ASPECT, HpAspect.HP0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(MAIN_ASPECT));
	}

	@Override
	public BlockState updateAspect(BlockState state, SignalInput input) {
		return state.setValue(MAIN_ASPECT, mainAspect(input));
	}

}
