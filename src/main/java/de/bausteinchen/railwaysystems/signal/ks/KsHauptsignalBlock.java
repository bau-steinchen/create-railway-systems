package de.bausteinchen.railwaysystems.signal.ks;

import de.bausteinchen.railwaysystems.signal.SignalInput;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Ks-Hauptsignal: Hp0 / Ks1, optional mit Zs3.
 */
public class KsHauptsignalBlock extends KsSignalBlock {

	public static final EnumProperty<KsAspect> ASPECT =
		EnumProperty.create("aspect", KsAspect.class, KsAspect.HP0, KsAspect.KS1);

	public KsHauptsignalBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(ASPECT, KsAspect.HP0)
			.setValue(ZS3, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(ASPECT, ZS3));
	}

	@Override
	public BlockState updateAspect(BlockState state, SignalInput input) {
		return state.setValue(ASPECT, input.isClear() ? KsAspect.KS1 : KsAspect.HP0);
	}

}
