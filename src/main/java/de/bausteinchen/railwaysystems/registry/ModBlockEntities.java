package de.bausteinchen.railwaysystems.registry;

import java.util.function.Supplier;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.signal.hv.HvSignalBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
		DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RailwaySystems.MOD_ID);

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<HvSignalBlockEntity>> HV_SIGNAL = BLOCK_ENTITIES.register("hv_signal",
		() -> BlockEntityType.Builder.of(HvSignalBlockEntity::new,
			ModBlocks.HV_HAUPTSIGNAL.get(), ModBlocks.HV_VORSIGNAL.get(), ModBlocks.HV_KOMBISIGNAL.get(),
			ModBlocks.SH_SPERRSIGNAL.get()).build(null));

}
