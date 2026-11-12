package de.bausteinchen.railwaysystems.traffic;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

/**
 * Begriff einer Straßenampel. Fußgängerampeln kennen nur {@link #OFF}, {@link #RED} und {@link #GREEN}.
 */
public enum TrafficAspect implements StringRepresentable {
	OFF,
	RED,
	RED_YELLOW,
	GREEN,
	YELLOW,
	/** Nachtbetrieb: Gelb blinkt, Verkehr fährt nach Vorfahrtsregeln */
	YELLOW_BLINK;

	/**
	 * Ob Fahrzeuge (Create-Züge) bei diesem Begriff halten müssen.
	 */
	public boolean holdsTraffic() {
		return this == RED || this == RED_YELLOW || this == YELLOW;
	}

	public boolean isRedLit() {
		return this == RED || this == RED_YELLOW;
	}

	public boolean isYellowLit() {
		return this == RED_YELLOW || this == YELLOW || this == YELLOW_BLINK;
	}

	public boolean isGreenLit() {
		return this == GREEN;
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
