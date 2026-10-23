package de.bausteinchen.railwaysystems.registry;

import java.util.function.Supplier;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.signal.LinkedSignalBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
		DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RailwaySystems.MOD_ID);

	@SuppressWarnings("DataFlowIssue")
	// Registry-ID "hv_signal" aus v0.1 beibehalten, damit bestehende Welten ihre Bindungen behalten
	public static final Supplier<BlockEntityType<LinkedSignalBlockEntity>> LINKED_SIGNAL = BLOCK_ENTITIES.register("hv_signal",
		() -> BlockEntityType.Builder.of(LinkedSignalBlockEntity::new,
			ModBlocks.HV_HAUPTSIGNAL.get(), ModBlocks.HV_VORSIGNAL.get(), ModBlocks.HV_KOMBISIGNAL.get(),
			ModBlocks.SH_SPERRSIGNAL.get(), ModBlocks.KS_HAUPTSIGNAL.get(), ModBlocks.KS_VORSIGNAL.get(),
			ModBlocks.KS_MEHRABSCHNITTSSIGNAL.get()).build(null));

}
