package de.bausteinchen.railwaysystems.signal.hv;

import de.bausteinchen.railwaysystems.signal.SignalInput;
import de.bausteinchen.railwaysystems.signal.SignalInput.NextSignal;

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
	protected boolean isTall() {
		return true;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(MAIN_ASPECT, DISTANT_ASPECT));
	}

	@Override
	public BlockState updateAspect(BlockState state, SignalInput input) {
		HpAspect main = mainAspect(input);
		boolean dark = main == HpAspect.HP0 || main == HpAspect.OFF;
		NextSignal next = dark ? NextSignal.STOP : input.getNextSignal();
		input.getBlockEntity()
			.announce(next);
		VrAspect distant = dark ? VrAspect.DARK : distantAspect(next);
		return state.setValue(MAIN_ASPECT, main)
			.setValue(DISTANT_ASPECT, distant);
	}

}
