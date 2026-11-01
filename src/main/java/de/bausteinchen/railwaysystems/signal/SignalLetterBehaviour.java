package de.bausteinchen.railwaysystems.signal;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Create-Wertefeld für den Buchstaben des Richtungsanzeigers Zs2: 0 = keiner, 1–26 = A–Z.
 * Eigener Typ, eigene Netz-ID und eigener NBT-Schlüssel wie bei {@link SignalOptionBehaviour}.
 */
public class SignalLetterBehaviour extends ScrollValueBehaviour {

	public static final int LETTERS = 26;

	private final BehaviourType<SignalLetterBehaviour> type;
	private final int netId;
	private final String nbtKey;

	public SignalLetterBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot,
		BehaviourType<SignalLetterBehaviour> type, int netId, String nbtKey) {
		super(label, be, slot);
		this.type = type;
		this.netId = netId;
		this.nbtKey = nbtKey;
		between(0, LETTERS);
		withFormatter(SignalLetterBehaviour::format);
		requiresWrench();
	}

	public static String format(int letter) {
		return letter <= 0 ? "-" : String.valueOf((char) ('A' + letter - 1));
	}

	@Override
	public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
		return new ValueSettingsBoard(label, max, 5, ImmutableList.of(Component.literal("Zs2")),
			new ValueSettingsFormatter(settings -> Component.literal(format(settings.value()))));
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
