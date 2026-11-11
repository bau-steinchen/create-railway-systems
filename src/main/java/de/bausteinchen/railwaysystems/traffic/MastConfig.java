package de.bausteinchen.railwaysystems.traffic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * Bauform eines Ampelmastes mit Ausleger.
 *
 * @param height   Höhe des senkrechten Mastes in Blöcken, einschließlich Fußblock; darüber beginnt der Bogen
 * @param reach    Ausladung: waagerechter Abstand von der Mastmitte bis zur Ampel am Auslegerende, in Blöcken
 * @param right    Ausleger zeigt aus Sicht des Fahrers nach rechts statt nach links
 * @param mastHead zusätzlicher Signalgeber unten am Mast
 */
public record MastConfig(int height, int reach, boolean right, boolean mastHead) {

	public static final int MIN_HEIGHT = 3;
	public static final int MAX_HEIGHT = 12;
	public static final int MIN_REACH = 2;
	public static final int MAX_REACH = 16;

	public static final MastConfig DEFAULT = new MastConfig(6, 5, false, true);

	public static final Codec<MastConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("height").forGetter(MastConfig::height),
			Codec.INT.fieldOf("reach").forGetter(MastConfig::reach),
			Codec.BOOL.fieldOf("right").forGetter(MastConfig::right),
			Codec.BOOL.fieldOf("mast_head").forGetter(MastConfig::mastHead))
		.apply(i, MastConfig::new));

	public static final StreamCodec<ByteBuf, MastConfig> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, MastConfig::height,
		ByteBufCodecs.VAR_INT, MastConfig::reach,
		ByteBufCodecs.BOOL, MastConfig::right,
		ByteBufCodecs.BOOL, MastConfig::mastHead,
		MastConfig::new);

	/**
	 * Werte in die erlaubten Bereiche bringen; Daten vom Client oder aus alten Welten nie ungeprüft übernehmen.
	 */
	public MastConfig clamped() {
		return new MastConfig(Mth.clamp(height, MIN_HEIGHT, MAX_HEIGHT), Mth.clamp(reach, MIN_REACH, MAX_REACH),
			right, mastHead);
	}

	public CompoundTag write() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("Height", height);
		tag.putInt("Reach", reach);
		tag.putBoolean("Right", right);
		tag.putBoolean("MastHead", mastHead);
		return tag;
	}

	public static MastConfig read(CompoundTag tag) {
		if (!tag.contains("Height"))
			return DEFAULT;
		return new MastConfig(tag.getInt("Height"), tag.getInt("Reach"), tag.getBoolean("Right"),
			tag.getBoolean("MastHead")).clamped();
	}

}
