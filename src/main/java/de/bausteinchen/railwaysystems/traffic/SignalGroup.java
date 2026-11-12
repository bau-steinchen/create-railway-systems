package de.bausteinchen.railwaysystems.traffic;

import net.minecraft.nbt.CompoundTag;

/**
 * Signalgruppe eines Steuergeräts: alle Ampeln einer Gruppe zeigen dasselbe.
 *
 * @param name         Anzeigename, z. B. "K1" oder "F2"
 * @param pedestrian   Fußgängergruppe (Rot/Grün ohne Gelb)
 * @param greenStart   Grünbeginn in Sekunden ab Umlaufbeginn
 * @param greenEnd     Grünende in Sekunden; kleiner als der Beginn = Grün über das Umlaufende hinaus,
 *                     gleich dem Beginn = nie Grün
 * @param conflictMask Bit i gesetzt = feindlich zu Gruppe i (darf nicht gleichzeitig Grün haben)
 */
public record SignalGroup(String name, boolean pedestrian, int greenStart, int greenEnd, int conflictMask) {

	public static final int MAX_NAME_LENGTH = 6;

	public boolean conflictsWith(int index) {
		return (conflictMask >> index & 1) != 0;
	}

	public SignalGroup withName(String name) {
		return new SignalGroup(name, pedestrian, greenStart, greenEnd, conflictMask);
	}

	public SignalGroup withPedestrian(boolean pedestrian) {
		return new SignalGroup(name, pedestrian, greenStart, greenEnd, conflictMask);
	}

	public SignalGroup withGreen(int start, int end) {
		return new SignalGroup(name, pedestrian, start, end, conflictMask);
	}

	public SignalGroup withConflictMask(int mask) {
		return new SignalGroup(name, pedestrian, greenStart, greenEnd, mask);
	}

	public CompoundTag write() {
		CompoundTag tag = new CompoundTag();
		tag.putString("Name", name);
		tag.putBoolean("Pedestrian", pedestrian);
		tag.putInt("Start", greenStart);
		tag.putInt("End", greenEnd);
		tag.putInt("Conflicts", conflictMask);
		return tag;
	}

	public static SignalGroup read(CompoundTag tag) {
		return new SignalGroup(tag.getString("Name"), tag.getBoolean("Pedestrian"), tag.getInt("Start"),
			tag.getInt("End"), tag.getInt("Conflicts"));
	}

}
