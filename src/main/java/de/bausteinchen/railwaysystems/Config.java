package de.bausteinchen.railwaysystems;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {

	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	public static final ModConfigSpec.IntValue SIGNAL_LINK_RANGE = BUILDER
		.comment("Maximale Entfernung (Blöcke) zwischen Create-Signal und gebundenem Signal")
		.defineInRange("signalLinkRange", 32, 1, 256);

	public static final ModConfigSpec.IntValue TRAFFIC_LINK_RANGE = BUILDER
		.comment("Maximale Entfernung (Blöcke) zwischen Ampel-Steuergerät und verbundener Ampel")
		.defineInRange("trafficLinkRange", 64, 1, 256);

	public static final ModConfigSpec.IntValue CROSSING_WARNING_TIME = BUILDER
		.comment("Bahnübergang: so viele Sekunden vor Ankunft eines Zuges schaltet die Steuerung ein",
			"(Lichtzeichen 3 s Gelb, Schranke senkt nach 5 s innerhalb von 6 s)")
		.defineInRange("crossingWarningTime", 20, 5, 120);

	public static final ModConfigSpec.IntValue CROSSING_SCAN_DISTANCE = BUILDER
		.comment("Bahnübergang: bis zu dieser Gleisentfernung (Blöcke) sucht die Steuerung nach Zügen.",
			"Sollte mindestens Vorwarnzeit x Höchstgeschwindigkeit der Züge sein.")
		.defineInRange("crossingScanDistance", 600, 32, 4096);

	public static final ModConfigSpec SPEC = BUILDER.build();

}
