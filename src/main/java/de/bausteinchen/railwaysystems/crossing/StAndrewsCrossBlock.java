package de.bausteinchen.railwaysystems.crossing;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import de.bausteinchen.railwaysystems.signal.SignalMount;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Andreaskreuz (Zeichen 201) auf einem kurzen Mast. Rein dekorativ; mit Signalmasten oder einem
 * Bahnübergangs-Lichtzeichen darunter beliebig hoch. Gegen einen Signalmast gesetzt hängt es ohne eigenen
 * Mast davor ({@link CrossingMount}).
 */
public class StAndrewsCrossBlock extends Block implements IWrenchable {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public static final EnumProperty<SignalMount> MOUNT = CrossingMount.MOUNT;

	private static final VoxelShape POST = Block.box(6.5, 0, 6.5, 9.5, 10, 9.5);
	/** Kreuz vor dem Mast, Index = 2D-Datenwert der Ausrichtung */
	private static final VoxelShape[] CROSSES = CrossingMount.rotatedBoxes(1, 1.5, 5.5, 15, 15.5, 6.75);
	/** An einem Signalmast: Kreuz an der hinteren Blockkante, ohne Pfosten */
	private static final VoxelShape[] MAST_CROSSES = CrossingMount.rotatedBoxes(1, 1.5, 14.75, 15, 15.5, 16);

	public StAndrewsCrossBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(MOUNT, SignalMount.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, MOUNT));
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return CrossingMount.forPlacement(defaultBlockState().setValue(FACING, context.getHorizontalDirection()
			.getOpposite()), context);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
		LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		return CrossingMount.update(state, direction, level, pos);
	}

	@Override
	public BlockState updateAfterWrenched(BlockState newState, UseOnContext context) {
		// gedreht: hängt nur noch am Mast, wenn hinter der neuen Rückseite einer steht
		return newState.setValue(MOUNT, CrossingMount.mountFor(newState, context.getLevel(), context.getClickedPos()));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		int index = state.getValue(FACING)
			.get2DDataValue();
		return state.getValue(MOUNT) == SignalMount.NONE ? Shapes.or(POST, CROSSES[index]) : MAST_CROSSES[index];
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
