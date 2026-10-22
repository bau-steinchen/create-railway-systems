package de.bausteinchen.railwaysystems;

import de.bausteinchen.railwaysystems.registry.ModBlockEntities;
import de.bausteinchen.railwaysystems.registry.ModBlocks;
import de.bausteinchen.railwaysystems.registry.ModCreativeTabs;
import de.bausteinchen.railwaysystems.registry.ModItems;
import de.bausteinchen.railwaysystems.signal.hv.HvSignalBlockEntity;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@Mod(RailwaySystems.MOD_ID)
public class RailwaySystems {

	public static final String MOD_ID = "create_railway_systems";

	public RailwaySystems(IEventBus modBus, ModContainer container) {
		ModBlocks.BLOCKS.register(modBus);
		ModItems.ITEMS.register(modBus);
		ModBlockEntities.BLOCK_ENTITIES.register(modBus);
		ModCreativeTabs.TABS.register(modBus);
		container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);

		NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> HvSignalBlockEntity.clearSlowSignals());
	}

	public static ResourceLocation asResource(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

}
