package de.bausteinchen.railwaysystems.signal.hv;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import de.bausteinchen.railwaysystems.signal.LinkedSignalBlockEntity;
import de.bausteinchen.railwaysystems.signal.SignalFeature;
import de.bausteinchen.railwaysystems.signal.SignalInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Formhauptsignal (H/V), drei Blöcke hoch: einflügelig Hp0/Hp1, zweiflügelig Hp0/Hp1/Hp2. Die
 * Flügel schwenken animiert (gezeichnet vom Overlay-Renderer, siehe {@link #armAngle}); die
 * Laternen für die Nachtzeichen sind Teil des Blockmodells.
 */
public class FormHauptsignalBlock extends HvSignalBlock {

	private final int arms;

	public FormHauptsignalBlock(int arms, Properties properties) {
		super(properties);
		this.arms = arms;
		registerDefaultState(defaultBlockState().setValue(MAIN_ASPECT, HpAspect.HP0));
	}

	public int getArms() {
		return arms;
	}

	@Override
	protected int getHeight() {
		return 3;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(MAIN_ASPECT));
	}

	@Override
	public BlockState updateAspect(BlockState state, SignalInput input) {
		HpAspect aspect = mainAspect(input);
		// kein Kennlicht; ein Flügel kennt kein Hp2
		if (aspect == HpAspect.OFF)
			aspect = HpAspect.HP0;
		if (arms == 1 && aspect == HpAspect.HP2)
			aspect = HpAspect.HP1;
		return state.setValue(MAIN_ASPECT, aspect);
	}

	/**
	 * Stellung eines Flügels in Grad um die Achse quer zum Gleis, 0 = waagerecht nach rechts (vom Zug
	 * aus gesehen), negativ = nach oben. Oberflügel (0): Hp0 waagerecht, Fahrt 45° schräg nach oben.
	 * Unterflügel (1): hängt bei Hp0 senkrecht am Mast, bei Fahrt (Hp1 und Hp2) ebenfalls 45° nach oben;
	 * Hp1 und Hp2 unterscheidet dann nur die Laterne.
	 */
	public static float armAngle(BlockState state, int arm) {
		HpAspect aspect = state.hasProperty(MAIN_ASPECT) ? state.getValue(MAIN_ASPECT) : HpAspect.HP0;
		boolean proceed = aspect == HpAspect.HP1 || aspect == HpAspect.HP2;
		if (arm == 0)
			return proceed ? -45 : 0;
		return proceed ? -45 : 90;
	}

	@Override
	public Set<SignalFeature> getFeatures() {
		// Zusatzanzeiger kommen später
		return Set.of();
	}

	@Override
	public int getSpeedLimit(BlockState state, LinkedSignalBlockEntity blockEntity) {
		return arms == 1 ? 0 : super.getSpeedLimit(state, blockEntity);
	}

	@Override
	@Nullable
	public String getWrenchTooltipKey() {
		return arms == 1 ? null : super.getWrenchTooltipKey();
	}

	@Override
	protected boolean wrenchSelectsHp2() {
		// Hp2 gibt es nur mit zwei Flügeln; sonst dreht der Schraubenschlüssel wie bei den Vorsignalen
		return arms == 2;
	}

}
