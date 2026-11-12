package de.bausteinchen.railwaysystems.network;

import java.util.HashMap;
import java.util.Map;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.traffic.SignalPlan;
import de.bausteinchen.railwaysystems.traffic.TrafficControllerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Signalzeitenplan und Gruppenzuordnung aus dem Steuergerät-Menü.
 *
 * @param plan   {@link SignalPlan#write()}
 * @param lights Liste aus {Pos, Group} mit absoluten Ampelpositionen
 */
public record ControllerPayload(BlockPos pos, CompoundTag plan, ListTag lights) implements CustomPacketPayload {

	public static final Type<ControllerPayload> TYPE = new Type<>(RailwaySystems.asResource("traffic_controller"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ControllerPayload> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, ControllerPayload::pos,
		ByteBufCodecs.COMPOUND_TAG, ControllerPayload::plan,
		ByteBufCodecs.COMPOUND_TAG.map(tag -> tag.getList("L", Tag.TAG_COMPOUND), list -> {
			CompoundTag tag = new CompoundTag();
			tag.put("L", list);
			return tag;
		}), ControllerPayload::lights,
		ControllerPayload::new);

	public static ControllerPayload of(BlockPos pos, SignalPlan plan, Map<BlockPos, Integer> lights) {
		ListTag list = new ListTag();
		lights.forEach((lightPos, group) -> {
			CompoundTag entry = new CompoundTag();
			entry.put("Pos", NbtUtils.writeBlockPos(lightPos));
			entry.putInt("Group", group);
			list.add(entry);
		});
		return new ControllerPayload(pos, plan.write(), list);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(ControllerPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Player player = context.player();
			Level level = player.level();
			BlockPos pos = payload.pos();
			if (player.distanceToSqr(pos.getCenter()) > 64)
				return;
			TrafficControllerBlockEntity be = TrafficControllerBlockEntity.get(level, pos);
			if (be == null)
				return;
			Map<BlockPos, Integer> assignments = new HashMap<>();
			for (Tag t : payload.lights()) {
				CompoundTag entry = (CompoundTag) t;
				NbtUtils.readBlockPos(entry, "Pos")
					.ifPresent(lightPos -> assignments.put(lightPos, entry.getInt("Group")));
			}
			be.apply(SignalPlan.read(payload.plan()), assignments);
		});
	}

}
