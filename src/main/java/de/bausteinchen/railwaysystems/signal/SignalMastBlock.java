package de.bausteinchen.railwaysystems.signal;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Signalmast, vor dem Signale bündig mit Halterstangen sitzen oder auf den sie gesetzt werden.
 * Es gibt drei Bauformen, siehe {@link Type}. {@link #ROTATION} dreht den Mast in 16 Stufen zu 22,5°
 * im Uhrzeigersinn ab Nord: beim Platzieren in Blickrichtung, danach mit dem Schraubenschlüssel.
 * Signale am Mast drehen mit (siehe {@link #attachmentFor}).
 */
public class SignalMastBlock extends Block implements IWrenchable {

	public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;

	public enum Type {
		/** Standardmast: zwei Platten mit Sprossen dazwischen, bei Stufe 0 nach Nord und Süd. */
		STANDARD(6.5, 5.5, 9.5, 10.5),
		/** Leichter Mast: ein einzelner Stab. */
		LIGHT(6.5, 6.5, 9.5, 9.5),
		/** Gittermast: breit und quadratisch, Eckstiele mit Streben dazwischen. */
		LATTICE(3, 3, 13, 13);

		private final VoxelShape[] shapes = new VoxelShape[16];

		/**
		 * Grundriss bei Stufe 0 in Pixeln; gedreht wird um die Blockmitte, als Hitbox dient das
		 * achsparallele Rechteck um den gedrehten Grundriss.
		 */
		Type(double x0, double z0, double x1, double z1) {
			for (int step = 0; step < 16; step++) {
				double angle = Math.toRadians(step * 22.5);
				double cos = Math.abs(Math.cos(angle)), sin = Math.abs(Math.sin(angle));
				double halfX = (x1 - x0) / 2, halfZ = (z1 - z0) / 2;
				double extentX = halfX * cos + halfZ * sin, extentZ = halfX * sin + halfZ * cos;
				double cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
				shapes[step] = Block.box(cx - extentX, 0, cz - extentZ, cx + extentX, 16, cz + extentZ);
			}
		}
	}

	/**
	 * Montage eines Signals an diesem Mast: Art (bestimmt die Länge der Halterstangen) und Neigung
	 * gegenüber der Himmelsrichtung des Signals, siehe {@link MountedSignalBlock#TILT}.
	 */
	public record Attachment(SignalMount mount, int tilt) {}

	private final Type type;

	public SignalMastBlock(Type type, Properties properties) {
		super(properties);
		this.type = type;
		registerDefaultState(defaultBlockState().setValue(ROTATION, 0));
	}

	public Type getType() {
		return type;
	}

	/**
	 * Montage eines Signals, das zur Seite {@code towardSignal} vor diesem Mast sitzt. Der Kopf dreht
	 * zur nächstgelegenen Mastseite; die Neigung ist daher die Maststufe modulo 4.
	 */
	public Attachment attachmentFor(BlockState state, Direction towardSignal) {
		int rotation = state.getValue(ROTATION);
		int tilt = rotation % 4;
		SignalMount mount = switch (type) {
			case LATTICE -> SignalMount.MAST_LATTICE;
			// Standardmast: Platten zeigen in Stufe 0 nach Nord/Süd; die Seite zum Signal (in 22,5°-Stufen)
			// ist eine Platte, wenn sie um ein Vielfaches von 180° von der Mastvorderseite abweicht
			case STANDARD -> Math.floorMod(faceStep(towardSignal, tilt) - rotation, 8) == 0 ? SignalMount.MAST_PLATE
				: SignalMount.MAST;
			case LIGHT -> SignalMount.MAST;
		};
		return new Attachment(mount, tilt);
	}

	/**
	 * Richtung der Mastseite zum Signal in 22,5°-Stufen ab Nord: Himmelsrichtung des Signals plus
	 * gefaltete Neigung.
	 */
	private static int faceStep(Direction towardSignal, int tilt) {
		int cardinal = switch (towardSignal) {
			case EAST -> 4;
			case SOUTH -> 8;
			case WEST -> 12;
			default -> 0;
		};
		return cardinal + MountedSignalBlock.tiltSteps(tilt);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(ROTATION));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// Blickrichtung des Spielers (Gierwinkel 180° = Nord) auf die nächste 22,5°-Stufe ab Nord
		return defaultBlockState().setValue(ROTATION, Mth.floor((context.getRotation() - 180) / 22.5 + 0.5) & 15);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return type.shapes[state.getValue(ROTATION)];
	}

	@Override
	public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
		return originalState.setValue(ROTATION, (originalState.getValue(ROTATION) + 1) & 15);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 16));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), 16));
	}

}
