package de.bausteinchen.railwaysystems.signal;

/**
 * Zusatzanzeigen, die ein Signal tragen kann. Sie werden nicht über Blockzustände, sondern vom
 * {@code SignalOverlayRenderer} aus den Daten des Block-Entity dargestellt, damit die Zahl der
 * Blockzustände klein bleibt.
 */
public enum SignalFeature {
	/** Geschwindigkeitsanzeiger (weiße Ziffer, per Schraubenschlüssel eingestellt) */
	ZS3,
	/** Geschwindigkeitsvoranzeiger (gelbe Ziffer, kündigt das nächste Hauptsignal an) */
	ZS3V,
	/** Richtungsanzeiger (weißer Buchstabe, per Redstone ausgelöst) */
	ZS2,
	/** Richtungsvoranzeiger (gelber Buchstabe, kündigt den Zs2 des nächsten Hauptsignals an) */
	ZS2V,
	/** Ersatzsignal Zs1, Vorsichtsignal Zs7 oder Gegengleis-Ersatzsignal Zs8, per Redstone ausgelöst */
	ZUSATZ,
	/** Gegengleisanzeiger */
	ZS6,
	/** Kennlicht für abgeschaltete Signale */
	KENNLICHT
}
