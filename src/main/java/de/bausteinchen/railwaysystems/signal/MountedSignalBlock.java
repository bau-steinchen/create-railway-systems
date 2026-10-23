package de.bausteinchen.railwaysystems.signal;

import java.util.EnumMap;
import java.util.Map;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import de.bausteinchen.railwaysystems.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Gemeinsame Basis der Lichtsignale mit Signalschirm, die auf einem Mast stehen oder mit
 * Auslegern an einer Wand hängen. Der Schirm zeigt in Richtung {@link #FACING} (zum anfahrenden
 * Zug); der Begriff wird von {@link LinkedSignalBlockEntity} aus dem gebundenen Create-Signal
 * abgeleitet.
 */
public abstract class MountedSignalBlock extends Block
	implements IBE<LinkedSignalBlockEntity>, IWrenchable, LinkedSignal {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<SignalMount> MOUNT = EnumProperty.create("mount", SignalMount.class);

	private static final Map<Direction, Map<SignalMount, VoxelShape>> SHAPES = buildShapes();

	protected MountedSignalBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(MOUNT, SignalMount.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, MOUNT));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection()
			.getOpposite();
		BlockState state = defaultBlockState().setValue(FACING, facing);

		// Gegen die Seite eines festen Blocks gesetzt: waagerechter Ausleger statt Mast
		Direction clickedFace = context.getClickedFace();
		if (clickedFace.getAxis()
			.isHorizontal()) {
			SignalMount mount = SignalMount.fromSupportDirection(facing, clickedFace.getOpposite());
			if (mount != SignalMount.NONE && canAttach(context.getLevel(), context.getClickedPos(), facing, mount))
				state = state.setValue(MOUNT, mount);
		}
		return state;
	}

	private static boolean canAttach(LevelReader level, BlockPos pos, Direction facing, SignalMount mount) {
		Direction support = mount.getSupportDirection(facing);
		if (support == null)
			return true;
		BlockPos supportPos = pos.relative(support);
		return level.getBlockState(supportPos)
			.isFaceSturdy(level, supportPos, support.getOpposite());
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
		LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		// Stützblock entfernt: zurück auf freie Aufstellung
		SignalMount mount = state.getValue(MOUNT);
		if (direction == mount.getSupportDirection(state.getValue(FACING))
			&& !canAttach(level, pos, state.getValue(FACING), mount))
			return state.setValue(MOUNT, SignalMount.NONE);
		return state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING))
			.get(state.getValue(MOUNT));
	}

	private static Map<Direction, Map<SignalMount, VoxelShape>> buildShapes() {
		Map<Direction, Map<SignalMount, VoxelShape>> shapes = new EnumMap<>(Direction.class);
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			Map<SignalMount, VoxelShape> byMount = new EnumMap<>(SignalMount.class);
			VoxelShape screen = rotatedBox(facing, 3, 0, 3.5, 13, 16, 9.5);
			byMount.put(SignalMount.NONE, screen);
			byMount.put(SignalMount.BACK, Shapes.or(screen, rotatedBox(facing, 6.5, 1, 9.5, 9.5, 15, 16)));
			byMount.put(SignalMount.LEFT, Shapes.or(screen, rotatedBox(facing, 9.5, 1, 6.5, 16, 15, 9.5)));
			byMount.put(SignalMount.RIGHT, Shapes.or(screen, rotatedBox(facing, 0, 1, 6.5, 6.5, 15, 9.5)));
			shapes.put(facing, byMount);
		}
		return shapes;
	}

	/**
	 * Box im Modellraum (Schirm nach Norden) entsprechend der Blockstate-Rotation drehen.
	 */
	private static VoxelShape rotatedBox(Direction facing, double x0, double y0, double z0, double x1, double y1,
		double z1) {
		return switch (facing) {
			case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
			case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
			case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
			default -> Block.box(x0, y0, z0, x1, y1, z1);
		};
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
	public BlockState updateAfterWrenched(BlockState newState, UseOnContext context) {
		// nach dem Drehen den Ausleger an einen passenden Nachbarblock hängen
		if (newState.getValue(MOUNT) == SignalMount.NONE)
			return newState;
		Direction facing = newState.getValue(FACING);
		for (SignalMount mount : new SignalMount[] { SignalMount.BACK, SignalMount.LEFT, SignalMount.RIGHT })
			if (canAttach(context.getLevel(), context.getClickedPos(), facing, mount))
				return newState.setValue(MOUNT, mount);
		return newState.setValue(MOUNT, SignalMount.NONE);
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
