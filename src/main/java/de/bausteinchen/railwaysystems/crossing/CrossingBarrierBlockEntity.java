package de.bausteinchen.railwaysystems.crossing;

import java.util.List;

import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import de.bausteinchen.railwaysystems.registry.ModBlockEntities;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Schrankenantrieb: Eingeschaltet (Redstone am Block oder Funkverbindung auf der Rückseite) senkt er den
 * Baum nach {@link #LOWER_DELAY_TICKS}, damit das Lichtzeichen erst Gelb und Rot zeigen kann; beim
 * Ausschalten hebt er ihn sofort. Der Winkel läuft auf Server und Client gleich mit, damit der Server
 * später wissen kann, wann die Schranke unten ist.
 */
public class CrossingBarrierBlockEntity extends SmartBlockEntity {

	/** Wartezeit vom Einschalten bis zum Senken: 3 s Gelb + 2 s Rot */
	public static final int LOWER_DELAY_TICKS = 100;
	/** Dauer einer Bewegung zwischen offen und geschlossen: 6 s */
	public static final int MOVE_TICKS = 120;
	public static final int MIN_LENGTH = 2, MAX_LENGTH = 8, DEFAULT_LENGTH = 4;
	/** Baumwinkel in Grad: offen senkrecht, geschlossen waagerecht */
	public static final float OPEN = 90, CLOSED = 0;

	private int receivedStrength;
	/** Ticks seit dem Einschalten; -1 = aus */
	private int activeTicks = -1;
	private LerpedFloat angle;
	private ScrollValueBehaviour length;

	public CrossingBarrierBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CROSSING_BARRIER.get(), pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(LinkBehaviour.receiver(this,
			ValueBoxTransform.Dual.makeSlots(first -> new CrossingLinkSlot(first, 13.25f)),
			strength -> receivedStrength = strength));
		length = new ScrollValueBehaviour(Component.translatable("create_railway_systems.barrier.length"), this,
			new CenteredSideValueBoxTransform((state, side) -> side == Direction.UP));
		length.between(MIN_LENGTH, MAX_LENGTH);
		length.setValue(DEFAULT_LENGTH);
		length.withCallback(value -> invalidateRenderBoundingBox());
		length.requiresWrench();
		behaviours.add(length);
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		BlockState state = getBlockState();
		if (!state.hasProperty(CrossingBarrierBlock.CLOSED))
			return;

		if (!level.isClientSide) {
			boolean active = receivedStrength > 0 || level.hasNeighborSignal(worldPosition);
			if (!active)
				activeTicks = -1;
			else if (activeTicks < LOWER_DELAY_TICKS)
				activeTicks++;
			boolean closed = active && activeTicks >= LOWER_DELAY_TICKS;
			if (closed != state.getValue(CrossingBarrierBlock.CLOSED)) {
				state = state.setValue(CrossingBarrierBlock.CLOSED, closed);
				level.setBlock(worldPosition, state, 3);
			}
		}

		float target = state.getValue(CrossingBarrierBlock.CLOSED) ? CLOSED : OPEN;
		if (angle == null)
			// beim Laden sofort in Endstellung; nur Wechsel danach werden animiert
			angle = LerpedFloat.linear()
				.startWithValue(target);
		angle.chase(target, (OPEN - CLOSED) / MOVE_TICKS, Chaser.LINEAR);
		angle.tickChaser();
	}

	/** Baumwinkel in Grad (90 = offen, 0 = geschlossen) */
	public float getAngle(float partialTicks) {
		if (angle == null)
			return getBlockState().getOptionalValue(CrossingBarrierBlock.CLOSED)
				.orElse(false) ? CLOSED : OPEN;
		return angle.getValue(partialTicks);
	}

	/** Schranke vollständig geschlossen (Baum unten) */
	public boolean isFullyClosed() {
		return getBlockState().getOptionalValue(CrossingBarrierBlock.CLOSED)
			.orElse(false) && getAngle(1) <= CLOSED + 0.01f;
	}

	public int getLength() {
		return length == null ? DEFAULT_LENGTH : length.getValue();
	}

	@Override
	protected AABB createRenderBoundingBox() {
		// Baum reicht offen nach oben, geschlossen zur Seite
		return new AABB(worldPosition).inflate(getLength() + 1);
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
		if (clientPacket)
			invalidateRenderBoundingBox();
	}

}
