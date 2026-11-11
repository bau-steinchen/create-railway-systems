package de.bausteinchen.railwaysystems.traffic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

/**
 * Signalzeitenplan eines Ampel-Steuergeräts: Umlaufzeit und je Signalgruppe ein Grünfenster.
 * Gelb nach dem Grün und Rot-Gelb davor ergänzt der Plan bei Kfz-Gruppen selbst.
 * <p>
 * Alle Zeiten in Sekunden. Der Plan ist unveränderlich; das Steuergerät ersetzt ihn als Ganzes.
 *
 * @param cycle     Umlaufzeit
 * @param yellow    Gelbzeit der Kfz-Gruppen
 * @param redYellow Rot-Gelb-Zeit der Kfz-Gruppen
 * @param clearance Mindestabstand zwischen dem Grünende einer Gruppe und dem Grünbeginn einer
 *                  feindlichen Gruppe (Zwischenzeit, einschließlich Gelb)
 */
public record SignalPlan(int cycle, int yellow, int redYellow, int clearance, List<SignalGroup> groups) {

	public static final int MAX_GROUPS = 12;
	public static final int MIN_CYCLE = 10;
	public static final int MAX_CYCLE = 300;

	public static final SignalPlan DEFAULT = new SignalPlan(60, 3, 1, 4, List.of(
		new SignalGroup("K1", false, 0, 25, 0b10),
		new SignalGroup("K2", false, 30, 55, 0b01)));

	public SignalPlan {
		groups = List.copyOf(groups);
	}

	/**
	 * Begriff einer Gruppe in Sekunde {@code t} des Umlaufs.
	 */
	public TrafficAspect aspectAt(int groupIndex, int t) {
		if (groupIndex < 0 || groupIndex >= groups.size())
			return TrafficAspect.OFF;
		SignalGroup group = groups.get(groupIndex);
		if (isGreen(group, t))
			return TrafficAspect.GREEN;
		if (group.pedestrian())
			return TrafficAspect.RED;
		int length = greenLength(group);
		if (length == 0)
			return TrafficAspect.RED;
		if (Math.floorMod(t - group.greenEnd(), cycle) < yellow)
			return TrafficAspect.YELLOW;
		int untilGreen = Math.floorMod(group.greenStart() - t, cycle);
		if (untilGreen > 0 && untilGreen <= redYellow)
			return TrafficAspect.RED_YELLOW;
		return TrafficAspect.RED;
	}

	public boolean isGreen(SignalGroup group, int t) {
		return Math.floorMod(t - group.greenStart(), cycle) < greenLength(group);
	}

	public int greenLength(SignalGroup group) {
		int start = group.greenStart();
		int end = group.greenEnd();
		if (start == end)
			return 0;
		if (end - start == cycle)
			return cycle;
		return Math.floorMod(end - start, cycle);
	}

	/**
	 * Gruppenpaare, deren Grün sich trotz Feindlichkeit überschneidet oder zu dicht aufeinander folgt.
	 *
	 * @return Paare (i, j) mit i &lt; j
	 */
	public List<int[]> findConflicts() {
		List<int[]> result = new ArrayList<>();
		for (int i = 0; i < groups.size(); i++)
			for (int j = i + 1; j < groups.size(); j++)
				if (groups.get(i)
					.conflictsWith(j) && (followsTooClose(i, j) || followsTooClose(j, i)))
					result.add(new int[] { i, j });
		return result;
	}

	/**
	 * Ob Gruppe {@code b} Grün bekommt, während {@code a} noch Grün hat oder ihr Grün weniger als die
	 * Zwischenzeit zurückliegt.
	 */
	private boolean followsTooClose(int a, int b) {
		SignalGroup ga = groups.get(a);
		SignalGroup gb = groups.get(b);
		if (greenLength(ga) == 0 || greenLength(gb) == 0)
			return false;
		for (int t = 0; t < cycle; t++) {
			if (!isGreen(gb, t))
				continue;
			if (isGreen(ga, t))
				return true;
			int sinceEnd = Math.floorMod(t - ga.greenEnd(), cycle);
			if (sinceEnd < clearance)
				return true;
		}
		return false;
	}

	/**
	 * Werte in die erlaubten Bereiche bringen, Konflikte symmetrisch machen. Pläne vom Client werden
	 * nur so übernommen.
	 */
	public SignalPlan sanitized() {
		int c = Mth.clamp(cycle, MIN_CYCLE, MAX_CYCLE);
		int y = Mth.clamp(yellow, 1, 10);
		int ry = Mth.clamp(redYellow, 0, 5);
		int cl = Mth.clamp(clearance, 0, 30);
		List<SignalGroup> list = new ArrayList<>();
		int count = Math.min(groups.size(), MAX_GROUPS);
		int allBits = (1 << count) - 1;
		for (int i = 0; i < count; i++) {
			SignalGroup g = groups.get(i);
			String name = g.name()
				.strip();
			if (name.length() > SignalGroup.MAX_NAME_LENGTH)
				name = name.substring(0, SignalGroup.MAX_NAME_LENGTH);
			if (name.isEmpty())
				name = (g.pedestrian() ? "F" : "K") + (i + 1);
			int start = Mth.clamp(g.greenStart(), 0, c - 1);
			int end = Mth.clamp(g.greenEnd(), 0, c);
			int mask = g.conflictMask() & allBits & ~(1 << i);
			list.add(new SignalGroup(name, g.pedestrian(), start, end, mask));
		}
		// feindlich ist immer gegenseitig
		for (int i = 0; i < list.size(); i++)
			for (int j = 0; j < list.size(); j++)
				if (list.get(j)
					.conflictsWith(i) && !list.get(i)
						.conflictsWith(j))
					list.set(i, list.get(i)
						.withConflictMask(list.get(i)
							.conflictMask() | 1 << j));
		return new SignalPlan(c, y, ry, cl, list);
	}

	public SignalPlan withGroups(List<SignalGroup> groups) {
		return new SignalPlan(cycle, yellow, redYellow, clearance, groups);
	}

	public SignalPlan withTimes(int cycle, int yellow, int redYellow, int clearance) {
		return new SignalPlan(cycle, yellow, redYellow, clearance, groups);
	}

	/**
	 * Gruppe entfernen; Konfliktbits der folgenden Gruppen rücken nach.
	 */
	public SignalPlan withoutGroup(int index) {
		List<SignalGroup> list = new ArrayList<>();
		for (int i = 0; i < groups.size(); i++) {
			if (i == index)
				continue;
			SignalGroup g = groups.get(i);
			int mask = g.conflictMask();
			int low = mask & ((1 << index) - 1);
			int high = (mask >>> (index + 1)) << index;
			list.add(g.withConflictMask(low | high));
		}
		return withGroups(list);
	}

	public CompoundTag write() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("Cycle", cycle);
		tag.putInt("Yellow", yellow);
		tag.putInt("RedYellow", redYellow);
		tag.putInt("Clearance", clearance);
		ListTag list = new ListTag();
		groups.forEach(g -> list.add(g.write()));
		tag.put("Groups", list);
		return tag;
	}

	public static SignalPlan read(CompoundTag tag) {
		if (!tag.contains("Cycle"))
			return DEFAULT;
		List<SignalGroup> groups = new ArrayList<>();
		tag.getList("Groups", Tag.TAG_COMPOUND)
			.forEach(t -> groups.add(SignalGroup.read((CompoundTag) t)));
		return new SignalPlan(tag.getInt("Cycle"), tag.getInt("Yellow"), tag.getInt("RedYellow"),
			tag.getInt("Clearance"), groups).sanitized();
	}

}
