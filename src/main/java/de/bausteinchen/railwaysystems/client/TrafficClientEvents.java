package de.bausteinchen.railwaysystems.client;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.simibubi.create.content.trains.signal.SignalBlockEntity;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.traffic.MastConfig;
import de.bausteinchen.railwaysystems.traffic.TrafficControllerBlockEntity;
import de.bausteinchen.railwaysystems.traffic.TrafficLightBlockEntity;
import de.bausteinchen.railwaysystems.traffic.TrafficLightBlockItem;
import de.bausteinchen.railwaysystems.traffic.TrafficLinkerItem;
import de.bausteinchen.railwaysystems.traffic.TrafficMastBlock;

import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Umrisse in der Welt: Vorschau des Ampelmastes beim Platzieren und, mit dem Ampel-Programmierer, das
 * ausgewählte Steuergerät samt verbundenen Ampeln (Farbe je Signalgruppe).
 */
@EventBusSubscriber(modid = RailwaySystems.MOD_ID, value = Dist.CLIENT)
public class TrafficClientEvents {

	private static final int[] GROUP_COLORS = { 0x5DADE2, 0xF5B041, 0x58D68D, 0xEC7063, 0xAF7AC5, 0xF4D03F,
		0x48C9B0, 0xEB984E, 0x85C1E9, 0xD7BDE2, 0xA3E4D7, 0xF1948A };

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || mc.screen != null)
			return;
		ItemStack stack = player.getMainHandItem();
		if (stack.getItem() instanceof TrafficLightBlockItem item && item.getBlock() instanceof TrafficMastBlock)
			showPlacementPreview(mc, player, stack);
		else if (stack.getItem() instanceof TrafficLinkerItem)
			showLinks(mc.level, stack);
	}

	private static void showPlacementPreview(Minecraft mc, LocalPlayer player, ItemStack stack) {
		if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
			return;
		ClientLevel level = mc.level;
		BlockPos clicked = hit.getBlockPos();
		// beim Binden an ein Create-Signal keine Vorschau
		if (level.getBlockEntity(clicked) instanceof SignalBlockEntity)
			return;
		BlockPos pos = level.getBlockState(clicked)
			.canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
		MastConfig config = TrafficMastBlock.getConfig(stack);
		Direction facing = player.getDirection()
			.getOpposite();

		boolean free = level.getBlockState(pos)
			.canBeReplaced();
		for (int i = 1; i < config.height() && free; i++)
			free = level.getBlockState(pos.above(i))
				.canBeReplaced();
		int color = free ? 0x9AC8FF : 0xFF5050;

		for (int i = 0; i < 4; i++) {
			AABB box = previewBoxes(pos, facing, config)[i];
			if (box != null)
				Outliner.getInstance()
					.showAABB("traffic_mast_preview_" + i, box)
					.colored(color)
					.lineWidth(1 / 32f);
		}
	}

	/**
	 * Mastschaft, Ausleger, Signalgeber am Auslegerende und (falls vorhanden) Signalgeber am Mast.
	 */
	public static AABB[] previewBoxes(BlockPos pos, Direction facing, MastConfig config) {
		double side = config.right() ? -1 : 1;
		double armY = config.height() + TrafficMastBlock.BEND_RADIUS;
		double tipX = 0.5 + side * config.reach();
		double px = 2 / 16.0;
		AABB pole = box(pos, facing, 0.5 - px, 0, 0.5 - px, 0.5 + px, config.height(), 0.5 + px);
		AABB arm = box(pos, facing, 0.5, config.height(), 0.5 - px, tipX + side * 3 / 16.0, armY + px, 0.5 + px);
		AABB head = box(pos, facing, tipX - 3 / 16.0, armY - 1.25, 2 / 16.0, tipX + 3 / 16.0, armY, 0.5);
		AABB mastHead = config.mastHead() ? box(pos, facing, 5 / 16.0, 1.25, 0.5 / 16, 11 / 16.0, 2.25, 6 / 16.0) : null;
		return new AABB[] { pole, arm, head, mastHead };
	}

	/**
	 * Quader im Modellraum (Signalgeber nach Norden) in Weltkoordinaten.
	 */
	private static AABB box(BlockPos pos, Direction facing, double x0, double y0, double z0, double x1, double y1,
		double z1) {
		Vec3 a = toWorld(pos, facing, x0, y0, z0);
		Vec3 b = toWorld(pos, facing, x1, y1, z1);
		return new AABB(a, b);
	}

	private static Vec3 toWorld(BlockPos pos, Direction facing, double x, double y, double z) {
		double cx = x - 0.5;
		double cz = z - 0.5;
		double wx;
		double wz;
		switch (facing) {
			case EAST -> {
				wx = -cz;
				wz = cx;
			}
			case SOUTH -> {
				wx = -cx;
				wz = -cz;
			}
			case WEST -> {
				wx = cz;
				wz = -cx;
			}
			default -> {
				wx = cx;
				wz = cz;
			}
		}
		return new Vec3(pos.getX() + 0.5 + wx, pos.getY() + y, pos.getZ() + 0.5 + wz);
	}

	private static void showLinks(ClientLevel level, ItemStack stack) {
		GlobalPos selected = TrafficLinkerItem.getSelected(stack);
		if (selected == null || selected.dimension() != level.dimension())
			return;
		TrafficControllerBlockEntity controller = TrafficControllerBlockEntity.get(level, selected.pos());
		if (controller == null)
			return;
		Outliner.getInstance()
			.showAABB("traffic_link_controller", new AABB(selected.pos()))
			.colored(0xFFFFFF)
			.lineWidth(1 / 16f);
		Set<BlockPos> shown = new HashSet<>();
		for (Map.Entry<BlockPos, Integer> entry : controller.getLights()
			.entrySet()) {
			BlockPos pos = entry.getKey();
			shown.add(pos);
			AABB box = new AABB(pos);
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof TrafficMastBlock && level.getBlockEntity(pos) instanceof TrafficLightBlockEntity be)
				box = box.expandTowards(0, be.getConfig()
					.height() - 1, 0);
			Outliner.getInstance()
				.showAABB("traffic_link_" + pos.asLong(), box.deflate(1 / 32.0))
				.colored(GROUP_COLORS[Math.floorMod(entry.getValue(), GROUP_COLORS.length)])
				.lineWidth(1 / 16f);
		}
	}

}
