package de.bausteinchen.railwaysystems.registry;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.signal.hv.HvHauptsignalBlock;
import de.bausteinchen.railwaysystems.signal.hv.HvKombisignalBlock;
import de.bausteinchen.railwaysystems.signal.hv.HvVorsignalBlock;
import de.bausteinchen.railwaysystems.signal.hv.SignalMastBlock;

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

	public static final DeferredBlock<SignalMastBlock> SIGNAL_MAST =
		BLOCKS.register("signal_mast", () -> new SignalMastBlock(signalProperties()));

	private static BlockBehaviour.Properties signalProperties() {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_GRAY)
			.strength(2.0f, 6.0f)
			.sound(SoundType.NETHERITE_BLOCK)
			.requiresCorrectToolForDrops()
			.noOcclusion();
	}

}
