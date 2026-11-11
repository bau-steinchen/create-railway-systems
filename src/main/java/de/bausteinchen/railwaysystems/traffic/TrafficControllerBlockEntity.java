package de.bausteinchen.railwaysystems.traffic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

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

	public TrafficControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.TRAFFIC_CONTROLLER.get(), pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

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
		if (level.isClientSide || !dirty)
			return;
		dirty = false;
		boolean powered = level.hasNeighborSignal(worldPosition);
		if (powered != night) {
			night = powered;
			sendData();
		}
		applyAspects();
	}

	private void applyAspects() {
		List<BlockPos> gone = new ArrayList<>();
		for (Map.Entry<BlockPos, Integer> entry : lights.entrySet()) {
			BlockPos pos = worldPosition.offset(entry.getKey());
			if (!level.isLoaded(pos))
				continue;
			if (!isLinkedHere(pos)) {
				gone.add(entry.getKey());
				continue;
			}
			TrafficAspect aspect = night ? TrafficAspect.YELLOW_BLINK : plan.aspectAt(entry.getValue(), time);
			TrafficLight.setAspect(level, pos, aspect);
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
		dirty = true;
	}

	@Nullable
	public static TrafficControllerBlockEntity get(net.minecraft.world.level.Level level, BlockPos pos) {
		return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof TrafficControllerBlockEntity be ? be : null;
	}

}
