package de.bausteinchen.railwaysystems.traffic;

import java.util.List;

import com.simibubi.create.content.redstone.displayLink.ClickToLinkBlockItem;
import com.simibubi.create.content.trains.signal.SignalBlockEntity;

import de.bausteinchen.railwaysystems.Config;
import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.client.TrafficClientHooks;
import de.bausteinchen.railwaysystems.registry.ModBlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Item einer Straßenampel. Wie bei den Eisenbahnsignalen bindet ein Rechtsklick auf ein Create-Signal
 * die Ampel daran; anders als dort ist die Bindung freiwillig. Beim Ampelmast öffnet ein Klick in die
 * Luft das Menü für Höhe und Ausladung.
 */
public class TrafficLightBlockItem extends ClickToLinkBlockItem {

	public TrafficLightBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	private boolean isMast() {
		return getBlock() instanceof TrafficMastBlock;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		// Fußgängerampel auf den Schaft eines Ampelmastes: wird dort angebaut statt als eigener Block davor
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		if (!(getBlock() instanceof PedestrianLightBlock) || !state.is(ModBlocks.TRAFFIC_MAST_POLE.get())
			|| state.getValue(TrafficMastPoleBlock.PEDESTRIAN))
			return super.useOn(context);
		if (!level.isClientSide) {
			Direction face = context.getClickedFace();
			Direction facing = face.getAxis()
				.isHorizontal() ? face : context.getHorizontalDirection()
					.getOpposite();
			TrafficMastPoleBlock.attachPedestrian(level, pos, state, facing);
			Player player = context.getPlayer();
			ItemStack stack = context.getItemInHand();
			// Bindung an ein Create-Signal übernehmen, wie beim Platzieren
			updateCustomBlockEntityTag(level, player, pos, stack);
			SoundType sound = level.getBlockState(pos)
				.getSoundType(level, pos, player);
			level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2,
				sound.getPitch() * 0.8f);
			if (player == null || !player.isCreative())
				stack.shrink(1);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!isMast())
			return super.use(level, player, hand);
		if (level.isClientSide && FMLEnvironment.dist == Dist.CLIENT && getBlock() instanceof TrafficMastBlock mast)
			TrafficClientHooks.openMastScreen(null, mast.getConfig(stack), false);
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public boolean placeWhenInvalid() {
		return true;
	}

	@Override
	public int getMaxDistanceFromSelection() {
		return Config.SIGNAL_LINK_RANGE.get();
	}

	@Override
	public String getMessageTranslationKey() {
		// ClickToLinkBlockItem stellt "create." voran
		return RailwaySystems.MOD_ID + ".traffic_light";
	}

	@Override
	public boolean isValidTarget(LevelAccessor level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof SignalBlockEntity;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		String prefix = RailwaySystems.MOD_ID + ".traffic_light.tooltip.";
		if (getBlock() instanceof TrafficMastBlock mast) {
			MastConfig config = mast.getConfig(stack);
			tooltip.add(Component.translatable(prefix + "config", config.height(), config.reach(),
					Component.translatable(prefix + (config.right() ? "right" : "left")))
				.withStyle(ChatFormatting.GOLD));
			tooltip.add(Component.translatable(prefix + "menu")
				.withStyle(ChatFormatting.GRAY));
		}
		if (getBlock() instanceof PedestrianLightBlock)
			tooltip.add(Component.translatable(prefix + "mount")
				.withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable(prefix + "link")
			.withStyle(ChatFormatting.GRAY));
	}

}
