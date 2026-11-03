package de.bausteinchen.railwaysystems.signal;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

/**
 * Befestigung eines Signalschirms: frei auf dem eigenen Mast, bündig an einer Wand ({@link #BACK}),
 * bündig vor einem {@link SignalMastBlock} mit zwei Halterstangen ({@link #MAST}) oder mit
 * waagerechtem Ausleger an einem seitlichen Nachbarblock. Links/rechts gelten aus Sicht des
 * Betrachters vor dem Schirm.
 */
public enum SignalMount implements StringRepresentable {
	NONE,
	BACK,
	LEFT,
	RIGHT,
	MAST;

	/**
	 * Richtung zum tragenden Block, oder null bei freier Aufstellung.
	 */
	@Nullable
	public Direction getSupportDirection(Direction facing) {
		return switch (this) {
			case NONE -> null;
			case BACK, MAST -> facing.getOpposite();
			case LEFT -> facing.getClockWise();
			case RIGHT -> facing.getCounterClockWise();
		};
	}

	/**
	 * Ob der Schirm mit der Rückseite an der Blockkante sitzt statt mittig im Block.
	 */
	public boolean isFlush() {
		return this == BACK || this == MAST;
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
