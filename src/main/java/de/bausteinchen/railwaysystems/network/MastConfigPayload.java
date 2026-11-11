package de.bausteinchen.railwaysystems.network;

import java.util.Optional;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.registry.ModDataComponents;
import de.bausteinchen.railwaysystems.traffic.MastConfig;
import de.bausteinchen.railwaysystems.traffic.TrafficLightBlockEntity;
import de.bausteinchen.railwaysystems.traffic.TrafficLightBlockItem;
import de.bausteinchen.railwaysystems.traffic.TrafficMastBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Bauform aus dem Mast-Menü: ohne Position für das Item in der Hand, mit Position für einen
 * platzierten Mast.
 */
public record MastConfigPayload(Optional<BlockPos> pos, MastConfig config) implements CustomPacketPayload {

	public static final Type<MastConfigPayload> TYPE = new Type<>(RailwaySystems.asResource("mast_config"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MastConfigPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.optional(BlockPos.STREAM_CODEC), MastConfigPayload::pos,
		MastConfig.STREAM_CODEC, MastConfigPayload::config,
		MastConfigPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(MastConfigPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Player player = context.player();
			MastConfig config = payload.config()
				.clamped();
			if (payload.pos()
				.isEmpty()) {
				for (InteractionHand hand : InteractionHand.values()) {
					ItemStack stack = player.getItemInHand(hand);
					if (stack.getItem() instanceof TrafficLightBlockItem item && item.getBlock() instanceof TrafficMastBlock) {
						stack.set(ModDataComponents.MAST_CONFIG.get(), config);
						return;
					}
				}
				return;
			}

			BlockPos pos = payload.pos()
				.get();
			Level level = player.level();
			if (!level.isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > 64
				|| !(level.getBlockEntity(pos) instanceof TrafficLightBlockEntity be)
				|| !(be.getBlockState()
					.getBlock() instanceof TrafficMastBlock))
				return;
			if (!TrafficMastBlock.placePole(level, pos, config.height())) {
				player.displayClientMessage(Component.translatable(RailwaySystems.MOD_ID + ".traffic_mast.no_space")
					.withStyle(ChatFormatting.RED), true);
				return;
			}
			be.setConfig(config);
		});
	}

}
