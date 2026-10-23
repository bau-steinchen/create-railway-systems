package de.bausteinchen.railwaysystems.registry;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.signal.hv.HvSignalBlockItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RailwaySystems.MOD_ID);

	public static final DeferredItem<HvSignalBlockItem> HV_HAUPTSIGNAL = ITEMS.register("hv_hauptsignal",
		() -> new HvSignalBlockItem(ModBlocks.HV_HAUPTSIGNAL.get(), new Item.Properties()));

	public static final DeferredItem<HvSignalBlockItem> HV_VORSIGNAL = ITEMS.register("hv_vorsignal",
		() -> new HvSignalBlockItem(ModBlocks.HV_VORSIGNAL.get(), new Item.Properties()));

	public static final DeferredItem<HvSignalBlockItem> HV_KOMBISIGNAL = ITEMS.register("hv_kombisignal",
		() -> new HvSignalBlockItem(ModBlocks.HV_KOMBISIGNAL.get(), new Item.Properties()));

	public static final DeferredItem<HvSignalBlockItem> SH_SPERRSIGNAL = ITEMS.register("sh_sperrsignal",
		() -> new HvSignalBlockItem(ModBlocks.SH_SPERRSIGNAL.get(), new Item.Properties()));

	public static final DeferredItem<BlockItem> SIGNAL_MAST = ITEMS.registerSimpleBlockItem(ModBlocks.SIGNAL_MAST);

}
