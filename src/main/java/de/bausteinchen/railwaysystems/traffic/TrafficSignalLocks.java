package de.bausteinchen.railwaysystems.traffic;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;

/**
 * Welche Ampeln ein Create-Signal gerade auf Halt halten. Mehrere Ampeln können an dasselbe Signal
 * gebunden sein; frei wird es erst, wenn keine mehr hält. Nur auf dem Server, nicht gespeichert:
 * geladene Ampeln tragen sich jeden Tick neu ein.
 */
public final class TrafficSignalLocks {

	private static final Map<GlobalPos, Set<BlockPos>> HOLDS = new ConcurrentHashMap<>();

	private TrafficSignalLocks() {}

	static void set(GlobalPos signal, BlockPos light, boolean hold) {
		if (hold)
			HOLDS.computeIfAbsent(signal, k -> ConcurrentHashMap.newKeySet())
				.add(light);
		else
			HOLDS.computeIfPresent(signal, (k, lights) -> {
				lights.remove(light);
				return lights.isEmpty() ? null : lights;
			});
	}

	static boolean isHeld(GlobalPos signal) {
		return HOLDS.containsKey(signal);
	}

	public static void clear() {
		HOLDS.clear();
	}

}
