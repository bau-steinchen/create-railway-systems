package de.bausteinchen.railwaysystems.signal;

/**
 * Zustand des gebundenen Create-Signals und der Strecke dahinter, aus dem ein
 * {@link LinkedSignal} seinen Begriff ableitet.
 */
public interface SignalInput {

	/**
	 * Ob das gebundene Create-Signal Fahrt zeigt (grün oder gelb).
	 */
	boolean isClear();

	/**
	 * Zustand des gebundenen Create-Signals samt Geschwindigkeit und Richtung, die ein H/V- oder
	 * Ks-Hauptsignal dort vorgibt. Für eigenständige Vorsignale.
	 */
	NextSignal getLinkedSignal();

	/**
	 * Zusammengefasster Zustand der Signale am Ende des Blockabschnitts hinter dem gebundenen
	 * Create-Signal. Für Vorsignale am Mast und Mehrabschnittssignale.
	 */
	NextSignal getNextSignal();

	LinkedSignalBlockEntity getBlockEntity();

	/**
	 * @param clear      alle erreichbaren Folgesignale zeigen Fahrt
	 * @param speedLimit niedrigste angekündigte Geschwindigkeit (in 10 km/h, 0 = keine)
	 * @param direction  angekündigter Buchstabe des Zs2 (1–26 = A–Z, 0 = keiner)
	 */
	record NextSignal(boolean clear, int speedLimit, int direction) {
		public static final NextSignal STOP = new NextSignal(false, 0, 0);
	}

}
