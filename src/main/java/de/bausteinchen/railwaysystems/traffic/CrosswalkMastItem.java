package de.bausteinchen.railwaysystems.traffic;

import java.util.List;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.client.TrafficClientHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Item des Zebrastreifen-Mastes. Ein Klick in die Luft öffnet das Menü für Höhe, Ausladung und
 * Schildhöhe; ein Klick auf einen Block setzt den Mast mit diesen Werten.
 */
public class CrosswalkMastItem extends BlockItem {

	public CrosswalkMastItem(CrosswalkMastBlock block, Properties properties) {
		super(block, properties);
	}

	private CrosswalkMastBlock mast() {
		return (CrosswalkMastBlock) getBlock();
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide && FMLEnvironment.dist == Dist.CLIENT)
			TrafficClientHooks.openMastScreen(null, mast().getConfig(stack), true);
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		String prefix = RailwaySystems.MOD_ID + ".traffic_light.tooltip.";
		MastConfig config = mast().getConfig(stack);
		tooltip.add(Component.translatable(prefix + "config", config.height(), config.reach(),
				Component.translatable(prefix + (config.right() ? "right" : "left")))
			.withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.translatable(RailwaySystems.MOD_ID + ".crosswalk_mast.tooltip.sign", config.signHeight())
			.withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.translatable(RailwaySystems.MOD_ID + ".crosswalk_mast.tooltip.menu")
			.withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable(RailwaySystems.MOD_ID + ".crosswalk_mast.tooltip.link")
			.withStyle(ChatFormatting.GRAY));
	}

}
