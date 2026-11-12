package de.bausteinchen.railwaysystems.traffic;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import de.bausteinchen.railwaysystems.Config;
import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ampel-Programmierer: Rechtsklick auf ein Steuergerät wählt es aus, danach verbindet (oder trennt)
 * jeder Rechtsklick auf eine Ampel sie mit diesem Steuergerät. Ein Klick in die Luft beendet das Verbinden.
 */
public class TrafficLinkerItem extends Item {

	public TrafficLinkerItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Nullable
	public static GlobalPos getSelected(ItemStack stack) {
		return stack.get(ModDataComponents.SELECTED_CONTROLLER.get());
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();

		if (state.getBlock() instanceof TrafficControllerBlock) {
			if (!level.isClientSide) {
				stack.set(ModDataComponents.SELECTED_CONTROLLER.get(), GlobalPos.of(level.dimension(), pos));
				message(player, "selected", ChatFormatting.GREEN);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}

		GlobalPos selected = getSelected(stack);
		if (!(state.getBlock() instanceof TrafficLight light))
			// bei laufender Auswahl nichts tun, statt die Auswahl wie ein Klick in die Luft zu beenden
			return selected != null ? InteractionResult.FAIL : InteractionResult.PASS;
		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		if (selected == null) {
			message(player, "no_controller", ChatFormatting.RED);
			return InteractionResult.FAIL;
		}
		BlockPos lightPos = light.getMainPos(level, pos, state);
		BlockPos controllerPos = selected.pos();
		TrafficControllerBlockEntity controller = selected.dimension() == level.dimension()
			? TrafficControllerBlockEntity.get(level, controllerPos) : null;
		if (controller == null) {
			stack.remove(ModDataComponents.SELECTED_CONTROLLER.get());
			message(player, "controller_missing", ChatFormatting.RED);
			return InteractionResult.FAIL;
		}
		int range = Config.TRAFFIC_LINK_RANGE.get();
		if (!lightPos.closerThan(controllerPos, range)) {
			message(player, "too_far", ChatFormatting.RED);
			return InteractionResult.FAIL;
		}
		if (!(level.getBlockEntity(lightPos) instanceof TrafficLightBlockEntity lightBe)
			|| !(level.getBlockState(lightPos)
				.getBlock() instanceof TrafficLight mainLight))
			return InteractionResult.FAIL;

		if (controller.isLinked(lightPos) && controllerPos.equals(lightBe.getControllerPosition())) {
			controller.removeLight(lightPos);
			message(player, "removed", ChatFormatting.YELLOW);
			return InteractionResult.SUCCESS;
		}
		// war an einem anderen Steuergerät: dort austragen
		BlockPos previous = lightBe.getControllerPosition();
		if (previous != null && !previous.equals(controllerPos)) {
			TrafficControllerBlockEntity old = TrafficControllerBlockEntity.get(level, previous);
			if (old != null)
				old.removeLight(lightPos);
		}
		int group = controller.addLight(lightPos, mainLight.isPedestrian());
		String name = controller.getPlan()
			.groups()
			.isEmpty() ? "-"
				: controller.getPlan()
					.groups()
					.get(group)
					.name();
		player.displayClientMessage(Component.translatable(key("added"), name)
			.withStyle(ChatFormatting.GREEN), true);
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (getSelected(stack) == null)
			return InteractionResultHolder.pass(stack);
		if (!level.isClientSide) {
			stack.remove(ModDataComponents.SELECTED_CONTROLLER.get());
			message(player, "done", ChatFormatting.GRAY);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return getSelected(stack) != null;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(key("tooltip"))
			.withStyle(ChatFormatting.GRAY));
	}

	private static String key(String name) {
		return RailwaySystems.MOD_ID + ".traffic_linker." + name;
	}

	private static void message(@Nullable Player player, String name, ChatFormatting color) {
		if (player != null)
			player.displayClientMessage(Component.translatable(key(name))
				.withStyle(color), true);
	}

}
