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
	 * Geschwindigkeit, die ein H/V- oder Ks-Hauptsignal am gebundenen Create-Signal vorgibt
	 * (in 10 km/h, 0 = keine). Für eigenständige Vorsignale.
	 */
	int getLinkedSpeedLimit();

	/**
	 * Zusammengefasster Zustand der Signale am Ende des Blockabschnitts hinter dem gebundenen
	 * Create-Signal. Für Vorsignale am Mast und Mehrabschnittssignale.
	 */
	NextSignal getNextSignal();

	LinkedSignalBlockEntity getBlockEntity();

	/**
	 * @param clear      alle erreichbaren Folgesignale zeigen Fahrt
	 * @param speedLimit niedrigste angekündigte Geschwindigkeit (in 10 km/h, 0 = keine)
	 */
	record NextSignal(boolean clear, int speedLimit) {
		public static final NextSignal STOP = new NextSignal(false, 0);
	}

}
