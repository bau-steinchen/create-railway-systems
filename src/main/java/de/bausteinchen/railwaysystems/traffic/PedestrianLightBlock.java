package de.bausteinchen.railwaysystems.traffic;

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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Fußgängerampel (Rot/Grün mit Sinnbild). Sitzt auf einem kurzen Mast; mit Signalmasten darunter
 * beliebig hoch.
 */
public class PedestrianLightBlock extends Block implements IBE<TrafficLightBlockEntity>, IWrenchable, TrafficLight {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<TrafficAspect> ASPECT = EnumProperty.create("aspect", TrafficAspect.class,
		TrafficAspect.OFF, TrafficAspect.RED, TrafficAspect.GREEN);

	private static final VoxelShape[] SHAPES = {
		// Süd, West, Nord, Ost (2D-Datenwert)
		Block.box(4, 0, 6, 12, 16, 12), Block.box(4, 0, 4, 10, 16, 12),
		Block.box(4, 0, 4, 12, 16, 10), Block.box(6, 0, 4, 12, 16, 12) };

	public PedestrianLightBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(ASPECT, TrafficAspect.OFF));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, ASPECT));
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
	public boolean isPedestrian() {
		return true;
	}

	@Override
	public TrafficAspect getAspect(BlockState state) {
		return state.getValue(ASPECT);
	}

	@Override
	public BlockState withAspect(BlockState state, TrafficAspect aspect) {
		TrafficAspect shown = switch (aspect) {
			case OFF, YELLOW_BLINK -> TrafficAspect.OFF;
			case GREEN -> TrafficAspect.GREEN;
			default -> TrafficAspect.RED;
		};
		return state.setValue(ASPECT, shown);
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
	public Class<TrafficLightBlockEntity> getBlockEntityClass() {
		return TrafficLightBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends TrafficLightBlockEntity> getBlockEntityType() {
		return ModBlockEntities.TRAFFIC_LIGHT.get();
	}

}
