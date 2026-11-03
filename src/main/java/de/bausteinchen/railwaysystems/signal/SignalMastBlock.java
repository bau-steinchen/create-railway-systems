package de.bausteinchen.railwaysystems.signal;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Signalmast, vor dem Signale bündig mit Halterstangen sitzen oder auf den sie gesetzt werden.
 * Es gibt drei Bauformen, siehe {@link Type}. {@link #AXIS} dreht den Mast: beim Platzieren in
 * Blickrichtung, danach mit dem Schraubenschlüssel.
 */
public class SignalMastBlock extends Block implements IWrenchable {

	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

	public enum Type {
		/** Standardmast: zwei Platten mit Sprossen dazwischen, Platten quer zur Achse. */
		STANDARD(Block.box(6.5, 0, 5.5, 9.5, 16, 10.5), Block.box(5.5, 0, 6.5, 10.5, 16, 9.5)),
		/** Leichter Mast: ein einzelner Stab. */
		LIGHT(Block.box(6.5, 0, 6.5, 9.5, 16, 9.5), Block.box(6.5, 0, 6.5, 9.5, 16, 9.5)),
		/** Gittermast: breit und quadratisch, Eckstiele mit Streben dazwischen. */
		LATTICE(Block.box(3, 0, 3, 13, 16, 13), Block.box(3, 0, 3, 13, 16, 13));

		private final VoxelShape shapeZ;
		private final VoxelShape shapeX;

		Type(VoxelShape shapeZ, VoxelShape shapeX) {
			this.shapeZ = shapeZ;
			this.shapeX = shapeX;
		}
	}

	private final Type type;

	public SignalMastBlock(Type type, Properties properties) {
		super(properties);
		this.type = type;
		registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Z));
	}

	public Type getType() {
		return type;
	}

	/**
	 * Montage eines Signals, das zur Seite {@code towardSignal} vor diesem Mast sitzt. Sie bestimmt,
	 * wie weit die Halterstangen in den Mastblock reichen.
	 */
	public SignalMount mountFor(BlockState state, Direction towardSignal) {
		return switch (type) {
			case LATTICE -> SignalMount.MAST_LATTICE;
			// Standardmast: zeigt die Platte zum Signal, liegt sie 1 px näher als die Kante
			case STANDARD -> towardSignal.getAxis() == state.getValue(AXIS) ? SignalMount.MAST_PLATE
				: SignalMount.MAST;
			case LIGHT -> SignalMount.MAST;
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(AXIS));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(AXIS, context.getHorizontalDirection()
			.getAxis());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(AXIS) == Direction.Axis.X ? type.shapeX : type.shapeZ;
	}

	@Override
	public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
		return rotate(originalState, Rotation.CLOCKWISE_90);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90)
			return state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
		return state;
	}

}
