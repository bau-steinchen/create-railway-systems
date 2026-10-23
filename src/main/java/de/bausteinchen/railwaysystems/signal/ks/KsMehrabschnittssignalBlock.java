package de.bausteinchen.railwaysystems.signal.ks;

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
		registerDefaultState(defaultBlockState().setValue(ASPECT, KsAspect.HP0)
			.setValue(ZS3, 0)
			.setValue(ZS3V, 0));
	}

	@Override
	protected boolean isTall() {
		return true;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(ASPECT, ZS3, ZS3V));
	}

	@Override
	public BlockState updateAspect(BlockState state, SignalInput input) {
		if (!input.isClear())
			return state.setValue(ASPECT, KsAspect.HP0)
				.setValue(ZS3V, 0);
		return withDistantAspect(state, input.getNextSignal(), ASPECT);
	}

}
