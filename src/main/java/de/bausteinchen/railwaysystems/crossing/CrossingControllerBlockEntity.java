package de.bausteinchen.railwaysystems.crossing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;

import de.bausteinchen.railwaysystems.Config;
import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.crossing.CrossingApproachScanner.Result;
import de.bausteinchen.railwaysystems.registry.ModBlockEntities;
import de.bausteinchen.railwaysystems.signal.SignalHolds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Logik der Bahnübergangs-Steuerung: fragt alle {@link #SCAN_INTERVAL} Ticks den
 * {@link CrossingApproachScanner} und hält den Bahnübergang nach dem letzten Treffer noch
 * {@link #HOLD_TICKS} lang eingeschaltet. Lichtzeichen und Schranken auf derselben Funkfrequenz folgen
 * mit ihrem eigenen Ablauf (Gelb, Rot, Schranke senken). Mehrere Steuerungen (z. B. eine je Gleis) auf
 * derselben Frequenz ergänzen sich.
 * <p>
 * Mit dem Programmierer gebundene Create-Signale werden über {@link SignalHolds} auf Halt gehalten:
 * <ul>
 * <li>{@link SignalRole#COVER Deckungssignale} am Gleis, bis der Bahnübergang gesichert ist (Schranken
 * unten, {@link #SECURE_TICKS} nach dem Einschalten). Ein eingeschalteter Bahnübergang bleibt bis dahin und
 * {@link #HOLD_TICKS} darüber hinaus eingeschaltet, damit ein am Deckungssignal haltender Zug ihn nicht
 * ausgehen lässt.</li>
 * <li>{@link SignalRole#ROAD Straßensignale} (Signale auf den Gleisen einer Straße), solange der
 * Bahnübergang eingeschaltet ist und noch {@link #ROAD_RELEASE_TICKS}, bis die Schranken wieder oben
 * sind.</li>
 * </ul>
 */
public class CrossingControllerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

	public static final int SCAN_INTERVAL = 4;
	/** Nachlauf nach dem letzten Treffer: 2 s */
	public static final int HOLD_TICKS = 40;
	/** ab dem Einschalten, bis die Schranken sicher unten sind */
	public static final int SECURE_TICKS = CrossingBarrierBlockEntity.LOWER_DELAY_TICKS
		+ CrossingBarrierBlockEntity.MOVE_TICKS + 10;
	/** nach dem Ausschalten, bis die Schranken wieder oben sind */
	public static final int ROAD_RELEASE_TICKS = CrossingBarrierBlockEntity.MOVE_TICKS;

	public enum SignalRole {
		/** Deckungssignal am Gleis */
		COVER,
		/** Signal einer Straße aus Create-Gleisen */
		ROAD
	}

	public enum DirectionFilter implements INamedIconOptions {
		BOTH(AllIcons.I_TOOL_MOVE_XZ),
		ARROW(AllIcons.I_MTD_RIGHT);

		private final AllIcons icon;

		DirectionFilter(AllIcons icon) {
			this.icon = icon;
		}

		@Override
		public AllIcons getIcon() {
			return icon;
		}

		@Override
		public String getTranslationKey() {
			return RailwaySystems.MOD_ID + ".crossing_controller.direction." + name().toLowerCase();
		}
	}

	private TrackTargetingBehaviour<LevelCrossingPoint> edgePoint;
	private LinkBehaviour link;
	private ScrollOptionBehaviour<DirectionFilter> direction;
	private final CrossingApproachScanner scanner = new CrossingApproachScanner();
	/** gebundene Create-Signale, relativ zur Steuerung */
	private final Map<BlockPos, SignalRole> signals = new LinkedHashMap<>();
	private int holdTicks;
	private int scanCooldown;
	/** Ticks seit dem Einschalten (gedeckelt) */
	private int activeTicks;
	private int roadReleaseTicks;

	public CrossingControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CROSSING_CONTROLLER.get(), pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		edgePoint = new TrackTargetingBehaviour<>(this, LevelCrossingPoint.TYPE);
		behaviours.add(edgePoint);
		link = LinkBehaviour.transmitter(this,
			ValueBoxTransform.Dual.makeSlots(first -> new CrossingLinkSlot(first, 12.75f)), () -> isActive() ? 15 : 0);
		behaviours.add(link);
		direction = new ScrollOptionBehaviour<>(DirectionFilter.class,
			Component.translatable(RailwaySystems.MOD_ID + ".crossing_controller.direction"), this,
			new CenteredSideValueBoxTransform((state, side) -> side == Direction.UP));
		direction.requiresWrench();
		behaviours.add(direction);
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;

		boolean active = isActive();
		if (active)
			activeTicks = Math.min(activeTicks + 1, SECURE_TICKS + HOLD_TICKS);
		else
			activeTicks = 0;

		if (scanCooldown-- <= 0) {
			scanCooldown = SCAN_INTERVAL - 1;
			Result result = scanner.scan(edgePoint.getEdgePoint(), Config.CROSSING_WARNING_TIME.get() * 20,
				Config.CROSSING_SCAN_DISTANCE.get(), isArrowOnly());
			if (result == Result.TRIGGER || (active && result == Result.NEAR))
				holdTicks = HOLD_TICKS;
			else
				holdTicks = Math.max(0, holdTicks - SCAN_INTERVAL);
			// einmal eingeschaltet, läuft der Ablauf mindestens bis zum Sichern und etwas darüber hinaus
			boolean keepOn = active && activeTicks < SECURE_TICKS + HOLD_TICKS;
			setActive(holdTicks > 0 || keepOn);
			if (active && !isActive())
				roadReleaseTicks = ROAD_RELEASE_TICKS;
		}
		if (roadReleaseTicks > 0 && !isActive())
			roadReleaseTicks--;

		// jeden Tick: Create setzt den Eintrag zurück, sobald sich echter Redstone am Signal ändert
		boolean holdCover = isActive() && !isSecured();
		boolean holdRoad = isActive() || roadReleaseTicks > 0;
		signals.forEach((offset, role) -> SignalHolds.set(level, worldPosition.offset(offset), worldPosition,
			role == SignalRole.COVER ? holdCover : holdRoad));
	}

	public boolean isActive() {
		BlockState state = getBlockState();
		return state.hasProperty(CrossingControllerBlock.ACTIVE) && state.getValue(CrossingControllerBlock.ACTIVE);
	}

	/** eingeschaltet und lange genug, dass die Schranken unten sind */
	public boolean isSecured() {
		return isActive() && activeTicks >= SECURE_TICKS;
	}

	public boolean isArrowOnly() {
		return direction != null && direction.get() == DirectionFilter.ARROW;
	}

	private void setActive(boolean active) {
		BlockState state = getBlockState();
		if (!state.hasProperty(CrossingControllerBlock.ACTIVE) || state.getValue(CrossingControllerBlock.ACTIVE) == active)
			return;
		level.setBlock(worldPosition, state.setValue(CrossingControllerBlock.ACTIVE, active), 3);
		level.updateNeighborsAt(worldPosition, state.getBlock());
		link.notifySignalChange();
	}

	/**
	 * Rolle eines Create-Signals weiterschalten: nicht gebunden → Deckungssignal → Straßensignal → nicht
	 * gebunden.
	 *
	 * @return die neue Rolle oder null, wenn das Signal nicht mehr gebunden ist
	 */
	@Nullable
	public SignalRole cycleSignal(BlockPos signal) {
		BlockPos offset = signal.subtract(worldPosition);
		SignalRole role = signals.get(offset);
		SignalRole next = role == null ? SignalRole.COVER : role == SignalRole.COVER ? SignalRole.ROAD : null;
		if (next == null) {
			signals.remove(offset);
			SignalHolds.release(level, signal, worldPosition);
		} else
			signals.put(offset, next);
		setChanged();
		return next;
	}

	private void releaseSignals() {
		// remove() und onChunkUnloaded() laufen auch auf dem Client; Create-Signale gehören dem Server
		if (level == null || level.isClientSide)
			return;
		for (BlockPos offset : signals.keySet())
			SignalHolds.release(level, worldPosition.offset(offset), worldPosition);
	}

	@Override
	public void remove() {
		super.remove();
		releaseSignals();
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		// sonst blieben die Signale rot, solange niemand die Steuerung lädt
		releaseSignals();
	}

	public TrackTargetingBehaviour<LevelCrossingPoint> getEdgePoint() {
		return edgePoint;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		String prefix = RailwaySystems.MOD_ID + ".crossing_controller.";
		tooltip.add(Component.literal("    ")
			.append(Component.translatable(prefix + "title")));
		String status = !isActive() ? "off" : isSecured() ? "secured" : "closing";
		tooltip.add(Component.literal("     ")
			.append(Component.translatable(prefix + "status." + status)
				.withStyle(isActive() ? ChatFormatting.RED : ChatFormatting.GREEN)));
		long cover = signals.values()
			.stream()
			.filter(r -> r == SignalRole.COVER)
			.count();
		tooltip.add(Component.literal("     ")
			.append(Component.translatable(prefix + "signals", cover, signals.size() - cover)
				.withStyle(ChatFormatting.GRAY)));
		return true;
	}

	@Override
	protected AABB createRenderBoundingBox() {
		// die Markierung auf dem Gleis liegt außerhalb des Blocks
		return new AABB(worldPosition).minmax(new AABB(edgePoint.getGlobalPosition()))
			.inflate(2);
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		ListTag list = new ListTag();
		signals.forEach((offset, role) -> {
			CompoundTag entry = new CompoundTag();
			entry.put("Offset", NbtUtils.writeBlockPos(offset));
			entry.putString("Role", role.name());
			list.add(entry);
		});
		tag.put("Signals", list);
		tag.putInt("ActiveTicks", activeTicks);
		if (!clientPacket) {
			tag.putInt("HoldTicks", holdTicks);
			tag.putInt("RoadReleaseTicks", roadReleaseTicks);
		}
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		signals.clear();
		for (Tag t : tag.getList("Signals", Tag.TAG_COMPOUND)) {
			CompoundTag entry = (CompoundTag) t;
			BlockPos offset = NbtUtils.readBlockPos(entry, "Offset")
				.orElse(null);
			if (offset == null)
				continue;
			try {
				signals.put(offset, SignalRole.valueOf(entry.getString("Role")));
			} catch (IllegalArgumentException e) {
				// unbekannte Rolle aus einer anderen Version: Bindung verwerfen
			}
		}
		activeTicks = tag.getInt("ActiveTicks");
		if (!clientPacket) {
			holdTicks = tag.getInt("HoldTicks");
			roadReleaseTicks = tag.getInt("RoadReleaseTicks");
		}
	}

}
