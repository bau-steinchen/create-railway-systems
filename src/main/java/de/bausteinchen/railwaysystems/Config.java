package de.bausteinchen.railwaysystems;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {

	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	public static final ModConfigSpec.IntValue SIGNAL_LINK_RANGE = BUILDER
		.comment("Maximale Entfernung (Blöcke) zwischen Create-Signal und gebundenem Signal")
		.defineInRange("signalLinkRange", 32, 1, 256);

	public static final ModConfigSpec SPEC = BUILDER.build();

}
