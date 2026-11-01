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
 * Ks-Hauptsignal: Hp0 / Ks1, optional mit Zs3, Zs1/Zs7, Zs6 und Kennlicht.
 */
public class KsHauptsignalBlock extends KsSignalBlock {

	public static final EnumProperty<KsAspect> ASPECT =
		EnumProperty.create("aspect", KsAspect.class, KsAspect.HP0, KsAspect.KS1, KsAspect.OFF);

	public KsHauptsignalBlock(Properties properties) {
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
		return EnumSet.of(SignalFeature.ZS3, SignalFeature.ZUSATZ, SignalFeature.ZS6, SignalFeature.KENNLICHT);
	}

	@Override
	public BlockState updateAspect(BlockState state, SignalInput input) {
		if (input.getBlockEntity()
			.isSwitchedOff())
			return state.setValue(ASPECT, KsAspect.OFF);
		return state.setValue(ASPECT, input.isClear() ? KsAspect.KS1 : KsAspect.HP0);
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
