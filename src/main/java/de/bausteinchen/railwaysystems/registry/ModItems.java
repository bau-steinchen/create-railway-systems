package de.bausteinchen.railwaysystems.registry;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.signal.LinkedSignalBlockItem;

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

}
