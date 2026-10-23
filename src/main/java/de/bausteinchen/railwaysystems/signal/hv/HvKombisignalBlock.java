package de.bausteinchen.railwaysystems.signal.hv;

import de.bausteinchen.railwaysystems.signal.SignalInput;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * H/V-Hauptsignal mit Vorsignal am Mast. Der Vorsignalteil kündigt das nächste Signal
 * entlang der Strecke an und ist dunkel, solange das Hauptsignal Hp0 zeigt.
 */
public class HvKombisignalBlock extends HvSignalBlock {

	public HvKombisignalBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(MAIN_ASPECT, HpAspect.HP0)
			.setValue(DISTANT_ASPECT, VrAspect.DARK));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(MAIN_ASPECT, DISTANT_ASPECT));
	}

	@Override
	public BlockState updateAspect(BlockState state, SignalInput input) {
		HpAspect main = mainAspect(input);
		VrAspect distant = main == HpAspect.HP0 ? VrAspect.DARK : distantAspect(input.getNextSignal());
		return state.setValue(MAIN_ASPECT, main)
			.setValue(DISTANT_ASPECT, distant);
	}

}
