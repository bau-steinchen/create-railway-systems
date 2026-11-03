package de.bausteinchen.railwaysystems.signal;

import java.util.EnumMap;
import java.util.Map;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import de.bausteinchen.railwaysystems.registry.ModBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Gemeinsame Basis der Lichtsignale mit Signalschirm, die auf einem Mast stehen oder mit
 * Auslegern an einer Wand hängen. Der Schirm zeigt in Richtung {@link #FACING} (zum anfahrenden
 * Zug); der Begriff wird von {@link LinkedSignalBlockEntity} aus dem gebundenen Create-Signal
 * abgeleitet.
 * <p>
 * Hohe Signale ({@link #isTall()}) bestehen wie eine Tür aus unterer und oberer Hälfte. Bindung
 * und Logik liegen in der unteren Hälfte; die obere übernimmt deren Zustand und trägt eine
 * eingebaute Funkverbindung ({@link SignalLinkBlockEntity}).
 */
public abstract class MountedSignalBlock extends Block
	implements IBE<LinkedSignalBlockEntity>, IWrenchable, LinkedSignal {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<SignalMount> MOUNT = EnumProperty.create("mount", SignalMount.class);
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

	private static final Map<Direction, Map<SignalMount, VoxelShape>> SHAPES = buildShapes();

	protected MountedSignalBlock(Properties properties) {
		super(properties);
		BlockState state = defaultBlockState().setValue(MOUNT, SignalMount.NONE);
		registerDefaultState(isTall() ? state.setValue(HALF, DoubleBlockHalf.LOWER) : state);
	}

	/**
	 * Ob das Signal zwei Blöcke hoch ist. Wird schon im Konstruktor abgefragt und darf daher nur
	 * eine Konstante liefern.
	 */
	protected boolean isTall() {
		return false;
	}

	public static boolean isUpper(BlockState state) {
		return state.hasProperty(HALF) && state.getValue(HALF) == DoubleBlockHalf.UPPER;
	}

	/**
	 * Position der unteren Hälfte, die Bindung und Logik trägt.
	 */
	public static BlockPos getMainPos(BlockState state, BlockPos pos) {
		return isUpper(state) ? pos.below() : pos;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, MOUNT);
		if (isTall())
			builder.add(HALF);
		super.createBlockStateDefinition(builder);
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		if (isTall()) {
			BlockPos above = context.getClickedPos()
				.above();
			Level level = context.getLevel();
			if (above.getY() >= level.getMaxBuildHeight() || !level.getBlockState(above)
				.canBeReplaced(context))
				return null;
		}

		Direction facing = context.getHorizontalDirection()
			.getOpposite();
		BlockState state = defaultBlockState().setValue(FACING, facing);

		// Gegen einen Block gesetzt: hinten bündig an Wand oder Signalmast, seitlich mit Ausleger
		Direction clickedFace = context.getClickedFace();
		if (clickedFace.getAxis()
			.isHorizontal()) {
			SignalMount mount = SignalMount.fromSupportDirection(facing, clickedFace.getOpposite());
			if (mount != SignalMount.NONE)
				state = state.setValue(MOUNT, attach(context.getLevel(), context.getClickedPos(), facing, mount));
		}
		return state;
	}

	/**
	 * Passende Montage zur gewünschten Seite: hinten ein Signalmast ergibt {@link SignalMount#MAST}
	 * bzw. beim Gittermast {@link SignalMount#MAST_LATTICE}, ein fester Block {@link SignalMount#BACK};
	 * sonst {@link SignalMount#NONE}.
	 *
	 * @param pos Position der unteren Hälfte
	 */
	private SignalMount attach(LevelReader level, BlockPos pos, Direction facing, SignalMount wanted) {
		if (wanted.isFlush()) {
			BlockState mast = mastBehind(level, pos, facing);
			if (mast != null)
				return ((SignalMastBlock) mast.getBlock()).mountFor(mast, facing);
			wanted = SignalMount.BACK;
		}
		return canAttach(level, pos, facing, wanted) ? wanted : SignalMount.NONE;
	}

	/**
	 * Signalmast hinter dem Signal, oder null. Hohe Signale brauchen hinter beiden Hälften einen
	 * Mastblock; Bauform und Ausrichtung gibt der untere vor.
	 */
	@Nullable
	private BlockState mastBehind(LevelReader level, BlockPos pos, Direction facing) {
		BlockPos behind = pos.relative(facing.getOpposite());
		BlockState mast = level.getBlockState(behind);
		if (!(mast.getBlock() instanceof SignalMastBlock))
			return null;
		if (isTall() && !(level.getBlockState(behind.above())
			.getBlock() instanceof SignalMastBlock))
			return null;
		return mast;
	}

	private boolean canAttach(LevelReader level, BlockPos pos, Direction facing, SignalMount mount) {
		Direction support = mount.getSupportDirection(facing);
		if (support == null)
			return true;
		BlockPos supportPos = pos.relative(support);
		return level.getBlockState(supportPos)
			.isFaceSturdy(level, supportPos, support.getOpposite());
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (isTall())
			level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		// Im Kreativmodus soll das Abbauen der oberen Hälfte die untere nicht droppen lassen
		if (!level.isClientSide && player.isCreative() && isUpper(state)) {
			BlockPos below = pos.below();
			BlockState lower = level.getBlockState(below);
			if (lower.is(this) && !isUpper(lower)) {
				level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				level.levelEvent(player, 2001, below, Block.getId(lower));
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
		LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (isTall()) {
			boolean upper = isUpper(state);
			if (direction == (upper ? Direction.DOWN : Direction.UP)) {
				// andere Hälfte entfernt: diese auch
				if (!neighborState.is(this) || isUpper(neighborState) == upper)
					return Blocks.AIR.defaultBlockState();
				// obere Hälfte übernimmt Begriff, Ausrichtung und Montage der unteren; die untere die
				// Montage der oberen, falls deren Mastblock wegfällt
				return upper ? neighborState.setValue(HALF, DoubleBlockHalf.UPPER)
					: state.setValue(MOUNT, neighborState.getValue(MOUNT));
			}
		}

		// Stützblock geändert: Wand, Mast oder freie Aufstellung neu bestimmen
		SignalMount mount = state.getValue(MOUNT);
		Direction facing = state.getValue(FACING);
		if (direction == mount.getSupportDirection(facing))
			return state.setValue(MOUNT, attach(level, getMainPos(state, pos), facing, mount));
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
			// hinten montiert: Kopf (z 2,5..6,5) um den Versatz nach hinten an die Blockkante
			for (SignalMount mount : SignalMount.values())
				if (mount.isFlush())
					byMount.put(mount, rotatedBox(facing, 3, 0, 2.5 + mount.getFlushShift(), 13, 16,
						6.5 + mount.getFlushShift()));
			byMount.put(SignalMount.LEFT, Shapes.or(screen, rotatedBox(facing, 9.5, 1, 6.5, 16, 15, 9.5)));
			byMount.put(SignalMount.RIGHT, Shapes.or(screen, rotatedBox(facing, 0, 1, 6.5, 6.5, 15, 9.5)));
			shapes.put(facing, byMount);
		}
		return shapes;
	}

	/**
	 * Punkt für Bedienfelder an der Seite des Schirms (Modellraum, Pixel), gedreht in Blockkoordinaten.
	 * {@code plusX}: Seite bei x = 13 (aus Sicht des Betrachters vor dem Schirm links), sonst x = 3.
	 * Die Tiefe folgt der Montage: Schirmmitte mittig im Block oder an der hinteren Blockkante.
	 */
	public static Vec3 sideSlot(BlockState state, boolean plusX, double y) {
		double x = plusX ? 13.25 : 2.75;
		// Mitte des Schirms (z 4..6,5), mit dem Kopf nach hinten versetzt
		double z = 5.25 + state.getValue(MOUNT)
			.getFlushShift();
		Direction facing = state.getValue(FACING);
		double rx = switch (facing) {
			case EAST -> 16 - z;
			case SOUTH -> 16 - x;
			case WEST -> z;
			default -> x;
		};
		double rz = switch (facing) {
			case EAST -> x;
			case SOUTH -> 16 - z;
			case WEST -> 16 - x;
			default -> z;
		};
		return new Vec3(rx / 16, y / 16, rz / 16);
	}

	/**
	 * Welt-Seite, auf der {@link #sideSlot} liegt.
	 */
	public static Direction slotSide(BlockState state, boolean plusX) {
		Direction facing = state.getValue(FACING);
		return plusX ? facing.getClockWise() : facing.getCounterClockWise();
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
		// nach dem Drehen an einen passenden Nachbarblock hängen: hinten Mast oder Wand, sonst seitlich
		if (newState.getValue(MOUNT) == SignalMount.NONE)
			return newState;
		Direction facing = newState.getValue(FACING);
		BlockPos main = getMainPos(newState, context.getClickedPos());
		for (SignalMount wanted : new SignalMount[] { SignalMount.BACK, SignalMount.LEFT, SignalMount.RIGHT }) {
			SignalMount mount = attach(context.getLevel(), main, facing, wanted);
			if (mount != SignalMount.NONE)
				return newState.setValue(MOUNT, mount);
		}
		return newState.setValue(MOUNT, SignalMount.NONE);
	}

	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		// Aufnehmen mit dem Schraubenschlüssel immer über die untere Hälfte, die das Item droppt
		if (isUpper(state) && context.getPlayer() != null) {
			BlockPos main = context.getClickedPos()
				.below();
			UseOnContext mainContext = new UseOnContext(context.getPlayer(), context.getHand(),
				new BlockHitResult(context.getClickLocation(), context.getClickedFace(), main, context.isInside()));
			return IWrenchable.super.onSneakWrenched(context.getLevel()
				.getBlockState(main), mainContext);
		}
		return IWrenchable.super.onSneakWrenched(state, context);
	}

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		// die untere Hälfte trägt Bindung und Logik, die obere die eingebaute Funkverbindung
		return isUpper(state) ? ModBlockEntities.SIGNAL_LINK.get()
			.create(pos, state) : IBE.super.newBlockEntity(pos, state);
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
