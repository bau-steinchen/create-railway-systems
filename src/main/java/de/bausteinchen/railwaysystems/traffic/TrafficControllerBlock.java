package de.bausteinchen.railwaysystems.traffic;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import de.bausteinchen.railwaysystems.client.TrafficClientHooks;
import de.bausteinchen.railwaysystems.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Ampel-Steuergerät (Schaltschrank). Rechtsklick öffnet den Signalzeitenplan.
 */
public class TrafficControllerBlock extends Block implements IBE<TrafficControllerBlockEntity>, IWrenchable {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	private static final VoxelShape[] SHAPES = {
		// Süd, West, Nord, Ost (2D-Datenwert); Türseite zeigt nach FACING
		Block.box(2, 0, 4, 14, 15, 13), Block.box(3, 0, 2, 12, 15, 14),
		Block.box(2, 0, 3, 14, 15, 12), Block.box(4, 0, 2, 13, 15, 14) };

	public TrafficControllerBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING));
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection()
			.getOpposite());
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
		Player player, InteractionHand hand, BlockHitResult hitResult) {
		// Programmierer und Schraubenschlüssel wirken über ihr Item, statt das Menü zu öffnen
		if (stack.getItem() instanceof TrafficLinkerItem || AllItems.WRENCH.isIn(stack))
			return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
		BlockHitResult hitResult) {
		if (level.isClientSide && FMLEnvironment.dist == Dist.CLIENT)
			TrafficClientHooks.openControllerScreen(pos);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(FACING)
			.get2DDataValue()];
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		IBE.onRemove(state, level, pos, newState);
	}

	@Override
	public Class<TrafficControllerBlockEntity> getBlockEntityClass() {
		return TrafficControllerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends TrafficControllerBlockEntity> getBlockEntityType() {
		return ModBlockEntities.TRAFFIC_CONTROLLER.get();
	}

}
