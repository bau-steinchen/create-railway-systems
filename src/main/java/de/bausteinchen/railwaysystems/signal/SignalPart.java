package de.bausteinchen.railwaysystems.signal;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

/**
 * Teil eines drei Blöcke hohen Signals, von unten nach oben.
 */
public enum SignalPart implements StringRepresentable {
	LOWER,
	MIDDLE,
	UPPER;

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
