package de.bausteinchen.railwaysystems.signal.hv;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import de.bausteinchen.railwaysystems.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Gemeinsame Basis der H/V-Lichtsignale. Der Signalschirm zeigt in Richtung {@link #FACING}
 * (zum anfahrenden Zug); der Begriff wird von {@link HvSignalBlockEntity} aus dem gebundenen
 * Create-Signal abgeleitet.
 */
public abstract class HvSignalBlock extends Block implements IBE<HvSignalBlockEntity>, IWrenchable {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<HpAspect> MAIN_ASPECT = EnumProperty.create("aspect", HpAspect.class);
	public static final EnumProperty<VrAspect> DISTANT_ASPECT = EnumProperty.create("distant", VrAspect.class);

	private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);

	protected HvSignalBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection()
			.getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
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

	/**
	 * Ob dieses Signal einen Hauptsignalbegriff (Hp) zeigt.
	 */
	public boolean hasMainAspect() {
		return getStateDefinition().getProperties()
			.contains(MAIN_ASPECT);
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if (!hasMainAspect())
			return IWrenchable.super.onWrenched(state, context);

		// Create kennt keine Geschwindigkeitsbegriffe: Hp2 wird per Schraubenschlüssel vorgewählt
		Level level = context.getLevel();
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		return onBlockEntityUse(level, context.getClickedPos(), be -> {
			boolean slow = be.toggleSlowMode();
			if (context.getPlayer() != null)
				context.getPlayer()
					.displayClientMessage(Component.translatable(slow ? "create_railway_systems.hv_signal.mode_slow"
						: "create_railway_systems.hv_signal.mode_normal"), true);
			IWrenchable.playRotateSound(level, context.getClickedPos());
			return InteractionResult.SUCCESS;
		});
	}

	@Override
	public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
		return originalState.setValue(FACING, originalState.getValue(FACING)
			.getClockWise());
	}

	@Override
	public Class<HvSignalBlockEntity> getBlockEntityClass() {
		return HvSignalBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends HvSignalBlockEntity> getBlockEntityType() {
		return ModBlockEntities.HV_SIGNAL.get();
	}

}
