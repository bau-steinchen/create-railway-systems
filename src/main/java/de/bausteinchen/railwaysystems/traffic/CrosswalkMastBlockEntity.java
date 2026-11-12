package de.bausteinchen.railwaysystems.traffic;

import java.util.List;

import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import de.bausteinchen.railwaysystems.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Zebrastreifen-Mast mit eingebauter Funkverbindung (Empfänger). Die beiden Frequenzfelder sitzen auf der
 * Rückseite des Fußblocks; solange auf der Frequenz ein Redstone-Signal ankommt, leuchtet die Leuchte.
 */
public class CrosswalkMastBlockEntity extends TrafficLightBlockEntity {

	public CrosswalkMastBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CROSSWALK_MAST.get(), pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(LinkBehaviour.receiver(this, ValueBoxTransform.Dual.makeSlots(CrosswalkLinkSlot::new),
			this::setReceivedStrength));
	}

	private void setReceivedStrength(int strength) {
		if (level == null || level.isClientSide)
			return;
		BlockState state = getBlockState();
		boolean lit = strength > 0;
		if (state.hasProperty(CrosswalkMastBlock.LIT) && state.getValue(CrosswalkMastBlock.LIT) != lit)
			level.setBlock(worldPosition, state.setValue(CrosswalkMastBlock.LIT, lit), 3);
	}

}
