package de.bausteinchen.railwaysystems.crossing;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import de.bausteinchen.railwaysystems.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bahnübergangs-Steuerung: Schaltkasten, der wie ein Create-Gleisbeobachter an einen Punkt im Gleis
 * gebunden wird (Item erst auf das Gleis, dann daneben setzen). Nähert sich ein Zug, wird er
 * {@link #ACTIVE}: Er sendet dann auf der Frequenz seiner Funkverbindung und gibt Redstone aus.
 */
public class CrossingControllerBlock extends Block implements IBE<CrossingControllerBlockEntity>, IWrenchable {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	private static final VoxelShape[] SHAPES = {
		// Süd, West, Nord, Ost (2D-Datenwert)
		Block.box(1.5, 0, 3.5, 14.5, 15, 13.5), Block.box(2.5, 0, 1.5, 12.5, 15, 14.5),
		Block.box(1.5, 0, 2.5, 14.5, 15, 12.5), Block.box(3.5, 0, 1.5, 13.5, 15, 14.5) };

	public CrossingControllerBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, ACTIVE));
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection()
			.getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(FACING)
			.get2DDataValue()];
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(ACTIVE) ? 15 : 0;
	}

	@Override
	public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
		return originalState.setValue(FACING, originalState.getValue(FACING)
			.getClockWise());
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
	public Class<CrossingControllerBlockEntity> getBlockEntityClass() {
		return CrossingControllerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CrossingControllerBlockEntity> getBlockEntityType() {
		return ModBlockEntities.CROSSING_CONTROLLER.get();
	}

}
