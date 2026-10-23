package de.bausteinchen.railwaysystems.registry;

import java.util.function.Supplier;

import de.bausteinchen.railwaysystems.RailwaySystems;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {

	public static final DeferredRegister<CreativeModeTab> TABS =
		DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RailwaySystems.MOD_ID);

	public static final Supplier<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
		.title(Component.translatable("itemGroup." + RailwaySystems.MOD_ID))
		.icon(() -> ModItems.HV_HAUPTSIGNAL.get().getDefaultInstance())
		.displayItems((params, output) -> {
			output.accept(ModItems.HV_HAUPTSIGNAL.get());
			output.accept(ModItems.HV_VORSIGNAL.get());
			output.accept(ModItems.HV_KOMBISIGNAL.get());
			output.accept(ModItems.SH_SPERRSIGNAL.get());
			output.accept(ModItems.SIGNAL_MAST.get());
		})
		.build());

}
