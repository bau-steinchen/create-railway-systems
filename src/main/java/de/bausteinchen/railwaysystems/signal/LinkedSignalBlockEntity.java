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
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.registry.ModBlockEntities;
import de.bausteinchen.railwaysystems.signal.SignalOptions.Betrieb;
import de.bausteinchen.railwaysystems.signal.SignalOptions.Gegengleis;
import de.bausteinchen.railwaysystems.signal.SignalOptions.Zusatz;
import de.bausteinchen.railwaysystems.signal.SignalInput.NextSignal;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Bindung an ein Create-Signal. Fragt dessen Zustand regelmäßig ab und lässt den Signalblock
 * ({@link LinkedSignal}) daraus seinen Begriff bestimmen.
 */
public class LinkedSignalBlockEntity extends SmartBlockEntity {

	/**
	 * Was Hauptsignale an ihrem Create-Signal vorgeben. Vorsignale schlagen hier nach, ob sie eine
	 * Geschwindigkeitsbeschränkung oder eine Richtung ankündigen müssen.
	 */
	private static final Map<GlobalPos, Announcement> ANNOUNCEMENTS = new ConcurrentHashMap<>();

	/**
	 * @param speedLimit Geschwindigkeit in 10 km/h, 0 = keine Beschränkung
	 * @param direction  leuchtender Buchstabe des Zs2 (1–26 = A–Z), 0 = keiner
	 */
	private record Announcement(int speedLimit, int direction) {
		static final Announcement NONE = new Announcement(0, 0);
	}

	private static final int CHAIN_REFRESH_INTERVAL = 20;

	private static final BehaviourType<SignalOptionBehaviour<?>> ZUSATZ_TYPE = new BehaviourType<>("zusatz");
	private static final BehaviourType<SignalOptionBehaviour<?>> GEGENGLEIS_TYPE = new BehaviourType<>("gegengleis");
	private static final BehaviourType<SignalOptionBehaviour<?>> BETRIEB_TYPE = new BehaviourType<>("betrieb");
	private static final BehaviourType<SignalLetterBehaviour> RICHTUNG_TYPE = new BehaviourType<>("richtung");

	@Nullable
	private BlockPos targetOffset;
	private boolean slowMode;
	/** eingestellte Geschwindigkeit des Zs3 in 10 km/h, 0 = kein Zs3 */
	private int zs3;
	/** angezeigte Geschwindigkeit des Zs3v in 10 km/h, 0 = dunkel */
	private int zs3v;
	/** angezeigter Buchstabe des Zs2v (1–26 = A–Z), 0 = dunkel */
	private int zs2v;
	/** Zs1/Zs7/Zs8 leuchtet */
	private boolean zusatzLit;
	/** Zs2 leuchtet */
	private boolean zs2Lit;
	private boolean displayChanged;

	// werden in addBehaviours() aus dem Konstruktor der Basisklasse gesetzt, daher ohne Initialisierer
	@Nullable
	private SignalOptionBehaviour<Zusatz> zusatz;
	@Nullable
	private SignalOptionBehaviour<Gegengleis> gegengleis;
	@Nullable
	private SignalOptionBehaviour<Betrieb> betrieb;
	@Nullable
	private SignalLetterBehaviour richtung;

	@Nullable
	private Map<UUID, Boolean> chainedSignals;
	private int chainRefreshTimer;

	public LinkedSignalBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LINKED_SIGNAL.get(), pos, state);
		setLazyTickRate(5);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		if (!(getBlockState().getBlock() instanceof LinkedSignal signal))
			return;
		// Wertefelder auf der Rückseite, von hinten gesehen: oben Zs1/Zs7/Zs8 und Zs2, unten Zs6 und Betrieb
		var features = signal.getFeatures();
		if (features.contains(SignalFeature.ZUSATZ)) {
			zusatz = option(Zusatz.class, "zusatz", new SignalValueBox(5.5f, 11.5f), ZUSATZ_TYPE, 1);
			behaviours.add(zusatz);
		}
		if (features.contains(SignalFeature.ZS6)) {
			gegengleis = option(Gegengleis.class, "gegengleis", new SignalValueBox(5.5f, 4.5f), GEGENGLEIS_TYPE, 2);
			behaviours.add(gegengleis);
		}
		if (features.contains(SignalFeature.KENNLICHT)) {
			betrieb = option(Betrieb.class, "betrieb", new SignalValueBox(10.5f, 4.5f), BETRIEB_TYPE, 3);
			behaviours.add(betrieb);
		}
		if (features.contains(SignalFeature.ZS2)) {
			richtung = new SignalLetterBehaviour(label("richtung"), this, new SignalValueBox(10.5f, 11.5f),
				RICHTUNG_TYPE, 4, "Optionrichtung");
			richtung.withCallback(value -> updateAspect());
			behaviours.add(richtung);
		}
	}

	private <E extends Enum<E> & INamedIconOptions> SignalOptionBehaviour<E> option(
		Class<E> options, String name, SignalValueBox slot, BehaviourType<SignalOptionBehaviour<?>> type, int netId) {
		SignalOptionBehaviour<E> behaviour = new SignalOptionBehaviour<>(options, label(name), this, slot, type, netId,
			"Option" + name);
		behaviour.withCallback(value -> updateAspect());
		return behaviour;
	}

	private static Component label(String name) {
		return Component.translatable(RailwaySystems.MOD_ID + ".signal.option." + name);
	}

	public Zusatz getZusatz() {
		return zusatz == null ? Zusatz.NONE : zusatz.get();
	}

	public boolean hasZs6() {
		return gegengleis != null && gegengleis.get() == Gegengleis.ZS6;
	}

	/**
	 * Abgeschaltete Signale zeigen statt ihres Begriffs nur das Kennlicht.
	 */
	public boolean isSwitchedOff() {
		return betrieb != null && betrieb.get() == Betrieb.OFF;
	}

	public boolean isZusatzLit() {
		return zusatzLit;
	}

	/**
	 * Eingestellter Buchstabe des Zs2 (1–26 = A–Z), 0 = kein Richtungsanzeiger.
	 */
	public int getZs2() {
		return richtung == null ? 0 : richtung.getValue();
	}

	public boolean isZs2Lit() {
		return zs2Lit;
	}

	public int getZs2v() {
		return zs2v;
	}

	public int getZs3() {
		return zs3;
	}

	public int cycleZs3() {
		zs3 = (zs3 + 1) % 10;
		displayChanged = true;
		setChanged();
		updateAspect();
		return zs3;
	}

	public int getZs3v() {
		return zs3v;
	}

	/**
	 * Zeigt an Zs3v und Zs2v an, was das angekündigte Signal vorgibt; dunkel, wenn es Halt zeigt.
	 */
	public void announce(NextSignal next) {
		int speed = next.clear() ? next.speedLimit() : 0;
		int direction = next.clear() ? next.direction() : 0;
		if (zs3v == speed && zs2v == direction)
			return;
		zs3v = speed;
		zs2v = direction;
		displayChanged = true;
	}

	private boolean isPowered() {
		if (level == null)
			return false;
		if (isPowered(worldPosition))
			return true;
		// bei zwei Blöcke hohen Signalen zählen auch Redstone an der oberen Hälfte und deren Funkverbindung
		BlockPos upper = worldPosition.above();
		if (!MountedSignalBlock.isUpper(level.getBlockState(upper)))
			return false;
		return isPowered(upper) || level.getBlockEntity(upper) instanceof SignalLinkBlockEntity link && link.isPowered();
	}

	private boolean isPowered(BlockPos pos) {
		if (level.hasNeighborSignal(pos))
			return true;
		// Redstone-Staub leitet nur in Blöcke, mit denen er sich verbindet; daneben liegender Staub zählt trotzdem
		for (Direction direction : Direction.values()) {
			BlockState neighbour = level.getBlockState(pos.relative(direction));
			if (neighbour.is(Blocks.REDSTONE_WIRE) && neighbour.getValue(RedStoneWireBlock.POWER) > 0)
				return true;
		}
		return false;
	}

	@Override
	protected AABB createRenderBoundingBox() {
		// Zusatzanzeigen können bis in die obere Hälfte reichen
		return new AABB(worldPosition).expandTowards(0, 1, 0);
	}

	public static void clearAnnouncements() {
		ANNOUNCEMENTS.clear();
	}

	private static Announcement getAnnouncement(ResourceKey<Level> dimension, @Nullable BlockPos createSignalPos) {
		if (createSignalPos == null)
			return Announcement.NONE;
		return ANNOUNCEMENTS.getOrDefault(GlobalPos.of(dimension, createSignalPos), Announcement.NONE);
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

		// Zs1/Zs7/Zs8 gelten bei Halt, Zs2 bei Fahrt; beide werden über Redstone ausgelöst
		boolean powered = isPowered();
		boolean lit = getZusatz() != Zusatz.NONE && signal.showsStop(newState) && powered;
		boolean directionLit = getZs2() > 0 && signal.showsProceed(newState) && powered;
		if (lit != zusatzLit || directionLit != zs2Lit) {
			zusatzLit = lit;
			zs2Lit = directionLit;
			displayChanged = true;
		}

		int speedLimit = signal.getSpeedLimit(newState, this);
		if (speedLimit >= 0 && target != null)
			ANNOUNCEMENTS.put(GlobalPos.of(level.dimension(), target),
				new Announcement(speedLimit, zs2Lit ? getZs2() : 0));

		if (newState != state)
			level.setBlock(worldPosition, newState, 3);
		if (displayChanged) {
			displayChanged = false;
			sendData();
		}
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
		public NextSignal getLinkedSignal() {
			if (level == null)
				return NextSignal.STOP;
			Announcement announcement = getAnnouncement(level.dimension(), target);
			return new NextSignal(isClear(), announcement.speedLimit(), announcement.direction());
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
	 * zeigen; angekündigt wird die niedrigste Geschwindigkeit und eine Richtung nur, wenn alle
	 * leuchtenden Zs2 denselben Buchstaben zeigen.
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
		int direction = 0;
		boolean conflictingDirections = false;
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
				Announcement announcement = getAnnouncement(dimension, pos);
				int limit = announcement.speedLimit();
				if (limit > 0 && (speedLimit == 0 || limit < speedLimit))
					speedLimit = limit;
				int letter = announcement.direction();
				if (letter > 0 && direction > 0 && letter != direction)
					conflictingDirections = true;
				else if (letter > 0)
					direction = letter;
			}
		}

		if (!anyFound)
			return NextSignal.STOP;
		return new NextSignal(true, speedLimit, conflictingDirections ? 0 : direction);
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
			ANNOUNCEMENTS.remove(GlobalPos.of(level.dimension(), target));
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (targetOffset != null)
			tag.put("TargetOffset", NbtUtils.writeBlockPos(targetOffset));
		tag.putBoolean("SlowMode", slowMode);
		tag.putInt("Zs3", zs3);
		tag.putInt("Zs3v", zs3v);
		tag.putInt("Zs2v", zs2v);
		tag.putBoolean("ZusatzLit", zusatzLit);
		tag.putBoolean("Zs2Lit", zs2Lit);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		targetOffset = tag.contains("TargetOffset") ? NBTHelper.readBlockPos(tag, "TargetOffset") : null;
		slowMode = tag.getBoolean("SlowMode");
		zs3 = tag.getInt("Zs3");
		zs3v = tag.getInt("Zs3v");
		zs2v = tag.getInt("Zs2v");
		zusatzLit = tag.getBoolean("ZusatzLit");
		zs2Lit = tag.getBoolean("Zs2Lit");
	}

}
