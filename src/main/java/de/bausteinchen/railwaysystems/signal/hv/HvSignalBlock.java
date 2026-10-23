package de.bausteinchen.railwaysystems.signal.hv;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import de.bausteinchen.railwaysystems.signal.LinkedSignalBlockEntity;
import de.bausteinchen.railwaysystems.signal.MountedSignalBlock;
import de.bausteinchen.railwaysystems.signal.SignalInput;
import de.bausteinchen.railwaysystems.signal.SignalInput.NextSignal;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Gemeinsame Basis der H/V-Lichtsignale.
 */
public abstract class HvSignalBlock extends MountedSignalBlock {

	public static final EnumProperty<HpAspect> MAIN_ASPECT = EnumProperty.create("aspect", HpAspect.class);
	public static final EnumProperty<VrAspect> DISTANT_ASPECT = EnumProperty.create("distant", VrAspect.class);

	/** Hp2 (Langsamfahrt) entspricht 40 km/h. */
	private static final int HP2_SPEED_LIMIT = 4;

	protected HvSignalBlock(Properties properties) {
		super(properties);
	}

	/**
	 * Ob dieses Signal einen Hauptsignalbegriff (Hp) zeigt.
	 */
	public boolean hasMainAspect() {
		return getStateDefinition().getProperties()
			.contains(MAIN_ASPECT);
	}

	protected static HpAspect mainAspect(SignalInput input) {
		if (!input.isClear())
			return HpAspect.HP0;
		return input.getBlockEntity()
			.isSlowMode() ? HpAspect.HP2 : HpAspect.HP1;
	}

	protected static VrAspect distantAspect(NextSignal next) {
		if (!next.clear())
			return VrAspect.VR0;
		return next.speedLimit() > 0 ? VrAspect.VR2 : VrAspect.VR1;
	}

	@Override
	public int getSpeedLimit(BlockState state, LinkedSignalBlockEntity blockEntity) {
		if (!hasMainAspect())
			return -1;
		return blockEntity.isSlowMode() ? HP2_SPEED_LIMIT : 0;
	}

	@Override
	@Nullable
	public String getWrenchTooltipKey() {
		return hasMainAspect() ? "create_railway_systems.hv_signal.tooltip.wrench" : null;
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if (!hasMainAspect())
			return super.onWrenched(state, context);

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

}
