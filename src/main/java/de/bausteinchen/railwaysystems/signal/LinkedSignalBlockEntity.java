package de.bausteinchen.railwaysystems.signal;

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
import de.bausteinchen.railwaysystems.signal.SignalInput.NextSignal;

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
 * Bindung an ein Create-Signal. Fragt dessen Zustand regelmäßig ab und lässt den Signalblock
 * ({@link LinkedSignal}) daraus seinen Begriff bestimmen.
 */
public class LinkedSignalBlockEntity extends SmartBlockEntity {

	/**
	 * Geschwindigkeiten (in 10 km/h), die Hauptsignale an ihrem Create-Signal vorgeben. Vorsignale
	 * schlagen hier nach, ob sie eine Geschwindigkeitsbeschränkung ankündigen müssen.
	 */
	private static final Map<GlobalPos, Integer> SPEED_LIMITS = new ConcurrentHashMap<>();

	private static final int CHAIN_REFRESH_INTERVAL = 20;

	@Nullable
	private BlockPos targetOffset;
	private boolean slowMode;

	@Nullable
	private Map<UUID, Boolean> chainedSignals;
	private int chainRefreshTimer;

	public LinkedSignalBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LINKED_SIGNAL.get(), pos, state);
		setLazyTickRate(5);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	public static void clearSpeedLimits() {
		SPEED_LIMITS.clear();
	}

	private static int getSpeedLimit(ResourceKey<Level> dimension, @Nullable BlockPos createSignalPos) {
		if (createSignalPos == null)
			return 0;
		return SPEED_LIMITS.getOrDefault(GlobalPos.of(dimension, createSignalPos), 0);
	}

	private static boolean isClear(SignalState state) {
		return state == SignalState.GREEN || state == SignalState.YELLOW;
	}

	public boolean isSlowMode() {
		return slowMode;
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
		updateAspect();
	}

	public void updateAspect() {
		if (level == null || level.isClientSide)
			return;
		BlockState state = getBlockState();
		if (!(state.getBlock() instanceof LinkedSignal signal))
			return;

		BlockPos target = getTargetPosition();
		SignalBlockEntity createSignal = null;
		if (target != null && level.isLoaded(target)
			&& level.getBlockEntity(target) instanceof SignalBlockEntity sbe)
			createSignal = sbe;

		BlockState newState = signal.updateAspect(state, new Input(createSignal, target));

		int speedLimit = signal.getSpeedLimit(newState, this);
		if (speedLimit >= 0 && target != null)
			SPEED_LIMITS.put(GlobalPos.of(level.dimension(), target), speedLimit);

		if (newState != state)
			level.setBlock(worldPosition, newState, 3);
	}

	private class Input implements SignalInput {

		@Nullable
		private final SignalBlockEntity createSignal;
		@Nullable
		private final BlockPos target;
		@Nullable
		private NextSignal next;

		Input(@Nullable SignalBlockEntity createSignal, @Nullable BlockPos target) {
			this.createSignal = createSignal;
			this.target = target;
		}

		@Override
		public boolean isClear() {
			return createSignal != null && LinkedSignalBlockEntity.isClear(createSignal.getState());
		}

		@Override
		public int getLinkedSpeedLimit() {
			return level == null ? 0 : getSpeedLimit(level.dimension(), target);
		}

		@Override
		public NextSignal getNextSignal() {
			if (next == null)
				next = resolveNextSignal(createSignal);
			return next;
		}

		@Override
		public LinkedSignalBlockEntity getBlockEntity() {
			return LinkedSignalBlockEntity.this;
		}

	}

	/**
	 * Wertet die Signale am Ende des Blockabschnitts hinter dem gebundenen Create-Signal aus.
	 * Bei mehreren erreichbaren Folgesignalen (Weichen) gilt nur dann Fahrt, wenn alle Fahrt
	 * zeigen; angekündigt wird die niedrigste Geschwindigkeit.
	 */
	private NextSignal resolveNextSignal(@Nullable SignalBlockEntity createSignal) {
		if (createSignal == null)
			return NextSignal.STOP;
		SignalBoundary boundary = createSignal.getSignal();
		if (boundary == null)
			return NextSignal.STOP;
		TrackGraph graph = findGraph(boundary);
		if (graph == null)
			return NextSignal.STOP;

		if (chainedSignals == null || chainRefreshTimer-- <= 0) {
			boolean side = boundary.blockEntities.getFirst()
				.containsKey(createSignal.getBlockPos());
			chainedSignals = SignalPropagator.collectChainedSignals(graph, boundary, side);
			chainRefreshTimer = CHAIN_REFRESH_INTERVAL;
		}

		boolean anyFound = false;
		int speedLimit = 0;
		for (Entry<UUID, Boolean> entry : chainedSignals.entrySet()) {
			SignalBoundary next = graph.getPoint(EdgePointType.SIGNAL, entry.getKey());
			if (next == null) {
				chainedSignals = null;
				return NextSignal.STOP;
			}
			boolean sideOfNext = entry.getValue();
			Map<BlockPos, Boolean> signalBlocks = next.blockEntities.get(sideOfNext);
			if (signalBlocks.isEmpty())
				continue;
			if (!isClear(next.cachedStates.get(sideOfNext)))
				return NextSignal.STOP;

			anyFound = true;
			ResourceKey<Level> dimension = next.edgeLocation.getFirst()
				.getDimension();
			for (BlockPos pos : signalBlocks.keySet()) {
				int limit = getSpeedLimit(dimension, pos);
				if (limit > 0 && (speedLimit == 0 || limit < speedLimit))
					speedLimit = limit;
			}
		}

		return anyFound ? new NextSignal(true, speedLimit) : NextSignal.STOP;
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
		BlockState state = getBlockState();
		if (level != null && target != null && state.getBlock() instanceof LinkedSignal signal
			&& signal.getSpeedLimit(state, this) >= 0)
			SPEED_LIMITS.remove(GlobalPos.of(level.dimension(), target));
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
