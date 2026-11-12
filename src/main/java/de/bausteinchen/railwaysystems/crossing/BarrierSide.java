package de.bausteinchen.railwaysystems.crossing;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

/**
 * Seite, zu der der Schrankenbaum vom Antrieb aus zeigt, gesehen von vorn (vom Straßenverkehr, der auf
 * die Schranke zufährt).
 */
public enum BarrierSide implements StringRepresentable {

	LEFT, RIGHT;

	public BarrierSide opposite() {
		return this == LEFT ? RIGHT : LEFT;
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

}
