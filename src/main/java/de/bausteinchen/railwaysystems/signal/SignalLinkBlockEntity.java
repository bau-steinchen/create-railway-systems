package de.bausteinchen.railwaysystems.signal;

import java.util.List;

import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import de.bausteinchen.railwaysystems.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Eingebaute Funkverbindung (Create Redstone Link, Empfänger) in der oberen Hälfte hoher Signale.
 * Die beiden Frequenzfelder liegen auf der Rückseite; ein empfangenes Signal wirkt wie Redstone
 * am Signal und löst Zs1/Zs7/Zs8 bzw. Zs2 aus.
 */
public class SignalLinkBlockEntity extends SmartBlockEntity {

	private int receivedStrength;

	public SignalLinkBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SIGNAL_LINK.get(), pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(LinkBehaviour.receiver(this, ValueBoxTransform.Dual.makeSlots(SignalLinkSlot::new),
			this::setReceivedStrength));
	}

	public boolean isPowered() {
		return receivedStrength > 0;
	}

	private void setReceivedStrength(int strength) {
		if (receivedStrength == strength)
			return;
		receivedStrength = strength;
		// das Signal in der unteren Hälfte sofort neu bewerten statt auf den nächsten Takt zu warten
		if (level != null && level.getBlockEntity(worldPosition.below()) instanceof LinkedSignalBlockEntity signal)
			signal.updateAspect();
	}

}
