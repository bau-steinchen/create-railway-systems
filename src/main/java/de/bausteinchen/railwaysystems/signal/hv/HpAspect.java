package de.bausteinchen.railwaysystems.signal.hv;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

/**
 * Signalbegriffe eines H/V-Hauptsignals.
 */
public enum HpAspect implements StringRepresentable {
	/** Halt */
	HP0,
	/** Fahrt */
	HP1,
	/** Langsamfahrt */
	HP2;

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
