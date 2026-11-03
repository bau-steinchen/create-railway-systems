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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
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
 * Hohe Signale ({@link #getHeight()} > 1) bestehen wie eine Tür aus übereinanderliegenden Teilen:
 * zwei Blöcke hohe aus unterer und oberer Hälfte ({@link #HALF}), drei Blöcke hohe aus unterem,
 * mittlerem und oberem Teil ({@link #PART}). Bindung und Logik liegen im untersten Teil; die Teile
 * darüber übernehmen dessen Zustand, der oberste trägt eine eingebaute Funkverbindung
 * ({@link SignalLinkBlockEntity}).
 */
public abstract class MountedSignalBlock extends Block
	implements IBE<LinkedSignalBlockEntity>, IWrenchable, LinkedSignal {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<SignalMount> MOUNT = EnumProperty.create("mount", SignalMount.class);
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
	/** Teil eines drei Blöcke hohen Signals. */
	public static final EnumProperty<SignalPart> PART = EnumProperty.create("part", SignalPart.class);
	/**
	 * Neigung am gedrehten Signalmast in 22,5°-Stufen gegenüber {@link #FACING}, siehe
	 * {@link #tiltSteps}. Nur bei {@link SignalMount#isMast()}, sonst 0.
	 */
	public static final IntegerProperty TILT = IntegerProperty.create("tilt", 0, 3);

	/** Mastmitte im Modellraum (Pixel), um die sich ein Signal am gedrehten Mast dreht. */
	private static final double PIVOT_X = 8, PIVOT_Z = 24;

	private static final Map<Direction, Map<SignalMount, VoxelShape[]>> SHAPES = buildShapes();

	protected MountedSignalBlock(Properties properties) {
		super(properties);
		BlockState state = defaultBlockState().setValue(MOUNT, SignalMount.NONE)
			.setValue(TILT, 0);
		registerDefaultState(withPart(state, 0));
	}

	/**
	 * Ob das Signal zwei Blöcke hoch ist. Wird schon im Konstruktor abgefragt und darf daher nur
	 * eine Konstante liefern.
	 */
	protected boolean isTall() {
		return false;
	}

	/**
	 * Höhe in Blöcken (1–3). Wird schon im Konstruktor abgefragt und darf daher nur eine Konstante
	 * liefern.
	 */
	protected int getHeight() {
		return isTall() ? 2 : 1;
	}

	/**
	 * Anzahl der Blockteile dieses Signals laut Zustand.
	 */
	public static int partCount(BlockState state) {
		return state.hasProperty(PART) ? 3 : state.hasProperty(HALF) ? 2 : 1;
	}

	/**
	 * Teil von unten gezählt, 0 = unterster Teil mit Bindung und Logik.
	 */
	public static int partIndex(BlockState state) {
		if (state.hasProperty(PART))
			return state.getValue(PART)
				.ordinal();
		return state.hasProperty(HALF) && state.getValue(HALF) == DoubleBlockHalf.UPPER ? 1 : 0;
	}

	private static BlockState withPart(BlockState state, int part) {
		if (state.hasProperty(PART))
			return state.setValue(PART, SignalPart.values()[part]);
		if (state.hasProperty(HALF))
			return state.setValue(HALF, part == 0 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
		return state;
	}

	/**
	 * Oberster Teil eines hohen Signals, der die Funkverbindung trägt.
	 */
	public static boolean isUpper(BlockState state) {
		int count = partCount(state);
		return count > 1 && partIndex(state) == count - 1;
	}

	/**
	 * Position des untersten Teils, der Bindung und Logik trägt.
	 */
	public static BlockPos getMainPos(BlockState state, BlockPos pos) {
		return pos.below(partIndex(state));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, MOUNT, TILT);
		if (getHeight() == 2)
			builder.add(HALF);
		else if (getHeight() == 3)
			builder.add(PART);
		super.createBlockStateDefinition(builder);
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		for (int part = 1; part < getHeight(); part++) {
			BlockPos above = context.getClickedPos()
				.above(part);
			if (above.getY() >= level.getMaxBuildHeight() || !level.getBlockState(above)
				.canBeReplaced(context))
				return null;
		}

		Direction facing = context.getHorizontalDirection()
			.getOpposite();
		Direction clickedFace = context.getClickedFace();
		// An einen Signalmast geklickt: Signal zeigt vom Mast weg, egal wie schräg der Spieler schaut
		if (clickedFace.getAxis()
			.isHorizontal() && context.getLevel()
				.getBlockState(context.getClickedPos()
					.relative(clickedFace.getOpposite()))
				.getBlock() instanceof SignalMastBlock)
			facing = clickedFace;
		BlockState state = defaultBlockState().setValue(FACING, facing);

		// Gegen einen Block gesetzt: hinten bündig an Wand oder Signalmast, seitlich mit Ausleger
		if (clickedFace.getAxis()
			.isHorizontal()) {
			SignalMount mount = SignalMount.fromSupportDirection(facing, clickedFace.getOpposite());
			if (mount != SignalMount.NONE)
				state = attach(state, context.getLevel(), context.getClickedPos(), mount);
		}
		return state;
	}

	/**
	 * Montage und Neigung zur gewünschten Seite: hinten ein Signalmast ergibt eine Mast-Montage mit
	 * dessen Neigung, ein fester Block {@link SignalMount#BACK}; sonst {@link SignalMount#NONE}.
	 *
	 * @param pos Position der unteren Hälfte
	 */
	private BlockState attach(BlockState state, LevelReader level, BlockPos pos, SignalMount wanted) {
		Direction facing = state.getValue(FACING);
		if (wanted.isFlush()) {
			BlockState mast = mastBehind(level, pos, facing);
			if (mast != null) {
				SignalMastBlock.Attachment attachment = ((SignalMastBlock) mast.getBlock()).attachmentFor(mast, facing);
				return state.setValue(MOUNT, attachment.mount())
					.setValue(TILT, attachment.tilt());
			}
			wanted = SignalMount.BACK;
		}
		return state.setValue(MOUNT, canAttach(level, pos, facing, wanted) ? wanted : SignalMount.NONE)
			.setValue(TILT, 0);
	}

	/**
	 * Neigung in 22,5°-Stufen im Uhrzeigersinn, gefaltet auf [−45°, 45°): der Kopf dreht zur
	 * nächstgelegenen Seite des Masts (0, +1, −2, −1).
	 */
	public static int tiltSteps(int tilt) {
		return tilt >= 2 ? tilt - 4 : tilt;
	}

	/**
	 * Drehung des Kopfs um die Mastmitte in Grad im Uhrzeigersinn (von oben gesehen, Modellraum).
	 */
	public static double tiltDegrees(BlockState state) {
		return state.getValue(MOUNT)
			.isMast() ? tiltSteps(state.getValue(TILT)) * 22.5 : 0;
	}

	/**
	 * Signalmast hinter dem Signal, oder null. Hohe Signale brauchen hinter jedem Teil einen
	 * Mastblock; Bauform und Ausrichtung gibt der unterste vor.
	 */
	@Nullable
	private BlockState mastBehind(LevelReader level, BlockPos pos, Direction facing) {
		BlockPos behind = pos.relative(facing.getOpposite());
		BlockState mast = level.getBlockState(behind);
		if (!(mast.getBlock() instanceof SignalMastBlock))
			return null;
		for (int part = 1; part < getHeight(); part++)
			if (!(level.getBlockState(behind.above(part))
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
		for (int part = 1; part < getHeight(); part++)
			level.setBlock(pos.above(part), withPart(state, part), 3);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		// Im Kreativmodus soll das Abbauen eines oberen Teils den untersten nicht droppen lassen
		if (!level.isClientSide && player.isCreative() && partIndex(state) > 0) {
			BlockPos main = getMainPos(state, pos);
			BlockState lower = level.getBlockState(main);
			if (lower.is(this) && partIndex(lower) == 0) {
				level.setBlock(main, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				level.levelEvent(player, 2001, main, Block.getId(lower));
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
		LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		int part = partIndex(state);
		boolean below = direction == Direction.DOWN && part > 0;
		boolean above = direction == Direction.UP && part < getHeight() - 1;
		if (below || above) {
			// benachbarter Teil entfernt: dieser auch
			if (!neighborState.is(this) || partIndex(neighborState) != part + (above ? 1 : -1))
				return Blocks.AIR.defaultBlockState();
			// ein Teil übernimmt Begriff, Ausrichtung und Montage des Teils darunter; Montage und
			// Neigung auch von oben, falls dort der Mastblock wegfällt
			return below ? withPart(neighborState, part)
				: state.setValue(MOUNT, neighborState.getValue(MOUNT))
					.setValue(TILT, neighborState.getValue(TILT));
		}

		// Stützblock geändert (auch gedrehter Mast): Wand, Mast oder freie Aufstellung neu bestimmen
		SignalMount mount = state.getValue(MOUNT);
		if (direction == mount.getSupportDirection(state.getValue(FACING)))
			return attach(state, level, getMainPos(state, pos), mount);
		return state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		SignalMount mount = state.getValue(MOUNT);
		return SHAPES.get(state.getValue(FACING))
			.get(mount)[mount.isMast() ? state.getValue(TILT) : 0];
	}

	private static Map<Direction, Map<SignalMount, VoxelShape[]>> buildShapes() {
		Map<Direction, Map<SignalMount, VoxelShape[]>> shapes = new EnumMap<>(Direction.class);
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			Map<SignalMount, VoxelShape[]> byMount = new EnumMap<>(SignalMount.class);
			VoxelShape screen = rotatedBox(facing, 3, 0, 3.5, 13, 16, 9.5);
			byMount.put(SignalMount.NONE, new VoxelShape[] { screen });
			// hinten montiert: Kopf (z 2,5..6,5) um den Versatz nach hinten an die Blockkante; am gedrehten
			// Mast um dessen Mitte gedreht, als achsparalleles Rechteck um den gedrehten Kopf
			for (SignalMount mount : SignalMount.values()) {
				if (!mount.isFlush())
					continue;
				VoxelShape[] byTilt = new VoxelShape[mount.isMast() ? 4 : 1];
				for (int tilt = 0; tilt < byTilt.length; tilt++) {
					double[] b = tiltedBounds(3, 2.5 + mount.getFlushShift(), 13, 6.5 + mount.getFlushShift(),
						tiltSteps(tilt) * 22.5);
					byTilt[tilt] = rotatedBox(facing, b[0], 0, b[1], b[2], 16, b[3]);
				}
				byMount.put(mount, byTilt);
			}
			byMount.put(SignalMount.LEFT,
				new VoxelShape[] { Shapes.or(screen, rotatedBox(facing, 9.5, 1, 6.5, 16, 15, 9.5)) });
			byMount.put(SignalMount.RIGHT,
				new VoxelShape[] { Shapes.or(screen, rotatedBox(facing, 0, 1, 6.5, 6.5, 15, 9.5)) });
			shapes.put(facing, byMount);
		}
		return shapes;
	}

	/**
	 * Rechteck x0..x1, z0..z1 (Modellraum) um die Mastmitte gedreht; Ergebnis als achsparalleles
	 * Rechteck {x0, z0, x1, z1}.
	 */
	private static double[] tiltedBounds(double x0, double z0, double x1, double z1, double degrees) {
		double minX = Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
		for (double x : new double[] { x0, x1 })
			for (double z : new double[] { z0, z1 }) {
				double[] p = tilt(x, z, degrees);
				minX = Math.min(minX, p[0]);
				maxX = Math.max(maxX, p[0]);
				minZ = Math.min(minZ, p[1]);
				maxZ = Math.max(maxZ, p[1]);
			}
		return new double[] { minX, minZ, maxX, maxZ };
	}

	/**
	 * Punkt im Modellraum um die Mastmitte drehen, im Uhrzeigersinn von oben gesehen (Nord -> Ost).
	 */
	private static double[] tilt(double x, double z, double degrees) {
		double angle = Math.toRadians(degrees), cos = Math.cos(angle), sin = Math.sin(angle);
		double dx = x - PIVOT_X, dz = z - PIVOT_Z;
		return new double[] { PIVOT_X + dx * cos - dz * sin, PIVOT_Z + dx * sin + dz * cos };
	}

	/**
	 * Punkt für Bedienfelder an der Seite des Schirms (Modellraum, Pixel), gedreht in Blockkoordinaten.
	 * {@code plusX}: Seite bei x = 13 (aus Sicht des Betrachters vor dem Schirm links), sonst x = 3.
	 * Die Tiefe folgt der Montage: Schirmmitte mittig im Block oder an der hinteren Blockkante.
	 */
	public static Vec3 sideSlot(BlockState state, boolean plusX, double y) {
		// Mitte des Schirms (z 4..6,5), mit dem Kopf nach hinten versetzt und am Mast mitgedreht
		double[] p = tilt(plusX ? 13.25 : 2.75, 5.25 + state.getValue(MOUNT)
			.getFlushShift(), tiltDegrees(state));
		double x = p[0], z = p[1];
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
		// Normale der Schirmseite (±x im Modellraum), am Mast mitgedreht, auf die nächste Weltrichtung
		double angle = Math.toRadians(tiltDegrees(state));
		double sign = plusX ? 1 : -1;
		double nx = sign * Math.cos(angle), nz = sign * Math.sin(angle);
		double wx = switch (state.getValue(FACING)) {
			case EAST -> -nz;
			case SOUTH -> -nx;
			case WEST -> nz;
			default -> nx;
		};
		double wz = switch (state.getValue(FACING)) {
			case EAST -> nx;
			case SOUTH -> -nz;
			case WEST -> -nx;
			default -> nz;
		};
		return Direction.getNearest(wx, 0, wz);
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
		BlockPos main = getMainPos(newState, context.getClickedPos());
		for (SignalMount wanted : new SignalMount[] { SignalMount.BACK, SignalMount.LEFT, SignalMount.RIGHT }) {
			BlockState attached = attach(newState, context.getLevel(), main, wanted);
			if (attached.getValue(MOUNT) != SignalMount.NONE)
				return attached;
		}
		return newState.setValue(MOUNT, SignalMount.NONE)
			.setValue(TILT, 0);
	}

	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		// Aufnehmen mit dem Schraubenschlüssel immer über den untersten Teil, der das Item droppt
		if (partIndex(state) > 0 && context.getPlayer() != null) {
			BlockPos main = getMainPos(state, context.getClickedPos());
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
		// der unterste Teil trägt Bindung und Logik, der oberste die eingebaute Funkverbindung
		if (isUpper(state))
			return ModBlockEntities.SIGNAL_LINK.get()
				.create(pos, state);
		return partIndex(state) == 0 ? IBE.super.newBlockEntity(pos, state) : null;
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
