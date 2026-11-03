package de.bausteinchen.railwaysystems.signal;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

/**
 * Befestigung eines Signalschirms: frei auf dem eigenen Mast, bündig an einer Wand ({@link #BACK}),
 * bündig vor einem {@link SignalMastBlock} mit zwei kurzen Halterstangen bis an dessen Oberfläche
 * ({@link #MAST}, {@link #MAST_PLATE}, {@link #MAST_LATTICE}; sie unterscheiden sich nur in der
 * Stangenlänge) oder mit waagerechtem Ausleger an einem seitlichen Nachbarblock. Links/rechts gelten
 * aus Sicht des Betrachters vor dem Schirm.
 */
public enum SignalMount implements StringRepresentable {
	NONE(0),
	BACK(9.5),
	LEFT(0),
	RIGHT(0),
	/** Leichter Mast oder Kante des Standardmasts: Oberfläche 6,5 px hinter der Blockkante. */
	MAST(9.5),
	/** Platte des Standardmasts: 5,5 px hinter der Blockkante. */
	MAST_PLATE(9.5),
	/** Streben des Gittermasts: 3,25 px hinter der Blockkante. */
	MAST_LATTICE(9.5);

	private final double flushShift;

	SignalMount(double flushShift) {
		this.flushShift = flushShift;
	}

	/**
	 * Richtung zum tragenden Block, oder null bei freier Aufstellung.
	 */
	@Nullable
	public Direction getSupportDirection(Direction facing) {
		return switch (this) {
			case NONE -> null;
			case BACK, MAST, MAST_PLATE, MAST_LATTICE -> facing.getOpposite();
			case LEFT -> facing.getClockWise();
			case RIGHT -> facing.getCounterClockWise();
		};
	}

	/**
	 * Ob der Schirm bündig an der hinteren Blockkante sitzt statt mittig im Block.
	 */
	public boolean isFlush() {
		return flushShift > 0;
	}

	/**
	 * Versatz des Signalkopfs nach hinten in Pixeln. Mittig montiert endet der Kopf bei z = 6,5;
	 * hinten montiert an der Blockkante. tools/generate_assets.py nutzt denselben Wert.
	 */
	public double getFlushShift() {
		return flushShift;
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
