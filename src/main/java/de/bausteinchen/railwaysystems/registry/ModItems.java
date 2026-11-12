package de.bausteinchen.railwaysystems.registry;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.signal.LinkedSignalBlockItem;
import de.bausteinchen.railwaysystems.traffic.CrosswalkMastItem;
import de.bausteinchen.railwaysystems.traffic.TrafficLightBlockItem;
import de.bausteinchen.railwaysystems.traffic.TrafficLinkerItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RailwaySystems.MOD_ID);

	public static final DeferredItem<LinkedSignalBlockItem> HV_HAUPTSIGNAL = ITEMS.register("hv_hauptsignal",
		() -> new LinkedSignalBlockItem(ModBlocks.HV_HAUPTSIGNAL.get(), new Item.Properties()));

	public static final DeferredItem<LinkedSignalBlockItem> HV_VORSIGNAL = ITEMS.register("hv_vorsignal",
		() -> new LinkedSignalBlockItem(ModBlocks.HV_VORSIGNAL.get(), new Item.Properties()));

	public static final DeferredItem<LinkedSignalBlockItem> HV_KOMBISIGNAL = ITEMS.register("hv_kombisignal",
		() -> new LinkedSignalBlockItem(ModBlocks.HV_KOMBISIGNAL.get(), new Item.Properties()));

	public static final DeferredItem<LinkedSignalBlockItem> SH_SPERRSIGNAL = ITEMS.register("sh_sperrsignal",
		() -> new LinkedSignalBlockItem(ModBlocks.SH_SPERRSIGNAL.get(), new Item.Properties()));

	public static final DeferredItem<LinkedSignalBlockItem> KS_HAUPTSIGNAL = ITEMS.register("ks_hauptsignal",
		() -> new LinkedSignalBlockItem(ModBlocks.KS_HAUPTSIGNAL.get(), new Item.Properties()));

	public static final DeferredItem<LinkedSignalBlockItem> KS_VORSIGNAL = ITEMS.register("ks_vorsignal",
		() -> new LinkedSignalBlockItem(ModBlocks.KS_VORSIGNAL.get(), new Item.Properties()));

	public static final DeferredItem<LinkedSignalBlockItem> KS_MEHRABSCHNITTSSIGNAL = ITEMS.register("ks_mehrabschnittssignal",
		() -> new LinkedSignalBlockItem(ModBlocks.KS_MEHRABSCHNITTSSIGNAL.get(), new Item.Properties()));

	public static final DeferredItem<BlockItem> SIGNAL_MAST = ITEMS.registerSimpleBlockItem(ModBlocks.SIGNAL_MAST);

	public static final DeferredItem<TrafficLightBlockItem> TRAFFIC_MAST = ITEMS.register("ampel_mast",
		() -> new TrafficLightBlockItem(ModBlocks.TRAFFIC_MAST.get(), new Item.Properties()));

	public static final DeferredItem<CrosswalkMastItem> CROSSWALK_MAST = ITEMS.register("zebrastreifen_mast",
		() -> new CrosswalkMastItem(ModBlocks.CROSSWALK_MAST.get(), new Item.Properties()));

	public static final DeferredItem<TrafficLightBlockItem> PEDESTRIAN_LIGHT = ITEMS.register("fussgaengerampel",
		() -> new TrafficLightBlockItem(ModBlocks.PEDESTRIAN_LIGHT.get(), new Item.Properties()));

	public static final DeferredItem<BlockItem> TRAFFIC_CONTROLLER = ITEMS.registerSimpleBlockItem(ModBlocks.TRAFFIC_CONTROLLER);

	public static final DeferredItem<TrafficLinkerItem> TRAFFIC_LINKER = ITEMS.register("ampel_programmierer",
		() -> new TrafficLinkerItem(new Item.Properties()));

}
