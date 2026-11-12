package de.bausteinchen.railwaysystems.registry;

import java.util.function.Supplier;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.crossing.CrossingBarrierBlockEntity;
import de.bausteinchen.railwaysystems.crossing.CrossingLightBlockEntity;
import de.bausteinchen.railwaysystems.signal.LinkedSignalBlockEntity;
import de.bausteinchen.railwaysystems.signal.SignalLinkBlockEntity;
import de.bausteinchen.railwaysystems.traffic.CrosswalkMastBlockEntity;
import de.bausteinchen.railwaysystems.traffic.TrafficControllerBlockEntity;
import de.bausteinchen.railwaysystems.traffic.TrafficLightBlockEntity;

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
			ModBlocks.KS_MEHRABSCHNITTSSIGNAL.get(), ModBlocks.FORM_HAUPTSIGNAL_1.get(),
			ModBlocks.FORM_HAUPTSIGNAL_2.get()).build(null));

	/** Funkverbindung im obersten Teil der hohen Signale */
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<SignalLinkBlockEntity>> SIGNAL_LINK = BLOCK_ENTITIES.register("signal_link",
		() -> BlockEntityType.Builder.of(SignalLinkBlockEntity::new,
			ModBlocks.HV_HAUPTSIGNAL.get(), ModBlocks.HV_KOMBISIGNAL.get(), ModBlocks.KS_HAUPTSIGNAL.get(),
			ModBlocks.KS_MEHRABSCHNITTSSIGNAL.get(), ModBlocks.FORM_HAUPTSIGNAL_1.get(),
			ModBlocks.FORM_HAUPTSIGNAL_2.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<TrafficLightBlockEntity>> TRAFFIC_LIGHT = BLOCK_ENTITIES.register("traffic_light",
		() -> BlockEntityType.Builder.of(TrafficLightBlockEntity::new,
			ModBlocks.TRAFFIC_MAST.get(), ModBlocks.PEDESTRIAN_LIGHT.get()).build(null));

	/** Zebrastreifen-Mast; eigener Typ wegen der eingebauten Funkverbindung */
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<CrosswalkMastBlockEntity>> CROSSWALK_MAST = BLOCK_ENTITIES.register("crosswalk_mast",
		() -> BlockEntityType.Builder.of(CrosswalkMastBlockEntity::new, ModBlocks.CROSSWALK_MAST.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<TrafficControllerBlockEntity>> TRAFFIC_CONTROLLER = BLOCK_ENTITIES.register("traffic_controller",
		() -> BlockEntityType.Builder.of(TrafficControllerBlockEntity::new, ModBlocks.TRAFFIC_CONTROLLER.get()).build(null));

	/** Bahnübergangs-Lichtzeichen und Blinklicht */
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<CrossingLightBlockEntity>> CROSSING_LIGHT = BLOCK_ENTITIES.register("crossing_light",
		() -> BlockEntityType.Builder.of(CrossingLightBlockEntity::new, ModBlocks.BUE_LICHTZEICHEN.get(),
			ModBlocks.BUE_BLINKLICHT.get()).build(null));

	/** Schrankenantrieb */
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<CrossingBarrierBlockEntity>> CROSSING_BARRIER = BLOCK_ENTITIES.register("crossing_barrier",
		() -> BlockEntityType.Builder.of(CrossingBarrierBlockEntity::new, ModBlocks.BUE_SCHRANKE.get()).build(null));

}
