package de.bausteinchen.railwaysystems.signal.ks;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import de.bausteinchen.railwaysystems.signal.LinkedSignalBlockEntity;
import de.bausteinchen.railwaysystems.signal.MountedSignalBlock;
import de.bausteinchen.railwaysystems.signal.SignalFeature;
import de.bausteinchen.railwaysystems.signal.SignalInput;
import de.bausteinchen.railwaysystems.signal.SignalInput.NextSignal;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Gemeinsame Basis der Ks-Signale. Hauptsignale tragen einen Geschwindigkeitsanzeiger Zs3
 * (per Schraubenschlüssel einstellbar), Vorsignale einen Geschwindigkeitsvoranzeiger Zs3v,
 * der die Geschwindigkeit des nächsten Hauptsignals ankündigt. Beide Werte liegen im
 * Block-Entity und werden vom Overlay-Renderer angezeigt.
 */
public abstract class KsSignalBlock extends MountedSignalBlock {

	protected KsSignalBlock(Properties properties) {
		super(properties);
	}

	public boolean hasSpeedIndicator() {
		return getFeatures().contains(SignalFeature.ZS3);
	}

	/**
	 * Vorsignalbegriff: Ks2 bei Halt, Ks1 blinkend mit Zs3v bei angekündigter Geschwindigkeit.
	 */
	protected static BlockState withDistantAspect(BlockState state, NextSignal next, EnumProperty<KsAspect> aspect,
		SignalInput input) {
		LinkedSignalBlockEntity be = input.getBlockEntity();
		if (!next.clear()) {
			be.setZs3v(0);
			return state.setValue(aspect, KsAspect.KS2);
		}
		be.setZs3v(next.speedLimit());
		return state.setValue(aspect, next.speedLimit() > 0 ? KsAspect.KS1_BLINK : KsAspect.KS1);
	}

	protected static boolean isProceed(KsAspect aspect) {
		return aspect == KsAspect.KS1 || aspect == KsAspect.KS1_BLINK || aspect == KsAspect.KS2;
	}

	@Override
	public int getSpeedLimit(BlockState state, LinkedSignalBlockEntity blockEntity) {
		return hasSpeedIndicator() ? blockEntity.getZs3() : -1;
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
		return onBlockEntityUse(level, getMainPos(state, context.getClickedPos()), be -> {
			int speed = be.cycleZs3();
			if (context.getPlayer() != null)
				context.getPlayer()
					.displayClientMessage(speed == 0 ? Component.translatable("create_railway_systems.ks_signal.zs3_off")
						: Component.translatable("create_railway_systems.ks_signal.zs3", speed * 10), true);
			IWrenchable.playRotateSound(level, context.getClickedPos());
			return InteractionResult.SUCCESS;
		});
	}

}
