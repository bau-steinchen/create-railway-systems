package de.bausteinchen.railwaysystems.crossing;

import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.signal.SingleBlockEntityEdgePoint;

import de.bausteinchen.railwaysystems.RailwaySystems;

/**
 * Gleispunkt eines Bahnübergangs: die Stelle, an der die Straße das Gleis kreuzt. Er trägt keine eigene
 * Logik; die {@link CrossingControllerBlockEntity} sucht von hier aus nach herannahenden Zügen.
 */
public class LevelCrossingPoint extends SingleBlockEntityEdgePoint {

	public static final EdgePointType<LevelCrossingPoint> TYPE =
		EdgePointType.register(RailwaySystems.asResource("level_crossing"), LevelCrossingPoint::new);

	/** Lädt die Klasse und meldet damit den Gleispunkt-Typ bei Create an (im Mod-Konstruktor aufrufen). */
	public static void register() {}

}
