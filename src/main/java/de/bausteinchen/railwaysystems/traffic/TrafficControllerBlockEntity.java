package de.bausteinchen.railwaysystems.traffic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import de.bausteinchen.railwaysystems.crossing.CrossingLinkSlot;
import de.bausteinchen.railwaysystems.registry.ModBlockEntities;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ampel-Steuergerät: läuft den {@link SignalPlan} im Sekundentakt ab und schaltet alle verbundenen
 * Ampeln entsprechend ihrer Signalgruppe. Redstone am Steuergerät schaltet auf Nachtbetrieb
 * (Kfz-Ampeln Gelb blinkend, Fußgängerampeln aus).
 * <p>
 * Kopplung mit dem Bahnübergang über die Funkverbindung hinten (gleiche Frequenz wie die BÜ-Steuerung):
 * Solange der Bahnübergang eingeschaltet ist, zeigen Kfz-Gruppen, die gerade Grün hatten, Gelb und dann
 * Rot, alle anderen Gruppen Rot. Der Umlauf läuft dabei weiter. Danach folgen die Ampeln wieder dem Plan;
 * eine Kfz-Gruppe, deren Grün gerade läuft, zeigt zuerst Rot-Gelb. Der Bahnübergang geht vor Nachtbetrieb.
 */
public class TrafficControllerBlockEntity extends SmartBlockEntity {

	private SignalPlan plan = SignalPlan.DEFAULT;
	/** Ampel (Position relativ zum Steuergerät) -> Index der Signalgruppe */
	private final Map<BlockPos, Integer> lights = new LinkedHashMap<>();
	/** Sekunde im Umlauf */
	private int time;
	private int ticks;
	private boolean night;
	private boolean dirty = true;

	private int receivedStrength;
	/** Ticks seit Einschalten des Bahnübergangs; -1 = aus */
	private int crossingTicks = -1;
	/** Sekunde im Umlauf, als der Bahnübergang einschaltete */
	private int crossingStart;
	/** Gruppen, die nach dem Bahnübergang noch Rot zeigen, bis der Plan sie regulär freigibt (Bitmaske) */
	private int held;
	/** Ticks seit Ausschalten des Bahnübergangs */
	private int releaseTicks;
	/**
	 * Nach dem Laden kommt das Funksignal erst an, wenn auch der Sender geladen ist; so lange einen laufenden
	 * Bahnübergang nicht beenden.
	 */
	private int loadGrace = 20;

	public TrafficControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.TRAFFIC_CONTROLLER.get(), pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		// Gehäuse wie die BÜ-Steuerung: Felder an derselben Stelle der Rückseite
		behaviours.add(LinkBehaviour.receiver(this,
			ValueBoxTransform.Dual.makeSlots(first -> new CrossingLinkSlot(first, 12.75f)),
			strength -> receivedStrength = strength));
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (++ticks >= 20) {
			ticks = 0;
			time = (time + 1) % plan.cycle();
			dirty = true;
		}
		if (level.isClientSide)
			return;
		tickCrossing();
		if (!dirty)
			return;
		dirty = false;
		boolean powered = level.hasNeighborSignal(worldPosition);
		if (powered != night) {
			night = powered;
			sendData();
		}
		applyAspects();
	}

	private void tickCrossing() {
		boolean active = receivedStrength > 0;
		if (loadGrace > 0) {
			loadGrace--;
			if (crossingTicks >= 0)
				active = true;
		}
		if (active && crossingTicks < 0) {
			crossingTicks = 0;
			crossingStart = time;
			dirty = true;
		} else if (!active && crossingTicks >= 0) {
			crossingTicks = -1;
			held = (1 << plan.groups()
				.size()) - 1;
			releaseTicks = 0;
			dirty = true;
		} else if (crossingTicks >= 0) {
			// Gelb endet nach der Gelbzeit des Plans
			if (++crossingTicks == plan.yellow() * 20)
				dirty = true;
		} else if (held != 0) {
			// Rot-Gelb endet nach der Rot-Gelb-Zeit des Plans
			if (++releaseTicks == plan.redYellow() * 20)
				dirty = true;
		}
	}

	/** Begriff einer Signalgruppe jetzt; gibt Gruppen nach dem Bahnübergang frei */
	private TrafficAspect groupAspect(int index) {
		if (index < 0 || index >= plan.groups()
			.size())
			return TrafficAspect.OFF;
		boolean pedestrian = plan.groups()
			.get(index)
			.pedestrian();
		if (crossingTicks >= 0) {
			if (pedestrian)
				return TrafficAspect.RED;
			TrafficAspect atStart = plan.aspectAt(index, crossingStart);
			// Grün: volle Gelbzeit; schon Gelb: Gelb läuft nach Plan zu Ende
			if (atStart == TrafficAspect.GREEN && crossingTicks < plan.yellow() * 20
				|| atStart == TrafficAspect.YELLOW && plan.aspectAt(index, time) == TrafficAspect.YELLOW)
				return TrafficAspect.YELLOW;
			return TrafficAspect.RED;
		}
		if (night)
			return TrafficAspect.YELLOW_BLINK;
		TrafficAspect aspect = plan.aspectAt(index, time);
		int bit = 1 << index;
		if ((held & bit) == 0)
			return aspect;
		// nach dem Bahnübergang nie Rot -> Gelb und nie Rot -> Grün
		if (aspect == TrafficAspect.YELLOW)
			return TrafficAspect.RED;
		if (aspect == TrafficAspect.GREEN && !pedestrian && releaseTicks < plan.redYellow() * 20)
			return TrafficAspect.RED_YELLOW;
		held &= ~bit;
		return aspect;
	}

	private void applyAspects() {
		TrafficAspect[] aspects = new TrafficAspect[plan.groups()
			.size()];
		for (int i = 0; i < aspects.length; i++)
			aspects[i] = groupAspect(i);
		List<BlockPos> gone = new ArrayList<>();
		for (Map.Entry<BlockPos, Integer> entry : lights.entrySet()) {
			BlockPos pos = worldPosition.offset(entry.getKey());
			if (!level.isLoaded(pos))
				continue;
			if (!isLinkedHere(pos)) {
				gone.add(entry.getKey());
				continue;
			}
			int group = entry.getValue();
			TrafficLight.setAspect(level, pos, group < aspects.length ? aspects[group] : TrafficAspect.OFF);
		}
		if (!gone.isEmpty()) {
			gone.forEach(lights::remove);
			notifyUpdate();
		}
	}

	/**
	 * Ob an {@code pos} noch eine Ampel steht, die zu diesem Steuergerät gehört.
	 */
	private boolean isLinkedHere(BlockPos pos) {
		return level.getBlockEntity(pos) instanceof TrafficLightBlockEntity light
			&& worldPosition.equals(light.getControllerPosition());
	}

	public SignalPlan getPlan() {
		return plan;
	}

	public int getTime() {
		return time;
	}

	public boolean isNight() {
		return night;
	}

	/**
	 * Verbundene Ampeln mit absoluter Position.
	 */
	public Map<BlockPos, Integer> getLights() {
		Map<BlockPos, Integer> result = new LinkedHashMap<>();
		lights.forEach((offset, group) -> result.put(worldPosition.offset(offset), group));
		return Collections.unmodifiableMap(result);
	}

	public boolean isLinked(BlockPos lightPos) {
		return lights.containsKey(lightPos.subtract(worldPosition));
	}

	/**
	 * Ampel verbinden. Sie kommt in die erste Gruppe ihrer Art; gibt es keine, wird eine angelegt.
	 *
	 * @return Index der Gruppe
	 */
	public int addLight(BlockPos lightPos, boolean pedestrian) {
		int group = -1;
		List<SignalGroup> groups = plan.groups();
		for (int i = 0; i < groups.size(); i++)
			if (groups.get(i)
				.pedestrian() == pedestrian) {
				group = i;
				break;
			}
		if (group < 0 && groups.size() < SignalPlan.MAX_GROUPS) {
			List<SignalGroup> list = new ArrayList<>(groups);
			long sameKind = groups.stream()
				.filter(g -> g.pedestrian() == pedestrian)
				.count();
			list.add(new SignalGroup((pedestrian ? "F" : "K") + (sameKind + 1), pedestrian, 0, 0, 0));
			plan = plan.withGroups(list);
			group = list.size() - 1;
		}
		lights.put(lightPos.subtract(worldPosition), Math.max(group, 0));
		if (level.getBlockEntity(lightPos) instanceof TrafficLightBlockEntity light)
			light.setControllerPosition(worldPosition);
		changed();
		return Math.max(group, 0);
	}

	public void removeLight(BlockPos lightPos) {
		if (lights.remove(lightPos.subtract(worldPosition)) == null)
			return;
		unlink(lightPos);
		changed();
	}

	private void unlink(BlockPos lightPos) {
		if (level == null || !level.isLoaded(lightPos))
			return;
		if (level.getBlockEntity(lightPos) instanceof TrafficLightBlockEntity light
			&& worldPosition.equals(light.getControllerPosition())) {
			light.setControllerPosition(null);
			TrafficLight.setAspect(level, lightPos, TrafficAspect.OFF);
		}
	}

	/**
	 * Neuen Plan und Gruppenzuordnung aus dem Menü übernehmen. Ampeln, die in {@code assignments} fehlen,
	 * werden getrennt; neue Ampeln kann das Menü nicht hinzufügen.
	 *
	 * @param assignments absolute Ampelposition -> Gruppe
	 */
	public void apply(SignalPlan newPlan, Map<BlockPos, Integer> assignments) {
		plan = newPlan.sanitized();
		for (BlockPos offset : new ArrayList<>(lights.keySet())) {
			BlockPos pos = worldPosition.offset(offset);
			Integer group = assignments.get(pos);
			if (group == null) {
				lights.remove(offset);
				unlink(pos);
			} else
				lights.put(offset, Math.clamp(group, 0, Math.max(0, plan.groups()
					.size() - 1)));
		}
		time %= plan.cycle();
		changed();
	}

	private void changed() {
		dirty = true;
		notifyUpdate();
	}

	@Override
	public void remove() {
		super.remove();
		// Steuergerät abgebaut: Ampeln dunkel schalten und freigeben
		if (level != null && !level.isClientSide)
			lights.keySet()
				.forEach(offset -> unlink(worldPosition.offset(offset)));
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.put("Plan", plan.write());
		ListTag list = new ListTag();
		lights.forEach((offset, group) -> {
			CompoundTag entry = new CompoundTag();
			entry.put("Offset", NbtUtils.writeBlockPos(offset));
			entry.putInt("Group", group);
			list.add(entry);
		});
		tag.put("Lights", list);
		tag.putInt("Time", time);
		tag.putBoolean("Night", night);
		if (!clientPacket) {
			tag.putInt("CrossingTicks", crossingTicks);
			tag.putInt("CrossingStart", crossingStart);
			tag.putInt("Held", held);
			tag.putInt("ReleaseTicks", releaseTicks);
		}
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		plan = SignalPlan.read(tag.getCompound("Plan"));
		lights.clear();
		for (Tag t : tag.getList("Lights", Tag.TAG_COMPOUND)) {
			CompoundTag entry = (CompoundTag) t;
			lights.put(NBTHelper.readBlockPos(entry, "Offset"), entry.getInt("Group"));
		}
		time = tag.getInt("Time") % plan.cycle();
		night = tag.getBoolean("Night");
		if (!clientPacket && tag.contains("CrossingTicks")) {
			crossingTicks = tag.getInt("CrossingTicks");
			crossingStart = tag.getInt("CrossingStart");
			held = tag.getInt("Held");
			releaseTicks = tag.getInt("ReleaseTicks");
		}
		dirty = true;
	}

	@Nullable
	public static TrafficControllerBlockEntity get(net.minecraft.world.level.Level level, BlockPos pos) {
		return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof TrafficControllerBlockEntity be ? be : null;
	}

}
