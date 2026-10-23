package de.bausteinchen.railwaysystems.signal.ks;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

/**
 * Signalbegriffe der Ks-Signale.
 */
public enum KsAspect implements StringRepresentable {
	/** Halt */
	HP0,
	/** Fahrt */
	KS1,
	/** Fahrt, angezeigte Geschwindigkeit (Zs3v) erwarten */
	KS1_BLINK,
	/** Halt erwarten */
	KS2;

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
