package de.bausteinchen.railwaysystems.registry;

import java.util.function.Supplier;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.traffic.MastConfig;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {

	public static final DeferredRegister.DataComponents COMPONENTS =
		DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, RailwaySystems.MOD_ID);

	/** Eingestellte Bauform am Ampelmast-Item */
	public static final Supplier<DataComponentType<MastConfig>> MAST_CONFIG = COMPONENTS.registerComponentType(
		"mast_config", builder -> builder.persistent(MastConfig.CODEC)
			.networkSynchronized(MastConfig.STREAM_CODEC));

	/** Am Ampel-Programmierer ausgewähltes Steuergerät */
	public static final Supplier<DataComponentType<GlobalPos>> SELECTED_CONTROLLER = COMPONENTS.registerComponentType(
		"selected_controller", builder -> builder.persistent(GlobalPos.CODEC)
			.networkSynchronized(GlobalPos.STREAM_CODEC));

}
