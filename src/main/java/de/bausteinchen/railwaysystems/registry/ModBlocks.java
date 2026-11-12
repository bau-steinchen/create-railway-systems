package de.bausteinchen.railwaysystems.registry;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.signal.SignalMastBlock;
import de.bausteinchen.railwaysystems.signal.hv.HvHauptsignalBlock;
import de.bausteinchen.railwaysystems.signal.hv.FormHauptsignalBlock;
import de.bausteinchen.railwaysystems.signal.hv.HvKombisignalBlock;
import de.bausteinchen.railwaysystems.signal.hv.HvVorsignalBlock;
import de.bausteinchen.railwaysystems.signal.hv.ShSperrsignalBlock;
import de.bausteinchen.railwaysystems.signal.ks.KsHauptsignalBlock;
import de.bausteinchen.railwaysystems.signal.ks.KsMehrabschnittssignalBlock;
import de.bausteinchen.railwaysystems.signal.ks.KsVorsignalBlock;
import de.bausteinchen.railwaysystems.traffic.CrosswalkMastBlock;
import de.bausteinchen.railwaysystems.traffic.PedestrianLightBlock;
import de.bausteinchen.railwaysystems.traffic.TrafficControllerBlock;
import de.bausteinchen.railwaysystems.traffic.TrafficMastBlock;
import de.bausteinchen.railwaysystems.traffic.TrafficMastPoleBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RailwaySystems.MOD_ID);

	public static final DeferredBlock<HvHauptsignalBlock> HV_HAUPTSIGNAL =
		BLOCKS.register("hv_hauptsignal", () -> new HvHauptsignalBlock(signalProperties()));

	public static final DeferredBlock<HvVorsignalBlock> HV_VORSIGNAL =
		BLOCKS.register("hv_vorsignal", () -> new HvVorsignalBlock(signalProperties()));

	public static final DeferredBlock<HvKombisignalBlock> HV_KOMBISIGNAL =
		BLOCKS.register("hv_kombisignal", () -> new HvKombisignalBlock(signalProperties()));

	public static final DeferredBlock<ShSperrsignalBlock> SH_SPERRSIGNAL =
		BLOCKS.register("sh_sperrsignal", () -> new ShSperrsignalBlock(signalProperties()));

	public static final DeferredBlock<KsHauptsignalBlock> KS_HAUPTSIGNAL =
		BLOCKS.register("ks_hauptsignal", () -> new KsHauptsignalBlock(signalProperties()));

	public static final DeferredBlock<KsVorsignalBlock> KS_VORSIGNAL =
		BLOCKS.register("ks_vorsignal", () -> new KsVorsignalBlock(signalProperties()));

	public static final DeferredBlock<KsMehrabschnittssignalBlock> KS_MEHRABSCHNITTSSIGNAL =
		BLOCKS.register("ks_mehrabschnittssignal", () -> new KsMehrabschnittssignalBlock(signalProperties()));

	public static final DeferredBlock<FormHauptsignalBlock> FORM_HAUPTSIGNAL_1 =
		BLOCKS.register("form_hauptsignal_1", () -> new FormHauptsignalBlock(1, signalProperties()));

	public static final DeferredBlock<FormHauptsignalBlock> FORM_HAUPTSIGNAL_2 =
		BLOCKS.register("form_hauptsignal_2", () -> new FormHauptsignalBlock(2, signalProperties()));

	public static final DeferredBlock<SignalMastBlock> SIGNAL_MAST =
		BLOCKS.register("signal_mast", () -> new SignalMastBlock(SignalMastBlock.Type.STANDARD, signalProperties()));

	public static final DeferredBlock<SignalMastBlock> SIGNAL_MAST_LIGHT =
		BLOCKS.register("signal_mast_light", () -> new SignalMastBlock(SignalMastBlock.Type.LIGHT, signalProperties()));

	public static final DeferredBlock<SignalMastBlock> SIGNAL_MAST_LATTICE =
		BLOCKS.register("signal_mast_lattice",
			() -> new SignalMastBlock(SignalMastBlock.Type.LATTICE, signalProperties()));

	// --- Straßenampeln ---

	public static final DeferredBlock<TrafficMastBlock> TRAFFIC_MAST =
		BLOCKS.register("ampel_mast", () -> new TrafficMastBlock(signalProperties()));

	/** Mastschaft über dem Fußblock; hat kein Item und droppt nichts */
	public static final DeferredBlock<TrafficMastPoleBlock> TRAFFIC_MAST_POLE =
		BLOCKS.register("ampel_mast_schaft", () -> new TrafficMastPoleBlock(signalProperties().noLootTable()));

	public static final DeferredBlock<CrosswalkMastBlock> CROSSWALK_MAST =
		BLOCKS.register("zebrastreifen_mast", () -> new CrosswalkMastBlock(signalProperties()));

	public static final DeferredBlock<PedestrianLightBlock> PEDESTRIAN_LIGHT =
		BLOCKS.register("fussgaengerampel", () -> new PedestrianLightBlock(signalProperties()));

	public static final DeferredBlock<TrafficControllerBlock> TRAFFIC_CONTROLLER =
		BLOCKS.register("ampel_steuergeraet", () -> new TrafficControllerBlock(signalProperties()));

	private static BlockBehaviour.Properties signalProperties() {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_GRAY)
			.strength(2.0f, 6.0f)
			.sound(SoundType.NETHERITE_BLOCK)
			.requiresCorrectToolForDrops()
			.noOcclusion();
	}

}
