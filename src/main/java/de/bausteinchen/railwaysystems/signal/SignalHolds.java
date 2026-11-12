package de.bausteinchen.railwaysystems.signal;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.content.trains.signal.SignalBoundary;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;

/**
 * Hält Create-Signale auf Halt, solange mindestens ein Block (Straßenampel, Bahnübergangs-Steuerung)
 * das verlangt. Das Signal wird dazu bei seiner {@link SignalBoundary} als redstone-gespeist gemeldet,
 * genauso wie Create es bei einem Redstone-Signal am Signalblock selbst tut; Züge halten dann davor.
 * <p>
 * Nur auf dem Server und nicht gespeichert: Haltende Blöcke tragen sich jeden Tick neu ein, denn Create
 * setzt den Eintrag zurück, sobald sich echter Redstone am Signal ändert. Beim Abbauen und Entladen
 * müssen sie {@link #release} aufrufen.
 */
public final class SignalHolds {

	private static final Map<GlobalPos, Set<BlockPos>> HOLDS = new ConcurrentHashMap<>();

	private SignalHolds() {}

	/**
	 * Wunsch eines haltenden Blocks eintragen und das Signal entsprechend setzen.
	 */
	public static void set(Level level, BlockPos signal, BlockPos holder, boolean hold) {
		GlobalPos key = GlobalPos.of(level.dimension(), signal);
		if (hold)
			HOLDS.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
				.add(holder);
		else
			HOLDS.computeIfPresent(key, (k, holders) -> {
				holders.remove(holder);
				return holders.isEmpty() ? null : holders;
			});
		apply(level, signal, HOLDS.containsKey(key));
	}

	public static void release(Level level, BlockPos signal, BlockPos holder) {
		set(level, signal, holder, false);
	}

	public static void clear() {
		HOLDS.clear();
	}

	/**
	 * Eintrag des Create-Signals in seiner {@link SignalBoundary} setzen: rot erzwungen, solange ein Block
	 * hält, sonst so, wie Create ihn selbst aus dem Redstone am Signal setzen würde.
	 */
	private static void apply(Level level, BlockPos target, boolean held) {
		if (!level.isLoaded(target) || !(level.getBlockEntity(target) instanceof SignalBlockEntity sbe))
			return;
		SignalBoundary boundary = sbe.getSignal();
		if (boundary == null)
			return;
		boolean forced = held || sbe.getReportedPower();
		for (boolean side : Iterate.trueAndFalse) {
			Map<BlockPos, Boolean> signals = boundary.blockEntities.get(side);
			Boolean current = signals.get(target);
			if (current != null && current != forced)
				signals.put(target, forced);
		}
	}

}
