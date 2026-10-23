package de.bausteinchen.railwaysystems.signal.hv;

import java.util.List;

import com.simibubi.create.content.redstone.displayLink.ClickToLinkBlockItem;
import com.simibubi.create.content.trains.signal.SignalBlockEntity;

import de.bausteinchen.railwaysystems.Config;
import de.bausteinchen.railwaysystems.RailwaySystems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;

/**
 * Platziert ein H/V-Signal: zuerst per Rechtsklick an ein Create-Signal binden, dann platzieren.
 * Bindung, Reichweitenprüfung und Hervorhebung des Ziels übernimmt Create's {@link ClickToLinkBlockItem}.
 */
public class HvSignalBlockItem extends ClickToLinkBlockItem {

	public HvSignalBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public int getMaxDistanceFromSelection() {
		return Config.SIGNAL_LINK_RANGE.get();
	}

	@Override
	public String getMessageTranslationKey() {
		// ClickToLinkBlockItem stellt "create." voran
		return RailwaySystems.MOD_ID + ".hv_signal";
	}

	@Override
	public boolean isValidTarget(LevelAccessor level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof SignalBlockEntity;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable(RailwaySystems.MOD_ID + ".hv_signal.tooltip.link")
			.withStyle(ChatFormatting.GRAY));
		if (getBlock() instanceof HvSignalBlock signal && signal.hasMainAspect())
			tooltip.add(Component.translatable(RailwaySystems.MOD_ID + ".hv_signal.tooltip.wrench")
				.withStyle(ChatFormatting.GRAY));
	}

}
