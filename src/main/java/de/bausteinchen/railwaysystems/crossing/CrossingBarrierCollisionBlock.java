package de.bausteinchen.railwaysystems.crossing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Unsichtbare Kollision unter dem geschlossenen Schrankenbaum. Der {@link CrossingBarrierBlockEntity Antrieb}
 * setzt sie in die Luftblöcke entlang des Baums und entfernt sie beim Öffnen wieder. Ein schmaler Balken,
 * 1,5 Blöcke hoch wie ein Zaun, damit niemand darüber springt. Nicht anklickbar, kein Item, kein Loot.
 * <p>
 * {@link #FACING} ist die Front des Antriebs; danach liegt der Balken in Baumlinie. Verwaiste Blöcke
 * (Antrieb abgebaut, Länge oder Seite geändert) entfernen sich über einen regelmäßigen Tick selbst.
 */
public class CrossingBarrierCollisionBlock extends Block {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	private static final int CHECK_TICKS = 10;

	private static final VoxelShape[] SHAPES = {
		// Süd, West, Nord, Ost (2D-Datenwert); Baum bei z = 3,25 px mit Front nach Norden
		Block.box(0, 0, 11.5, 16, 24, 14), Block.box(2, 0, 0, 4.5, 24, 16),
		Block.box(0, 0, 2, 16, 24, 4.5), Block.box(11.5, 0, 0, 14, 24, 16) };

	public CrossingBarrierCollisionBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING));
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
		CollisionContext context) {
		return SHAPES[state.getValue(FACING)
			.get2DDataValue()];
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		level.scheduleTick(pos, this, CHECK_TICKS);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (isHeld(level, pos))
			level.scheduleTick(pos, this, CHECK_TICKS);
		else
			level.removeBlock(pos, false);
	}

	/** Gehört zu einem geschlossenen Antrieb in einer Linie mit diesem Block */
	private static boolean isHeld(Level level, BlockPos pos) {
		for (Direction direction : Direction.Plane.HORIZONTAL)
			for (int i = 1; i <= CrossingBarrierBlockEntity.MAX_LENGTH; i++) {
				BlockPos barrier = pos.relative(direction, i);
				if (!level.isLoaded(barrier))
					// Antrieb könnte im nicht geladenen Bereich liegen: lieber stehen lassen
					return true;
				if (level.getBlockEntity(barrier) instanceof CrossingBarrierBlockEntity be && be.holdsCollisionAt(pos))
					return true;
			}
		return false;
	}

}
