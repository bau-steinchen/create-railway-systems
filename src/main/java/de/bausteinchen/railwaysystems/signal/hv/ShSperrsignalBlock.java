package de.bausteinchen.railwaysystems.signal.hv;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import de.bausteinchen.railwaysystems.registry.ModBlockEntities;
import de.bausteinchen.railwaysystems.signal.LinkedSignal;
import de.bausteinchen.railwaysystems.signal.LinkedSignalBlockEntity;
import de.bausteinchen.railwaysystems.signal.SignalInput;
import de.bausteinchen.railwaysystems.signal.SignalMastBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Lichtsperrsignal (Zwergsignal) für Rangierfahrten: Sh0 / Sh1. Steht auf einem kleinen Fuß
 * oder, wenn darunter ein {@link SignalMastBlock} steht, direkt auf dem Mast.
 */
public class ShSperrsignalBlock extends Block implements IBE<LinkedSignalBlockEntity>, IWrenchable, LinkedSignal {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<ShAspect> ASPECT = EnumProperty.create("aspect", ShAspect.class);
	public static final BooleanProperty ON_MAST = BooleanProperty.create("mast");

	private static final VoxelShape SHAPE = Block.box(3.5, 0, 3.5, 12.5, 11, 12.5);

	public ShSperrsignalBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(ASPECT, ShAspect.SH0)
			.setValue(ON_MAST, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, ASPECT, ON_MAST));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection()
			.getOpposite())
			.setValue(ON_MAST, isMast(context.getLevel(), context.getClickedPos()
				.below()));
	}

	private static boolean isMast(BlockGetter level, BlockPos pos) {
		return level.getBlockState(pos)
			.getBlock() instanceof SignalMastBlock;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
		LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (direction == Direction.DOWN)
			return state.setValue(ON_MAST, neighborState.getBlock() instanceof SignalMastBlock);
		return state;
	}

	@Override
	public BlockState updateAspect(BlockState state, SignalInput input) {
		return state.setValue(ASPECT, input.isClear() ? ShAspect.SH1 : ShAspect.SH0);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
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
	public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
		return originalState.setValue(FACING, originalState.getValue(FACING)
			.getClockWise());
	}

	@Override
	public Class<LinkedSignalBlockEntity> getBlockEntityClass() {
		return LinkedSignalBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends LinkedSignalBlockEntity> getBlockEntityType() {
		return ModBlockEntities.LINKED_SIGNAL.get();
	}

}
