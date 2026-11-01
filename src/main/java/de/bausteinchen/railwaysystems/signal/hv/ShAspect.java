package de.bausteinchen.railwaysystems.signal.hv;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

/**
 * Signalbegriffe eines Lichtsperrsignals.
 */
public enum ShAspect implements StringRepresentable {
	/** Halt! Fahrverbot */
	SH0,
	/** Fahrverbot aufgehoben */
	SH1;

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
