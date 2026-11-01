package de.bausteinchen.railwaysystems.signal.hv;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

/**
 * Signalbegriffe eines H/V-Vorsignals.
 */
public enum VrAspect implements StringRepresentable {
	/** Halt erwarten */
	VR0,
	/** Fahrt erwarten */
	VR1,
	/** Langsamfahrt erwarten */
	VR2,
	/** Dunkelgeschaltet (Vorsignal am Mast eines Halt zeigenden Hauptsignals) */
	DARK;

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
