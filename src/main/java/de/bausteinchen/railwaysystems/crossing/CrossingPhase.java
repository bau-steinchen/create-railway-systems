package de.bausteinchen.railwaysystems.crossing;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

/**
 * Anzeige eines Bahnübergangs-Lichtzeichens: dunkel, Gelb (Vorwarnung) oder Rot. Das Blinklicht kennt
 * nur {@link #OFF} und {@link #RED} (rot blinkend).
 */
public enum CrossingPhase implements StringRepresentable {

	OFF, YELLOW, RED;

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

}
