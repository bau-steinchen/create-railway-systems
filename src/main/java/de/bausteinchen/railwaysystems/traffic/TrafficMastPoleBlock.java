package de.bausteinchen.railwaysystems.traffic;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import de.bausteinchen.railwaysystems.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Teil des Mastschafts über einem {@link TrafficMastBlock}. Hat kein eigenes Item; Abbauen, Schraubenschlüssel
 * und Pick-Block wirken auf den ganzen Mast.
 */
public class TrafficMastPoleBlock extends Block implements IWrenchable, TrafficLight {

	private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

	public TrafficMastPoleBlock(Properties properties) {
		super(properties);
	}

	/**
	 * Fußblock unter einem Schaftteil, oder die Position selbst, wenn keiner gefunden wird.
	 */
	public static BlockPos findBase(BlockGetter level, BlockPos pos) {
		BlockPos p = pos;
		while (level.getBlockState(p)
			.is(ModBlocks.TRAFFIC_MAST_POLE.get()))
			p = p.below();
		return level.getBlockState(p)
			.is(ModBlocks.TRAFFIC_MAST.get()) ? p : pos;
	}

	@Override
	public BlockPos getMainPos(Level level, BlockPos pos, BlockState state) {
		return findBase(level, pos);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
		LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (direction == Direction.DOWN && !neighborState.is(this) && !neighborState.is(ModBlocks.TRAFFIC_MAST.get()))
			return Blocks.AIR.defaultBlockState();
		return state;
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		// ganzen Mast abbauen; der Fuß droppt das Item
		BlockPos base = findBase(level, pos);
		if (!level.isClientSide && !base.equals(pos))
			level.destroyBlock(base, !player.isCreative(), player);
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
		Player player) {
		return new ItemStack(ModBlocks.TRAFFIC_MAST.get());
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		return redirect(context, false);
	}

	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		return redirect(context, true);
	}

	private InteractionResult redirect(UseOnContext context, boolean sneak) {
		Level level = context.getLevel();
		BlockPos base = findBase(level, context.getClickedPos());
		BlockState baseState = level.getBlockState(base);
		if (!(baseState.getBlock() instanceof TrafficMastBlock mast) || context.getPlayer() == null)
			return InteractionResult.PASS;
		UseOnContext baseContext = new UseOnContext(context.getPlayer(), context.getHand(),
			new BlockHitResult(context.getClickLocation(), context.getClickedFace(), base, context.isInside()));
		return sneak ? mast.onSneakWrenched(baseState, baseContext) : mast.onWrenched(baseState, baseContext);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public boolean isPedestrian() {
		return false;
	}

	@Override
	public TrafficAspect getAspect(BlockState state) {
		return TrafficAspect.OFF;
	}

	@Override
	public BlockState withAspect(BlockState state, TrafficAspect aspect) {
		return state;
	}

}
