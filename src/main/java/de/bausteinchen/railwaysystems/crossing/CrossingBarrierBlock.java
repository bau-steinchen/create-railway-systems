package de.bausteinchen.railwaysystems.crossing;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import de.bausteinchen.railwaysystems.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Schrankenantrieb am Bahnübergang. Der Baum wird nur gezeichnet ({@code CrossingBarrierRenderer}) und
 * schwenkt zwischen senkrecht (offen) und waagerecht (geschlossen); seine Länge ist ein Wertefeld oben
 * auf dem Antrieb. {@link #CLOSED} ist der Sollzustand, den die Block-Entity aus Redstone bzw.
 * Funkverbindung ableitet.
 * <p>
 * Schraubenschlüssel auf die Vorderseite: Baum auf die andere Seite; auf jede andere Seite: drehen.
 */
public class CrossingBarrierBlock extends Block implements IBE<CrossingBarrierBlockEntity>, IWrenchable {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<BarrierSide> SIDE = EnumProperty.create("side", BarrierSide.class);
	public static final BooleanProperty CLOSED = BooleanProperty.create("closed");

	/** Drehpunkt des Baums im Modell mit Front nach Norden (Pixel) */
	public static final double PIVOT_Y = 10, PIVOT_Z = 3.25;

	private static final VoxelShape[] SHAPES = {
		// Süd, West, Nord, Ost (2D-Datenwert): Gehäuse mit Lager vorn
		Block.box(3, 0, 3, 13, 14, 14), Block.box(2, 0, 3, 13, 14, 13),
		Block.box(3, 0, 2, 13, 14, 13), Block.box(3, 0, 3, 14, 14, 13) };

	public CrossingBarrierBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(SIDE, BarrierSide.LEFT)
			.setValue(CLOSED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, SIDE, CLOSED));
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
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if (context.getClickedFace() != state.getValue(FACING))
			return IWrenchable.super.onWrenched(state, context);
		Level level = context.getLevel();
		if (!level.isClientSide) {
			level.setBlock(context.getClickedPos(), state.setValue(SIDE, state.getValue(SIDE)
				.opposite()), 3);
			IWrenchable.playRotateSound(level, context.getClickedPos());
		}
		return InteractionResult.SUCCESS;
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
		BlockState rotated = state.rotate(mirror.getRotation(state.getValue(FACING)));
		return mirror == Mirror.NONE ? rotated : rotated.setValue(SIDE, state.getValue(SIDE)
			.opposite());
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		IBE.onRemove(state, level, pos, newState);
	}

	@Override
	public Class<CrossingBarrierBlockEntity> getBlockEntityClass() {
		return CrossingBarrierBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CrossingBarrierBlockEntity> getBlockEntityType() {
		return ModBlockEntities.CROSSING_BARRIER.get();
	}

}
