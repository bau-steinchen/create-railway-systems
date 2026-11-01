package de.bausteinchen.railwaysystems.signal.hv;

import de.bausteinchen.railwaysystems.signal.SignalInput;
import de.bausteinchen.railwaysystems.signal.SignalInput.NextSignal;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Eigenständiges H/V-Lichtvorsignal: Vr0 / Vr1 / Vr2. Wird an das Create-Signal des
 * angekündigten Hauptsignals gebunden.
 */
public class HvVorsignalBlock extends HvSignalBlock {

	public static final EnumProperty<VrAspect> ASPECT =
		EnumProperty.create("aspect", VrAspect.class, VrAspect.VR0, VrAspect.VR1, VrAspect.VR2);

	public HvVorsignalBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(ASPECT, VrAspect.VR0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(ASPECT));
	}

	@Override
	public BlockState updateAspect(BlockState state, SignalInput input) {
		return state.setValue(ASPECT, distantAspect(new NextSignal(input.isClear(), input.getLinkedSpeedLimit())));
	}

}
