package de.bausteinchen.railwaysystems.signal.ks;

import java.util.EnumSet;
import java.util.Set;

import de.bausteinchen.railwaysystems.signal.SignalFeature;
import de.bausteinchen.railwaysystems.signal.SignalInput;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Ks-Mehrabschnittssignal: Haupt- und Vorsignal in einem Schirm (Hp0 / Ks1 / Ks1 blinkend /
 * Ks2). Der Vorsignalbegriff kündigt die Signale am Ende des Blockabschnitts an.
 */
public class KsMehrabschnittssignalBlock extends KsSignalBlock {

	public static final EnumProperty<KsAspect> ASPECT = EnumProperty.create("aspect", KsAspect.class);

	public KsMehrabschnittssignalBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(ASPECT, KsAspect.HP0));
	}

	@Override
	protected boolean isTall() {
		return true;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(ASPECT));
	}

	@Override
	public Set<SignalFeature> getFeatures() {
		return EnumSet.of(SignalFeature.ZS3, SignalFeature.ZS3V, SignalFeature.ZUSATZ, SignalFeature.ZS6,
			SignalFeature.KENNLICHT);
	}

	@Override
	public BlockState updateAspect(BlockState state, SignalInput input) {
		if (input.getBlockEntity()
			.isSwitchedOff() || !input.isClear()) {
			input.getBlockEntity()
				.setZs3v(0);
			return state.setValue(ASPECT, input.getBlockEntity()
				.isSwitchedOff() ? KsAspect.OFF : KsAspect.HP0);
		}
		return withDistantAspect(state, input.getNextSignal(), ASPECT, input);
	}

	@Override
	public boolean showsProceed(BlockState state) {
		return isProceed(state.getValue(ASPECT));
	}

	@Override
	public boolean showsStop(BlockState state) {
		return state.getValue(ASPECT) == KsAspect.HP0;
	}

}
