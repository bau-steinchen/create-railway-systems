package de.bausteinchen.railwaysystems.traffic;

import de.bausteinchen.railwaysystems.registry.ModBlockEntities;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Zebrastreifen-Mast: Peitschenmast mit einer Leuchte über dem Fußgängerüberweg, die beidseitig das
 * Zeichen 350 trägt, und einem flachen Zeichen 350 am Mast (Bauart wie in Hamburg). Die Leuchte schaltet
 * eine eingebaute Funkverbindung (Create Redstone Link, Empfänger) im Fußblock; {@link #LIT} ist ihr Zustand.
 */
public class CrosswalkMastBlock extends AbstractMastBlock {

	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public CrosswalkMastBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(LIT));
	}

	@Override
	public MastConfig defaultConfig() {
		return MastConfig.DEFAULT_CROSSWALK;
	}

	@Override
	public boolean isCrosswalk() {
		return true;
	}

	@Override
	public BlockEntityType<? extends TrafficLightBlockEntity> getBlockEntityType() {
		return ModBlockEntities.CROSSWALK_MAST.get();
	}

}
