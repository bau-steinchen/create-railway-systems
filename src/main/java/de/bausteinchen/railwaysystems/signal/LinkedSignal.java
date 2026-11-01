package de.bausteinchen.railwaysystems.signal;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Ein Signalblock, der an ein Create-Signal gebunden wird und dessen Zustand als
 * vorbildgerechten Signalbegriff anzeigt.
 */
public interface LinkedSignal {

	/**
	 * Leitet den anzuzeigenden Blockzustand aus dem gebundenen Create-Signal ab.
	 */
	BlockState updateAspect(BlockState state, SignalInput input);

	/**
	 * Geschwindigkeit (in 10 km/h), die Vorsignale für dieses Signal ankündigen; 0 = keine
	 * Beschränkung, -1 wenn das Signal kein Hauptsignal ist und nichts ankündigen lässt.
	 */
	default int getSpeedLimit(BlockState state, LinkedSignalBlockEntity blockEntity) {
		return -1;
	}

	/**
	 * Zusatzanzeigen dieses Signals, dargestellt vom Overlay-Renderer.
	 */
	default Set<SignalFeature> getFeatures() {
		return Set.of();
	}

	/**
	 * Ob der Begriff Fahrt erlaubt; Zs3 und Zs6 leuchten nur dann.
	 */
	default boolean showsProceed(BlockState state) {
		return false;
	}

	/**
	 * Ob das Signal Halt zeigt; nur dann können Zs1/Zs7 leuchten.
	 */
	default boolean showsStop(BlockState state) {
		return false;
	}

	/**
	 * Tooltip-Schlüssel für die Schraubenschlüssel-Funktion, oder null.
	 */
	@Nullable
	default String getWrenchTooltipKey() {
		return null;
	}

}
