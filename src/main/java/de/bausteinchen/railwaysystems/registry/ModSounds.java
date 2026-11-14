package de.bausteinchen.railwaysystems.registry;

import java.util.function.Supplier;

import de.bausteinchen.railwaysystems.RailwaySystems;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {

	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT,
		RailwaySystems.MOD_ID);

	/** ein Schlag des Läutewerks am Bahnübergang (Klang in sounds.json) */
	public static final Supplier<SoundEvent> CROSSING_BELL = SOUNDS.register("crossing_bell",
		() -> SoundEvent.createVariableRangeEvent(RailwaySystems.asResource("crossing_bell")));

}
