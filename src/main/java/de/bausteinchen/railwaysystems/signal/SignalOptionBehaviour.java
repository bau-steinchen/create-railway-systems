package de.bausteinchen.railwaysystems.signal;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * Create-Wertefeld für eine Signaloption. Create speichert Behaviours je Typ und schickt
 * Einstellungen über eine Netz-ID; damit mehrere Wertefelder an einem Block-Entity funktionieren,
 * bekommt jedes Feld einen eigenen Typ, eine eigene Netz-ID und einen eigenen NBT-Schlüssel.
 */
public class SignalOptionBehaviour<E extends Enum<E> & INamedIconOptions> extends ScrollOptionBehaviour<E> {

	private final BehaviourType<SignalOptionBehaviour<?>> type;
	private final int netId;
	private final String nbtKey;

	public SignalOptionBehaviour(Class<E> options, Component label, SmartBlockEntity be, ValueBoxTransform slot,
		BehaviourType<SignalOptionBehaviour<?>> type, int netId, String nbtKey) {
		super(options, label, be, slot);
		this.type = type;
		this.netId = netId;
		this.nbtKey = nbtKey;
		requiresWrench();
	}

	@Override
	public BehaviourType<?> getType() {
		return type;
	}

	@Override
	public int netId() {
		return netId;
	}

	@Override
	public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
		nbt.putInt(nbtKey, value);
	}

	@Override
	public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
		value = nbt.getInt(nbtKey);
	}

}
