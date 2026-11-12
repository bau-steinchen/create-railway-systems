package de.bausteinchen.railwaysystems.client;

import org.jetbrains.annotations.Nullable;

import de.bausteinchen.railwaysystems.traffic.MastConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Einstieg aus gemeinsamem Code in Client-Klassen. Nur aufrufen, wenn {@code FMLEnvironment.dist} der
 * Client ist, sonst würden auf dem Server Client-Klassen geladen.
 */
public class TrafficClientHooks {

	/**
	 * @param pos       platzierter Mast, oder null für das Item in der Hand
	 * @param crosswalk Zebrastreifen-Mast (Schildhöhe statt Ampel am Mast)
	 */
	public static void openMastScreen(@Nullable BlockPos pos, MastConfig config, boolean crosswalk) {
		Minecraft.getInstance()
			.setScreen(new TrafficMastScreen(pos, config, crosswalk));
	}

	public static void openControllerScreen(BlockPos pos) {
		Minecraft.getInstance()
			.setScreen(new TrafficControllerScreen(pos));
	}

}
