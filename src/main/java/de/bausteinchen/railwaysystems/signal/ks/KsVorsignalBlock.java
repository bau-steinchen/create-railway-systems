package de.bausteinchen.railwaysystems.signal.ks;

import de.bausteinchen.railwaysystems.signal.SignalInput;
import de.bausteinchen.railwaysystems.signal.SignalInput.NextSignal;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Ks-Vorsignal: Ks1 / Ks1 blinkend mit Zs3v / Ks2. Wird an das Create-Signal des angekündigten
 * Hauptsignals gebunden.
 */
public class KsVorsignalBlock extends KsSignalBlock {

	public static final EnumProperty<KsAspect> ASPECT =
		EnumProperty.create("aspect", KsAspect.class, KsAspect.KS1, KsAspect.KS1_BLINK, KsAspect.KS2);

	public KsVorsignalBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(ASPECT, KsAspect.KS2)
			.setValue(ZS3V, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(ASPECT, ZS3V));
	}

	@Override
	public BlockState updateAspect(BlockState state, SignalInput input) {
		return withDistantAspect(state, new NextSignal(input.isClear(), input.getLinkedSpeedLimit()), ASPECT);
	}

}
