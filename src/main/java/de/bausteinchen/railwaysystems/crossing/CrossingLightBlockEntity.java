package de.bausteinchen.railwaysystems.crossing;

import java.util.List;

import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import de.bausteinchen.railwaysystems.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Schaltet ein Bahnübergangs-Lichtzeichen. Eingeschaltet ist es, solange Redstone am Block anliegt oder
 * auf der Frequenz der eingebauten Funkverbindung ein Signal ankommt. Das Lichtzeichen zeigt dann
 * {@link #YELLOW_TICKS} lang Gelb und danach Dauerrot, das Blinklicht sofort rotes Blinken.
 */
public class CrossingLightBlockEntity extends SmartBlockEntity {

	/** Gelbphase des Lichtzeichens: 3 s wie bei der DB */
	public static final int YELLOW_TICKS = 60;

	private int receivedStrength;
	/** Ticks seit dem Einschalten; -1 = aus */
	private int activeTicks = -1;
	/**
	 * Nach dem Laden kommt das Funksignal erst an, wenn auch der Sender geladen ist; so lange nicht
	 * ausschalten, sonst begänne ein laufender Bahnübergang nach jedem Neuladen von vorn.
	 */
	private int loadGrace = 20;

	public CrossingLightBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CROSSING_LIGHT.get(), pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(LinkBehaviour.receiver(this, ValueBoxTransform.Dual.makeSlots(CrossingLinkSlot::new),
			strength -> receivedStrength = strength));
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		BlockState state = getBlockState();
		if (!(state.getBlock() instanceof CrossingLightBlock light))
			return;

		boolean active = receivedStrength > 0 || level.hasNeighborSignal(worldPosition);
		if (loadGrace > 0)
			loadGrace--;
		if (!active && loadGrace > 0 && activeTicks >= 0)
			return;
		if (!active)
			activeTicks = -1;
		else if (activeTicks < YELLOW_TICKS)
			activeTicks++;

		CrossingPhase phase = !active ? CrossingPhase.OFF
			: activeTicks < YELLOW_TICKS ? CrossingPhase.YELLOW : CrossingPhase.RED;
		BlockState shown = light.withPhase(state, phase);
		if (shown != state)
			level.setBlock(worldPosition, shown, 3);
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (!clientPacket)
			tag.putInt("ActiveTicks", activeTicks);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		if (!clientPacket && tag.contains("ActiveTicks"))
			activeTicks = tag.getInt("ActiveTicks");
	}

}
