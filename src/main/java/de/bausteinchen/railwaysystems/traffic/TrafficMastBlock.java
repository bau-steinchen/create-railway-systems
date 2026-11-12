package de.bausteinchen.railwaysystems.traffic;

import de.bausteinchen.railwaysystems.registry.ModBlockEntities;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Ampelmast mit Ausleger (Peitschenmast): Kfz-Signalgeber am Auslegerende und auf Wunsch unten am Mast.
 */
public class TrafficMastBlock extends AbstractMastBlock implements TrafficLight {

	public static final EnumProperty<TrafficAspect> ASPECT = EnumProperty.create("aspect", TrafficAspect.class);

	public TrafficMastBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(ASPECT, TrafficAspect.OFF));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(ASPECT));
	}

	@Override
	public MastConfig defaultConfig() {
		return MastConfig.DEFAULT;
	}

	@Override
	public boolean isCrosswalk() {
		return false;
	}

	@Override
	public boolean isPedestrian() {
		return false;
	}

	@Override
	public TrafficAspect getAspect(BlockState state) {
		return state.getValue(ASPECT);
	}

	@Override
	public BlockState withAspect(BlockState state, TrafficAspect aspect) {
		return state.setValue(ASPECT, aspect);
	}

	@Override
	public BlockEntityType<? extends TrafficLightBlockEntity> getBlockEntityType() {
		return ModBlockEntities.TRAFFIC_LIGHT.get();
	}

}
