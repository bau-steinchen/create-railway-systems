package de.bausteinchen.railwaysystems.crossing;

import de.bausteinchen.railwaysystems.signal.SignalMastBlock;
import de.bausteinchen.railwaysystems.signal.SignalMount;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Montage von Lichtzeichen, Blinklicht und Andreaskreuz an einem Signalmast: wie ein Lichtsignal bündig
 * vor dem Mast, ohne eigenen Pfosten, mit Halterstangen bis an die Mastoberfläche. Nur an ungedrehten
 * Masten (Maststufe Vielfaches von 4); sonst frei aufgestellt.
 */
public final class CrossingMount {

	public static final EnumProperty<SignalMount> MOUNT = EnumProperty.create("mount", SignalMount.class,
		SignalMount.NONE, SignalMount.MAST, SignalMount.MAST_PLATE, SignalMount.MAST_LATTICE);

	private CrossingMount() {}

	/** Gegen die Seite eines Signalmasts gesetzt: zeigt vom Mast weg und hängt daran */
	public static BlockState forPlacement(BlockState state, BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		if (face.getAxis()
			.isHorizontal() && mastAt(context.getLevel(), context.getClickedPos()
				.relative(face.getOpposite())) != null)
			state = state.setValue(HorizontalDirectionalBlock.FACING, face);
		return state.setValue(MOUNT, mountFor(state, context.getLevel(), context.getClickedPos()));
	}

	/**
	 * Mast hinter dem Block geändert (abgebaut, gedreht): Montage neu bestimmen; ohne Mast frei aufgestellt
	 * wie ein Lichtsignal in derselben Lage
	 */
	public static BlockState update(BlockState state, Direction direction, LevelReader level, BlockPos pos) {
		if (state.getValue(MOUNT) == SignalMount.NONE || direction != state.getValue(HorizontalDirectionalBlock.FACING)
			.getOpposite())
			return state;
		return state.setValue(MOUNT, mountFor(state, level, pos));
	}

	/** Montage an einem ungedrehten Signalmast hinter dem Block, sonst {@link SignalMount#NONE} */
	public static SignalMount mountFor(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
		BlockState mast = mastAt(level, pos.relative(facing.getOpposite()));
		return mast == null ? SignalMount.NONE : ((SignalMastBlock) mast.getBlock()).attachmentFor(mast, facing)
			.mount();
	}

	private static BlockState mastAt(LevelReader level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.getBlock() instanceof SignalMastBlock && state.getValue(SignalMastBlock.ROTATION) % 4 == 0 ? state
			: null;
	}

	/**
	 * Box im Modellraum (Front nach Norden) als Formen je Ausrichtung, Index = 2D-Datenwert (Süd, West,
	 * Nord, Ost).
	 */
	public static VoxelShape[] rotatedBoxes(double x0, double y0, double z0, double x1, double y1, double z1) {
		VoxelShape[] shapes = new VoxelShape[4];
		for (Direction facing : Direction.Plane.HORIZONTAL)
			shapes[facing.get2DDataValue()] = switch (facing) {
				case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
				case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
				case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
				default -> Block.box(x0, y0, z0, x1, y1, z1);
			};
		return shapes;
	}

}
