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
 * Ks-Vorsignal: Ks1 / Ks1 blinkend mit Zs3v / Ks2, dazu Zs2v. Wird an das Create-Signal des angekündigten
 * Hauptsignals gebunden.
 */
public class KsVorsignalBlock extends KsSignalBlock {

	public static final EnumProperty<KsAspect> ASPECT =
		EnumProperty.create("aspect", KsAspect.class, KsAspect.KS1, KsAspect.KS1_BLINK, KsAspect.KS2);

	public KsVorsignalBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(ASPECT, KsAspect.KS2));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(ASPECT));
	}

	@Override
	public Set<SignalFeature> getFeatures() {
		return EnumSet.of(SignalFeature.ZS3V, SignalFeature.ZS2V);
	}

	@Override
	public BlockState updateAspect(BlockState state, SignalInput input) {
		return withDistantAspect(state, input.getLinkedSignal(), ASPECT, input);
	}

}
