package de.bausteinchen.railwaysystems.traffic;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import de.bausteinchen.railwaysystems.registry.ModBlockEntities;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Daten einer Straßenampel: gebundenes Create-Signal, zuständiges Steuergerät und bei Masten die
 * Bauform des Auslegers.
 * <p>
 * Zeigt die Ampel Rot, Rot-Gelb oder Gelb, hält sie das gebundene Create-Signal geschlossen. Dazu
 * meldet sie es bei dessen {@link SignalBoundary} als redstone-gespeist, genauso wie Create es bei
 * einem Redstone-Signal am Signalblock selbst tut; Züge halten dann davor. Der Abgleich läuft jeden
 * Tick über {@link TrafficSignalLocks}, nichts davon wird gespeichert.
 */
public class TrafficLightBlockEntity extends SmartBlockEntity {

	@Nullable
	private BlockPos targetOffset;
	@Nullable
	private BlockPos controllerOffset;
	private MastConfig config = MastConfig.DEFAULT;

	public TrafficLightBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.TRAFFIC_LIGHT.get(), pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		// jeden Tick: Create setzt den Eintrag zurück, sobald sich echter Redstone am Signal ändert
		updateHold();
	}

	private TrafficAspect getAspect() {
		BlockState state = getBlockState();
		return state.getBlock() instanceof TrafficLight light ? light.getAspect(state) : TrafficAspect.OFF;
	}

	private void updateHold() {
		BlockPos target = getTargetPosition();
		if (target == null)
			return;
		GlobalPos signal = GlobalPos.of(level.dimension(), target);
		TrafficSignalLocks.set(signal, worldPosition, getAspect().holdsTraffic());
		apply(target, TrafficSignalLocks.isHeld(signal));
	}

	/**
	 * Eintrag des Create-Signals in seiner {@link SignalBoundary} setzen: rot erzwungen, solange eine Ampel
	 * hält, sonst so, wie Create ihn selbst aus dem Redstone am Signal setzen würde.
	 */
	private void apply(BlockPos target, boolean held) {
		if (!level.isLoaded(target) || !(level.getBlockEntity(target) instanceof SignalBlockEntity sbe))
			return;
		SignalBoundary boundary = sbe.getSignal();
		if (boundary == null)
			return;
		boolean forced = held || sbe.getReportedPower();
		for (boolean side : Iterate.trueAndFalse) {
			Map<BlockPos, Boolean> signals = boundary.blockEntities.get(side);
			Boolean current = signals.get(target);
			if (current != null && current != forced)
				signals.put(target, forced);
		}
	}

	private void release() {
		// remove() und onChunkUnloaded() laufen auch auf dem Client; Create-Signale gehören dem Server
		if (level == null || level.isClientSide)
			return;
		BlockPos target = getTargetPosition();
		if (target == null)
			return;
		GlobalPos signal = GlobalPos.of(level.dimension(), target);
		TrafficSignalLocks.set(signal, worldPosition, false);
		apply(target, TrafficSignalLocks.isHeld(signal));
	}

	@Override
	public void remove() {
		super.remove();
		release();
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		// sonst bliebe das Create-Signal rot, solange niemand die Ampel lädt
		release();
	}

	@Nullable
	public BlockPos getTargetPosition() {
		return targetOffset == null ? null : worldPosition.offset(targetOffset);
	}

	@Nullable
	public BlockPos getControllerPosition() {
		return controllerOffset == null ? null : worldPosition.offset(controllerOffset);
	}

	public void setControllerPosition(@Nullable BlockPos controller) {
		controllerOffset = controller == null ? null : controller.subtract(worldPosition);
		setChanged();
	}

	public MastConfig getConfig() {
		return config;
	}

	public void setConfig(MastConfig config) {
		this.config = config.clamped();
		invalidateRenderBoundingBox();
		notifyUpdate();
	}

	@Override
	protected AABB createRenderBoundingBox() {
		if (!(getBlockState().getBlock() instanceof TrafficMastBlock))
			return super.createRenderBoundingBox();
		// Ausleger reicht in jede Richtung bis zur Ausladung; Bogen und Ampel ragen über den Mast hinaus
		int reach = config.reach() + 1;
		return new AABB(worldPosition).inflate(reach, 0, reach)
			.expandTowards(0, config.height() + TrafficMastBlock.BEND_RADIUS + 1, 0);
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (targetOffset != null)
			tag.put("TargetOffset", NbtUtils.writeBlockPos(targetOffset));
		if (controllerOffset != null)
			tag.put("ControllerOffset", NbtUtils.writeBlockPos(controllerOffset));
		tag.put("Mast", config.write());
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		targetOffset = tag.contains("TargetOffset") ? NBTHelper.readBlockPos(tag, "TargetOffset") : null;
		controllerOffset = tag.contains("ControllerOffset") ? NBTHelper.readBlockPos(tag, "ControllerOffset") : null;
		MastConfig newConfig = MastConfig.read(tag.getCompound("Mast"));
		if (!newConfig.equals(config)) {
			config = newConfig;
			if (clientPacket)
				invalidateRenderBoundingBox();
		}
	}

}
