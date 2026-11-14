package de.bausteinchen.railwaysystems.traffic;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;

import de.bausteinchen.railwaysystems.registry.ModBlockEntities;
import de.bausteinchen.railwaysystems.registry.ModBlocks;
import de.bausteinchen.railwaysystems.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Teil des Mastschafts über einem {@link TrafficMastBlock}. Hat kein eigenes Item; Abbauen, Schraubenschlüssel
 * und Pick-Block wirken auf den ganzen Mast.
 * <p>
 * Ein Schaftteil kann eine Fußgängerampel tragen ({@link #PEDESTRIAN}, mit der Fußgängerampel auf den Schaft
 * klicken). Es ist dann selbst eine eigenständige Ampel mit eigenem Block-Entity, die sich wie eine
 * {@link PedestrianLightBlock} an ein Steuergerät binden lässt.
 */
public class TrafficMastPoleBlock extends Block implements EntityBlock, IWrenchable, TrafficLight {

	public static final BooleanProperty PEDESTRIAN = BooleanProperty.create("pedestrian");
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<TrafficAspect> ASPECT = PedestrianLightBlock.ASPECT;

	private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);
	private static final VoxelShape[] PEDESTRIAN_SHAPES = {
		// Süd, West, Nord, Ost (2D-Datenwert); Gehäuse vor dem Schaft
		Shapes.or(SHAPE, Block.box(4, 1, 10, 12, 16, 16)), Shapes.or(SHAPE, Block.box(0, 1, 4, 6, 16, 12)),
		Shapes.or(SHAPE, Block.box(4, 1, 0, 12, 16, 6)), Shapes.or(SHAPE, Block.box(10, 1, 4, 16, 16, 12)) };

	public TrafficMastPoleBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(PEDESTRIAN, false)
			.setValue(FACING, Direction.NORTH)
			.setValue(ASPECT, TrafficAspect.OFF));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(PEDESTRIAN, FACING, ASPECT));
	}

	/**
	 * Fußblock unter einem Schaftteil, oder die Position selbst, wenn keiner gefunden wird.
	 */
	public static BlockPos findBase(BlockGetter level, BlockPos pos) {
		BlockPos p = pos;
		while (level.getBlockState(p)
			.is(ModBlocks.TRAFFIC_MAST_POLE.get()))
			p = p.below();
		return level.getBlockState(p)
			.getBlock() instanceof AbstractMastBlock ? p : pos;
	}

	public static boolean hasPedestrian(BlockState state) {
		return state.is(ModBlocks.TRAFFIC_MAST_POLE.get()) && state.getValue(PEDESTRIAN);
	}

	/**
	 * Fußgängerampel an den Schaft setzen, zeigt in Richtung {@code facing}.
	 */
	public static void attachPedestrian(Level level, BlockPos pos, BlockState state, Direction facing) {
		level.setBlock(pos, state.setValue(PEDESTRIAN, true)
			.setValue(FACING, facing)
			.setValue(ASPECT, TrafficAspect.OFF), 3);
	}

	/**
	 * Fußgängerampel vom Schaft nehmen; ohne Drop.
	 */
	public static void detachPedestrian(Level level, BlockPos pos, BlockState state) {
		if (!hasPedestrian(state))
			return;
		// Block bleibt derselbe: das Block-Entity muss von Hand weg
		IBE.onRemove(state, level, pos, Blocks.AIR.defaultBlockState());
		level.setBlock(pos, state.setValue(PEDESTRIAN, false)
			.setValue(ASPECT, TrafficAspect.OFF), 3);
	}

	/**
	 * Alle Fußgängerampeln eines Mastes ohne Drop entfernen (Abbau im Kreativmodus).
	 */
	public static void detachAllPedestrians(Level level, BlockPos base) {
		for (BlockPos p = base.above(); level.getBlockState(p)
			.is(ModBlocks.TRAFFIC_MAST_POLE.get()); p = p.above())
			detachPedestrian(level, p, level.getBlockState(p));
	}

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(PEDESTRIAN) ? ModBlockEntities.TRAFFIC_LIGHT.get()
			.create(pos, state) : null;
	}

	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
		BlockEntityType<T> type) {
		return state.getValue(PEDESTRIAN) && type == ModBlockEntities.TRAFFIC_LIGHT.get()
			? new SmartBlockEntityTicker<>() : null;
	}

	@Override
	public BlockPos getMainPos(Level level, BlockPos pos, BlockState state) {
		return state.getValue(PEDESTRIAN) ? pos : findBase(level, pos);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
		LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (direction == Direction.DOWN && !neighborState.is(this) && !(neighborState.getBlock() instanceof AbstractMastBlock))
			return Blocks.AIR.defaultBlockState();
		return state;
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		// ganzen Mast abbauen; der Fuß droppt das Item, jede Fußgängerampel ihr eigenes
		BlockPos base = findBase(level, pos);
		if (!level.isClientSide) {
			if (player.isCreative())
				detachAllPedestrians(level, base);
			if (!base.equals(pos))
				level.destroyBlock(base, !player.isCreative(), player);
		}
		return super.playerWillDestroy(level, pos, level.getBlockState(pos)
			.is(this) ? level.getBlockState(pos) : state, player);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (state.getValue(PEDESTRIAN) && !newState.is(this)) {
			IBE.onRemove(state, level, pos, newState);
			// Schaft hat keinen Loot: Fußgängerampel hier zurückgeben, auch wenn der Schaft mit dem Mast fällt
			if (!level.isClientSide)
				popResource(level, pos, new ItemStack(ModItems.PEDESTRIAN_LIGHT.get()));
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
		Player player) {
		if (state.getValue(PEDESTRIAN)) {
			Direction facing = state.getValue(FACING);
			// Klick auf das Gehäuse (vor dem Schaft) wählt die Fußgängerampel
			if (target.getLocation()
				.subtract(pos.getCenter())
				.dot(net.minecraft.world.phys.Vec3.atLowerCornerOf(facing.getNormal())) > 2 / 16.0)
				return new ItemStack(ModItems.PEDESTRIAN_LIGHT.get());
		}
		BlockState base = level.getBlockState(findBase(level, pos));
		return new ItemStack(base.getBlock() instanceof AbstractMastBlock ? base.getBlock() : ModBlocks.TRAFFIC_MAST.get());
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		return redirect(context, false);
	}

	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		// mit Fußgängerampel: nur diese abnehmen, der Mast bleibt stehen
		if (!state.getValue(PEDESTRIAN))
			return redirect(context, true);
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (!level.isClientSide) {
			detachPedestrian(level, pos, state);
			Player player = context.getPlayer();
			if (player != null && !player.isCreative())
				player.getInventory()
					.placeItemBackInInventory(new ItemStack(ModItems.PEDESTRIAN_LIGHT.get()));
			IWrenchable.playRemoveSound(level, pos);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	private InteractionResult redirect(UseOnContext context, boolean sneak) {
		Level level = context.getLevel();
		BlockPos base = findBase(level, context.getClickedPos());
		BlockState baseState = level.getBlockState(base);
		if (!(baseState.getBlock() instanceof AbstractMastBlock mast) || context.getPlayer() == null)
			return InteractionResult.PASS;
		UseOnContext baseContext = new UseOnContext(context.getPlayer(), context.getHand(),
			new BlockHitResult(context.getClickLocation(), context.getClickedFace(), base, context.isInside()));
		return sneak ? mast.onSneakWrenched(baseState, baseContext) : mast.onWrenched(baseState, baseContext);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(PEDESTRIAN) ? PEDESTRIAN_SHAPES[state.getValue(FACING)
			.get2DDataValue()] : SHAPE;
	}

	@Override
	public boolean isPedestrian(BlockState state) {
		return state.getValue(PEDESTRIAN);
	}

	@Override
	public TrafficAspect getAspect(BlockState state) {
		return state.getValue(PEDESTRIAN) ? state.getValue(ASPECT) : TrafficAspect.OFF;
	}

	@Override
	public BlockState withAspect(BlockState state, TrafficAspect aspect) {
		return state.getValue(PEDESTRIAN) ? state.setValue(ASPECT, PedestrianLightBlock.shown(aspect)) : state;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

}
