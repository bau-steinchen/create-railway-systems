package de.bausteinchen.railwaysystems.signal.hv;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.content.trains.signal.SignalBlockEntity.SignalState;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.SignalPropagator;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import de.bausteinchen.railwaysystems.registry.ModBlockEntities;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Spiegelt den Zustand des gebundenen Create-Signals als H/V-Signalbegriff.
 */
public class HvSignalBlockEntity extends SmartBlockEntity {

	/**
	 * Create-Signale, deren H/V-Hauptsignal auf Langsamfahrt (Hp2) eingestellt ist. Vorsignale
	 * schlagen hier nach, ob sie Vr2 statt Vr1 zeigen müssen.
	 */
	private static final Map<GlobalPos, Boolean> SLOW_SIGNALS = new ConcurrentHashMap<>();

	private static final int CHAIN_REFRESH_INTERVAL = 20;

	@Nullable
	private BlockPos targetOffset;
	private boolean slowMode;

	@Nullable
	private Map<UUID, Boolean> chainedSignals;
	private int chainRefreshTimer;

	public HvSignalBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.HV_SIGNAL.get(), pos, state);
		setLazyTickRate(5);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	public static void clearSlowSignals() {
		SLOW_SIGNALS.clear();
	}

	private static boolean isSlow(ResourceKey<Level> dimension, BlockPos createSignalPos) {
		return SLOW_SIGNALS.getOrDefault(GlobalPos.of(dimension, createSignalPos), false);
	}

	private static boolean isClear(SignalState state) {
		return state == SignalState.GREEN || state == SignalState.YELLOW;
	}

	public boolean toggleSlowMode() {
		slowMode = !slowMode;
		setChanged();
		updateAspect();
		return slowMode;
	}

	@Nullable
	public BlockPos getTargetPosition() {
		return targetOffset == null ? null : worldPosition.offset(targetOffset);
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (level == null || level.isClientSide)
			return;
		updateAspect();
	}

	private void updateAspect() {
		if (level == null || level.isClientSide)
			return;

		BlockPos target = getTargetPosition();
		SignalBlockEntity createSignal = null;
		if (target != null && level.isLoaded(target)
			&& level.getBlockEntity(target) instanceof SignalBlockEntity sbe)
			createSignal = sbe;

		boolean clear = createSignal != null && isClear(createSignal.getState());
		BlockState state = getBlockState();
		BlockState newState = state;

		if (state.hasProperty(HvSignalBlock.MAIN_ASPECT)) {
			HpAspect main = !clear ? HpAspect.HP0 : slowMode ? HpAspect.HP2 : HpAspect.HP1;
			newState = newState.setValue(HvSignalBlock.MAIN_ASPECT, main);
			if (target != null)
				SLOW_SIGNALS.put(GlobalPos.of(level.dimension(), target), slowMode);

			if (state.hasProperty(HvSignalBlock.DISTANT_ASPECT)) {
				VrAspect distant = main == HpAspect.HP0 ? VrAspect.DARK : resolveNextSignal(createSignal);
				newState = newState.setValue(HvSignalBlock.DISTANT_ASPECT, distant);
			}
		}

		if (state.hasProperty(HvVorsignalBlock.ASPECT)) {
			VrAspect distant = !clear ? VrAspect.VR0
				: isSlow(level.dimension(), target) ? VrAspect.VR2 : VrAspect.VR1;
			newState = newState.setValue(HvVorsignalBlock.ASPECT, distant);
		}

		if (state.hasProperty(ShSperrsignalBlock.ASPECT))
			newState = newState.setValue(ShSperrsignalBlock.ASPECT, clear ? ShAspect.SH1 : ShAspect.SH0);

		if (newState != state)
			level.setBlock(worldPosition, newState, 3);
	}

	/**
	 * Ermittelt den Vorsignalbegriff aus den Signalen am Ende des Blockabschnitts hinter dem
	 * gebundenen Create-Signal. Bei mehreren erreichbaren Folgesignalen (Weichen) wird nur dann
	 * Fahrt erwartet, wenn alle Fahrt zeigen.
	 */
	private VrAspect resolveNextSignal(@Nullable SignalBlockEntity createSignal) {
		if (createSignal == null)
			return VrAspect.VR0;
		SignalBoundary boundary = createSignal.getSignal();
		if (boundary == null)
			return VrAspect.VR0;
		TrackGraph graph = findGraph(boundary);
		if (graph == null)
			return VrAspect.VR0;

		if (chainedSignals == null || chainRefreshTimer-- <= 0) {
			boolean side = boundary.blockEntities.getFirst()
				.containsKey(createSignal.getBlockPos());
			chainedSignals = SignalPropagator.collectChainedSignals(graph, boundary, side);
			chainRefreshTimer = CHAIN_REFRESH_INTERVAL;
		}

		boolean anyFound = false;
		boolean anySlow = false;
		for (Entry<UUID, Boolean> entry : chainedSignals.entrySet()) {
			SignalBoundary next = graph.getPoint(EdgePointType.SIGNAL, entry.getKey());
			if (next == null) {
				chainedSignals = null;
				return VrAspect.VR0;
			}
			boolean sideOfNext = entry.getValue();
			Map<BlockPos, Boolean> signalBlocks = next.blockEntities.get(sideOfNext);
			if (signalBlocks.isEmpty())
				continue;
			if (!isClear(next.cachedStates.get(sideOfNext)))
				return VrAspect.VR0;

			anyFound = true;
			ResourceKey<Level> dimension = next.edgeLocation.getFirst()
				.getDimension();
			for (BlockPos pos : signalBlocks.keySet())
				anySlow |= isSlow(dimension, pos);
		}

		if (!anyFound)
			return VrAspect.VR0;
		return anySlow ? VrAspect.VR2 : VrAspect.VR1;
	}

	@Nullable
	private static TrackGraph findGraph(SignalBoundary boundary) {
		for (TrackGraph graph : Create.RAILWAYS.trackNetworks.values())
			if (graph.getPoint(EdgePointType.SIGNAL, boundary.getId()) != null)
				return graph;
		return null;
	}

	@Override
	public void remove() {
		super.remove();
		BlockPos target = getTargetPosition();
		if (level != null && target != null && getBlockState().hasProperty(HvSignalBlock.MAIN_ASPECT))
			SLOW_SIGNALS.remove(GlobalPos.of(level.dimension(), target));
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (targetOffset != null)
			tag.put("TargetOffset", NbtUtils.writeBlockPos(targetOffset));
		tag.putBoolean("SlowMode", slowMode);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		targetOffset = tag.contains("TargetOffset") ? NBTHelper.readBlockPos(tag, "TargetOffset") : null;
		slowMode = tag.getBoolean("SlowMode");
	}

}
