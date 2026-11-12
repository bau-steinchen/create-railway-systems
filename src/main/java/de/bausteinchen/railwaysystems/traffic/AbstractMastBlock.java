package de.bausteinchen.railwaysystems.traffic;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import de.bausteinchen.railwaysystems.client.TrafficClientHooks;
import de.bausteinchen.railwaysystems.registry.ModBlocks;
import de.bausteinchen.railwaysystems.registry.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Fußblock eines Peitschenmastes (Ampelmast, Zebrastreifen-Mast). Darüber stehen
 * {@link TrafficMastPoleBlock}e bis zur eingestellten Höhe; Bogen, Ausleger und Anbauteile zeichnet der
 * TrafficMastRenderer ohne Kollision. Anbauteile zeigen in Richtung {@link #FACING}, zum ankommenden
 * Verkehr.
 */
public abstract class AbstractMastBlock extends Block implements IBE<TrafficLightBlockEntity>, IWrenchable {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	/** Radius des Bogens zwischen Mast und Ausleger, in Blöcken */
	public static final double BEND_RADIUS = 1.5;

	private static final VoxelShape SHAPE = Shapes.or(Block.box(4.5, 0, 4.5, 11.5, 1, 11.5), Block.box(6, 0, 6, 10, 16, 10));

	protected AbstractMastBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING));
	}

	/** Bauform eines neuen Items ohne gespeicherte Einstellung */
	public abstract MastConfig defaultConfig();

	/** Zebrastreifen-Mast (Leuchte und Schild) statt Ampelmast; bestimmt das Menü */
	public abstract boolean isCrosswalk();

	public MastConfig getConfig(ItemStack stack) {
		MastConfig config = stack.get(ModDataComponents.MAST_CONFIG.get());
		return config == null ? defaultConfig() : config.clamped();
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		MastConfig config = getConfig(context.getItemInHand());
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		for (int i = 1; i < config.height(); i++) {
			BlockPos above = pos.above(i);
			if (above.getY() >= level.getMaxBuildHeight() || !level.getBlockState(above)
				.canBeReplaced(context))
				return null;
		}
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection()
			.getOpposite());
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		MastConfig config = getConfig(stack);
		placePole(level, pos, config.height());
		withBlockEntityDo(level, pos, be -> be.setConfig(config));
	}

	/**
	 * Mastschaft auf {@code height} Blöcke (mit Fuß) bringen: fehlende Teile setzen, überzählige entfernen.
	 *
	 * @return ob der Platz gereicht hat
	 */
	public static boolean placePole(Level level, BlockPos base, int height) {
		for (int i = 1; i < height; i++) {
			BlockPos above = base.above(i);
			BlockState existing = level.getBlockState(above);
			if (!existing.is(ModBlocks.TRAFFIC_MAST_POLE.get()) && (!existing.canBeReplaced()
				|| above.getY() >= level.getMaxBuildHeight()))
				return false;
		}
		for (int i = 1; i < height; i++)
			if (!level.getBlockState(base.above(i))
				.is(ModBlocks.TRAFFIC_MAST_POLE.get()))
				level.setBlock(base.above(i), ModBlocks.TRAFFIC_MAST_POLE.get()
					.defaultBlockState(), 3);
		// höher als nötig: oberste Teile entfernen
		BlockPos top = base.above(height);
		while (level.getBlockState(top)
			.is(ModBlocks.TRAFFIC_MAST_POLE.get())) {
			top = top.above();
		}
		for (BlockPos p = top.below(); p.getY() >= base.getY() + height; p = p.below())
			level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
		return true;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
		LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		// Mastschaft entfernt (z. B. durch eine Explosion): ganzer Mast fällt
		if (direction == Direction.UP && !neighborState.is(ModBlocks.TRAFFIC_MAST_POLE.get()))
			return Blocks.AIR.defaultBlockState();
		return state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		// Schraubenschlüssel öffnet das Menü für die Bauform
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (level.isClientSide && FMLEnvironment.dist == Dist.CLIENT && level.getBlockEntity(pos) instanceof TrafficLightBlockEntity be)
			TrafficClientHooks.openMastScreen(pos, be.getConfig(), isCrosswalk());
		return InteractionResult.SUCCESS;
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

}
