package de.bausteinchen.railwaysystems.signal.ks;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import de.bausteinchen.railwaysystems.signal.LinkedSignalBlockEntity;
import de.bausteinchen.railwaysystems.signal.MountedSignalBlock;
import de.bausteinchen.railwaysystems.signal.SignalInput.NextSignal;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Gemeinsame Basis der Ks-Signale. Hauptsignale tragen einen Geschwindigkeitsanzeiger Zs3
 * (per Schraubenschlüssel einstellbar), Vorsignale einen Geschwindigkeitsvoranzeiger Zs3v,
 * der die Geschwindigkeit des nächsten Hauptsignals ankündigt.
 */
public abstract class KsSignalBlock extends MountedSignalBlock {

	/** Am Signal vorgegebene Geschwindigkeit in 10 km/h, 0 = kein Zs3 */
	public static final IntegerProperty ZS3 = IntegerProperty.create("zs3", 0, 9);
	/** Angekündigte Geschwindigkeit in 10 km/h, 0 = Zs3v dunkel */
	public static final IntegerProperty ZS3V = IntegerProperty.create("zs3v", 0, 9);

	protected KsSignalBlock(Properties properties) {
		super(properties);
	}

	public boolean hasSpeedIndicator() {
		return getStateDefinition().getProperties()
			.contains(ZS3);
	}

	/**
	 * Vorsignalbegriff: Ks2 bei Halt, Ks1 blinkend mit Zs3v bei angekündigter Geschwindigkeit.
	 */
	protected static BlockState withDistantAspect(BlockState state, NextSignal next, EnumProperty<KsAspect> aspect) {
		if (!next.clear())
			return state.setValue(aspect, KsAspect.KS2)
				.setValue(ZS3V, 0);
		if (next.speedLimit() > 0)
			return state.setValue(aspect, KsAspect.KS1_BLINK)
				.setValue(ZS3V, next.speedLimit());
		return state.setValue(aspect, KsAspect.KS1)
			.setValue(ZS3V, 0);
	}

	@Override
	public int getSpeedLimit(BlockState state, LinkedSignalBlockEntity blockEntity) {
		return hasSpeedIndicator() ? state.getValue(ZS3) : -1;
	}

	@Override
	@Nullable
	public String getWrenchTooltipKey() {
		return hasSpeedIndicator() ? "create_railway_systems.ks_signal.tooltip.wrench" : null;
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if (!hasSpeedIndicator())
			return super.onWrenched(state, context);

		// Create kennt keine Geschwindigkeiten: Zs3 wird per Schraubenschlüssel vorgegeben
		Level level = context.getLevel();
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		BlockState newState = state.cycle(ZS3);
		level.setBlock(context.getClickedPos(), newState, 3);
		int speed = newState.getValue(ZS3);
		if (context.getPlayer() != null)
			context.getPlayer()
				.displayClientMessage(speed == 0 ? Component.translatable("create_railway_systems.ks_signal.zs3_off")
					: Component.translatable("create_railway_systems.ks_signal.zs3", speed * 10), true);
		IWrenchable.playRotateSound(level, context.getClickedPos());
		withBlockEntityDo(level, context.getClickedPos(), LinkedSignalBlockEntity::updateAspect);
		return InteractionResult.SUCCESS;
	}

}
