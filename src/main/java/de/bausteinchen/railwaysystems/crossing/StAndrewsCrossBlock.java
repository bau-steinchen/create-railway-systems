package de.bausteinchen.railwaysystems.crossing;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Andreaskreuz (Zeichen 201) auf einem kurzen Mast. Rein dekorativ; mit Signalmasten oder einem
 * Bahnübergangs-Lichtzeichen darunter beliebig hoch.
 */
public class StAndrewsCrossBlock extends Block implements IWrenchable {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	private static final VoxelShape POST = Block.box(6.5, 0, 6.5, 9.5, 10, 9.5);
	private static final VoxelShape[] SHAPES = {
		// Süd, West, Nord, Ost (2D-Datenwert): Kreuz vor dem Mast
		Shapes.or(POST, Block.box(1, 1.5, 9.25, 15, 15.5, 10.5)),
		Shapes.or(POST, Block.box(5.5, 1.5, 1, 6.75, 15.5, 15)),
		Shapes.or(POST, Block.box(1, 1.5, 5.5, 15, 15.5, 6.75)),
		Shapes.or(POST, Block.box(9.25, 1.5, 1, 10.5, 15.5, 15)) };

	public StAndrewsCrossBlock(Properties properties) {
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
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(FACING)
			.get2DDataValue()];
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

}
