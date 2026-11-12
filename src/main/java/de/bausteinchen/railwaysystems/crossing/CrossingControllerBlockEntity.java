package de.bausteinchen.railwaysystems.crossing;

import java.util.List;

import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import de.bausteinchen.railwaysystems.Config;
import de.bausteinchen.railwaysystems.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Logik der Bahnübergangs-Steuerung: fragt alle {@link #SCAN_INTERVAL} Ticks den
 * {@link CrossingApproachScanner} und hält den Bahnübergang nach dem letzten Treffer noch
 * {@link #HOLD_TICKS} lang eingeschaltet. Lichtzeichen und Schranken auf derselben Funkfrequenz folgen
 * mit ihrem eigenen Ablauf (Gelb, Rot, Schranke senken). Mehrere Steuerungen (z. B. eine je Gleis) auf
 * derselben Frequenz ergänzen sich.
 */
public class CrossingControllerBlockEntity extends SmartBlockEntity {

	public static final int SCAN_INTERVAL = 4;
	/** Nachlauf nach dem letzten Treffer: 2 s */
	public static final int HOLD_TICKS = 40;

	private TrackTargetingBehaviour<LevelCrossingPoint> edgePoint;
	private LinkBehaviour link;
	private final CrossingApproachScanner scanner = new CrossingApproachScanner();
	private int holdTicks;
	private int scanCooldown;

	public CrossingControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CROSSING_CONTROLLER.get(), pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		edgePoint = new TrackTargetingBehaviour<>(this, LevelCrossingPoint.TYPE);
		behaviours.add(edgePoint);
		link = LinkBehaviour.transmitter(this,
			ValueBoxTransform.Dual.makeSlots(first -> new CrossingLinkSlot(first, 12.75f)), () -> isActive() ? 15 : 0);
		behaviours.add(link);
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		if (scanCooldown-- > 0)
			return;
		scanCooldown = SCAN_INTERVAL - 1;

		boolean triggered = scanner.scan(edgePoint.getEdgePoint(), Config.CROSSING_WARNING_TIME.get() * 20,
			Config.CROSSING_SCAN_DISTANCE.get());
		if (triggered)
			holdTicks = HOLD_TICKS;
		else
			holdTicks = Math.max(0, holdTicks - SCAN_INTERVAL);
		setActive(holdTicks > 0);
	}

	public boolean isActive() {
		BlockState state = getBlockState();
		return state.hasProperty(CrossingControllerBlock.ACTIVE) && state.getValue(CrossingControllerBlock.ACTIVE);
	}

	private void setActive(boolean active) {
		BlockState state = getBlockState();
		if (!state.hasProperty(CrossingControllerBlock.ACTIVE) || state.getValue(CrossingControllerBlock.ACTIVE) == active)
			return;
		level.setBlock(worldPosition, state.setValue(CrossingControllerBlock.ACTIVE, active), 3);
		level.updateNeighborsAt(worldPosition, state.getBlock());
		link.notifySignalChange();
	}

	public TrackTargetingBehaviour<LevelCrossingPoint> getEdgePoint() {
		return edgePoint;
	}

	@Override
	protected AABB createRenderBoundingBox() {
		// die Markierung auf dem Gleis liegt außerhalb des Blocks
		return new AABB(worldPosition).minmax(new AABB(edgePoint.getGlobalPosition()))
			.inflate(2);
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (!clientPacket)
			tag.putInt("HoldTicks", holdTicks);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		if (!clientPacket)
			holdTicks = tag.getInt("HoldTicks");
	}

}
