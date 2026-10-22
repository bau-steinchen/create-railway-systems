package de.bausteinchen.railwaysystems.signal.hv;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

/**
 * Befestigung eines Signalschirms: frei auf dem Mast oder mit waagerechtem Ausleger an einem
 * Nachbarblock. Links/rechts gelten aus Sicht des Betrachters vor dem Schirm.
 */
public enum SignalMount implements StringRepresentable {
	NONE,
	BACK,
	LEFT,
	RIGHT;

	/**
	 * Richtung zum tragenden Block, oder null bei freier Aufstellung.
	 */
	@Nullable
	public Direction getSupportDirection(Direction facing) {
		return switch (this) {
			case NONE -> null;
			case BACK -> facing.getOpposite();
			case LEFT -> facing.getClockWise();
			case RIGHT -> facing.getCounterClockWise();
		};
	}

	public static SignalMount fromSupportDirection(Direction facing, Direction support) {
		for (SignalMount mount : values())
			if (mount.getSupportDirection(facing) == support)
				return mount;
		return NONE;
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
