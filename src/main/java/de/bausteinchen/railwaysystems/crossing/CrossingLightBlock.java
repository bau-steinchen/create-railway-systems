package de.bausteinchen.railwaysystems.crossing;

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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Lichtsignal am Bahnübergang. Zwei Bauformen:
 * <ul>
 * <li>Lichtzeichen (DB): Gelb über Rot; beim Einschalten erst Gelb, dann Dauerrot</li>
 * <li>Blinklicht: ein rotes Licht, das blinkt (animierte Textur)</li>
 * </ul>
 * Geschaltet wird über Redstone am Block oder die eingebaute Funkverbindung (Rückseite). Der Mast läuft
 * durch den ganzen Block, damit ein Andreaskreuz oder ein Signalmast darauf passt.
 */
public class CrossingLightBlock extends Block implements IBE<CrossingLightBlockEntity>, IWrenchable {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<CrossingPhase> PHASE = EnumProperty.create("phase", CrossingPhase.class);
	public static final EnumProperty<CrossingPhase> BLINKER_PHASE =
		EnumProperty.create("phase", CrossingPhase.class, CrossingPhase.OFF, CrossingPhase.RED);

	private static final VoxelShape POST = Block.box(6.5, 0, 6.5, 9.5, 16, 9.5);
	private static final VoxelShape[] SHAPES = {
		// Süd, West, Nord, Ost (2D-Datenwert): Blende mit Lampen vor dem Mast
		Shapes.or(POST, Block.box(3, 1, 9.5, 13, 15, 12.5)),
		Shapes.or(POST, Block.box(3.5, 1, 3, 6.5, 15, 13)),
		Shapes.or(POST, Block.box(3, 1, 3.5, 13, 15, 6.5)),
		Shapes.or(POST, Block.box(9.5, 1, 3, 12.5, 15, 13)) };

	public CrossingLightBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(phaseProperty(), CrossingPhase.OFF));
	}

	/**
	 * true: Blinklicht (nur Rot), false: Lichtzeichen (Gelb, dann Rot). Eine überschriebene Methode statt
	 * eines Feldes, weil createBlockStateDefinition schon im Konstruktor von Block läuft.
	 */
	public boolean isBlinker() {
		return false;
	}

	public EnumProperty<CrossingPhase> phaseProperty() {
		return isBlinker() ? BLINKER_PHASE : PHASE;
	}

	public CrossingPhase getPhase(BlockState state) {
		return state.getValue(phaseProperty());
	}

	public BlockState withPhase(BlockState state, CrossingPhase phase) {
		if (isBlinker() && phase == CrossingPhase.YELLOW)
			phase = CrossingPhase.RED;
		return state.setValue(phaseProperty(), phase);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, phaseProperty()));
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

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		IBE.onRemove(state, level, pos, newState);
	}

	@Override
	public Class<CrossingLightBlockEntity> getBlockEntityClass() {
		return CrossingLightBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CrossingLightBlockEntity> getBlockEntityType() {
		return ModBlockEntities.CROSSING_LIGHT.get();
	}

	/** Blinklicht: eigene Klasse, weil die Blockzustände schon im Konstruktor von Block feststehen */
	public static class Blinker extends CrossingLightBlock {

		public Blinker(Properties properties) {
			super(properties);
		}

		@Override
		public boolean isBlinker() {
			return true;
		}

	}

}
