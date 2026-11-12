package de.bausteinchen.railwaysystems.crossing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;

/**
 * Sucht Züge, die sich einem Bahnübergang nähern oder auf ihm stehen.
 * <p>
 * Vom Gleispunkt aus wird das Gleisnetz in beide Richtungen bis zu einer Höchstentfernung abgelaufen
 * (kürzester Weg zu jedem Knoten, getrennt nach Seite A und B des Übergangs). Gefolgt wird nur echten
 * Gleisverbindungen; Kreuzungen mit anderen Gleisen (z. B. einer Straße aus Phantom-Gleisen) sind in
 * Create keine Verbindungen und werden daher nie betreten.
 * <p>
 * Ein Zug schaltet den Bahnübergang ein, wenn
 * <ul>
 * <li>er sich nähert und ihn bei aktueller Geschwindigkeit innerhalb der Vorwarnzeit erreicht, oder</li>
 * <li>er ihn gerade befährt (Wagen auf beiden Seiten) bzw. ein Drehgestell unmittelbar daran steht.</li>
 * </ul>
 * Ein Zug, der vor dem Übergang steht (z. B. vor einem Signal), zählt nicht; er schaltet ihn erst wieder
 * ein, wenn er anfährt.
 */
public class CrossingApproachScanner {

	/** Mindestgeschwindigkeit (Blöcke/Tick), ab der ein Zug als fahrend gilt */
	private static final double MIN_SPEED = 0.01;
	/** Abstand (Blöcke), ab dem ein Drehgestell als „auf dem Übergang“ gilt */
	private static final double ON_CROSSING = 1.5;
	/** Wegnetz nach so vielen Abfragen neu ablaufen, falls Gleise geändert wurden */
	private static final int REBUILD_INTERVAL = 5;

	private record Reach(double distance, boolean sideA) {}

	private record Visit(TrackNode node, double distance, boolean sideA) {}

	private final Map<TrackNode, Reach> reach = new HashMap<>();
	private final Map<UUID, Double> lastDistance = new HashMap<>();
	@Nullable
	private TrackGraph graph;
	@Nullable
	private TrackNode nodeA, nodeB;
	@Nullable
	private TrackEdge crossingEdge;
	private double pointPosition;
	private double builtFor;
	private int age;

	/**
	 * @param warningTicks Vorwarnzeit: so lange vor Ankunft eines Zuges wird eingeschaltet
	 * @param maxDistance  Höchstentfernung der Suche in Blöcken
	 * @return ob ein Zug den Bahnübergang gerade einschaltet
	 */
	public boolean scan(@Nullable LevelCrossingPoint point, int warningTicks, double maxDistance) {
		TrackGraph current = point == null ? null : findGraph(point);
		if (current == null) {
			reset();
			return false;
		}
		if (current != graph || builtFor != maxDistance || age++ % REBUILD_INTERVAL == 0 || !isValid(current))
			rebuild(current, point, maxDistance);
		if (crossingEdge == null)
			return false;

		boolean triggered = false;
		Set<UUID> seen = new HashSet<>();
		for (Train train : Create.RAILWAYS.trains.values()) {
			if (train.graph != graph || train.derailed || train.invalid)
				continue;
			Double distance = trainDistance(train);
			if (distance == null) {
				lastDistance.remove(train.id);
				continue;
			}
			seen.add(train.id);
			if (distance < 0) {
				// befährt den Übergang
				lastDistance.put(train.id, 0d);
				triggered = true;
				continue;
			}
			Double previous = lastDistance.put(train.id, distance);
			double speed = Math.abs(train.speed);
			boolean approaching = previous != null && distance < previous - 1e-3;
			if (approaching && speed >= MIN_SPEED && distance / speed <= warningTicks)
				triggered = true;
		}
		lastDistance.keySet()
			.retainAll(seen);
		return triggered;
	}

	/**
	 * Kleinster Abstand des Zuges zum Übergang in Blöcken; negativ, wenn er ihn gerade befährt; null, wenn
	 * er außerhalb des Suchbereichs ist.
	 */
	@Nullable
	private Double trainDistance(Train train) {
		double minA = Double.MAX_VALUE, minB = Double.MAX_VALUE;
		for (Carriage carriage : train.carriages) {
			for (TravellingPoint tp : new TravellingPoint[] { carriage.getLeadingPoint(), carriage.getTrailingPoint() }) {
				if (tp == null || tp.edge == null)
					continue;
				Reach at = locate(tp);
				if (at == null)
					continue;
				if (at.sideA())
					minA = Math.min(minA, at.distance());
				else
					minB = Math.min(minB, at.distance());
			}
		}
		double min = Math.min(minA, minB);
		if (min == Double.MAX_VALUE)
			return null;
		if (min <= ON_CROSSING)
			return -1d;
		// Wagen auf beiden Seiten, die zusammen nicht weiter auseinander liegen als der Zug lang ist
		if (minA != Double.MAX_VALUE && minB != Double.MAX_VALUE && minA + minB <= trainLength(train) + 4)
			return -1d;
		return min;
	}

	@Nullable
	private Reach locate(TravellingPoint tp) {
		double length = tp.edge.getLength();
		if ((tp.node1 == nodeA && tp.node2 == nodeB) || (tp.node1 == nodeB && tp.node2 == nodeA)) {
			double fromA = tp.node1 == nodeA ? tp.position : length - tp.position;
			double delta = fromA - pointPosition;
			return new Reach(Math.abs(delta), delta < 0);
		}
		Reach r1 = reach.get(tp.node1), r2 = reach.get(tp.node2);
		Reach via1 = r1 == null ? null : new Reach(r1.distance() + tp.position, r1.sideA());
		Reach via2 = r2 == null ? null : new Reach(r2.distance() + length - tp.position, r2.sideA());
		if (via1 == null)
			return via2;
		if (via2 == null)
			return via1;
		return via1.distance() <= via2.distance() ? via1 : via2;
	}

	private static double trainLength(Train train) {
		double length = 0;
		for (Carriage carriage : train.carriages)
			length += carriage.bogeySpacing;
		for (Integer spacing : train.carriageSpacing)
			length += spacing;
		return length;
	}

	private boolean isValid(TrackGraph current) {
		return nodeA != null && nodeB != null && current.getConnectionsFrom(nodeA) != null
			&& current.getConnectionsFrom(nodeA)
				.get(nodeB) == crossingEdge;
	}

	private void rebuild(TrackGraph current, LevelCrossingPoint point, double maxDistance) {
		reach.clear();
		graph = current;
		builtFor = maxDistance;
		nodeA = current.locateNode(point.edgeLocation.getFirst());
		nodeB = current.locateNode(point.edgeLocation.getSecond());
		Map<TrackNode, TrackEdge> fromA = nodeA == null ? null : current.getConnectionsFrom(nodeA);
		crossingEdge = fromA == null || nodeB == null ? null : fromA.get(nodeB);
		if (crossingEdge == null)
			return;
		pointPosition = point.getLocationOn(crossingEdge);

		// Dijkstra von beiden Enden der Kante aus, auf der der Übergang liegt
		PriorityQueue<Visit> queue = new PriorityQueue<>((x, y) -> Double.compare(x.distance(), y.distance()));
		queue.add(new Visit(nodeA, pointPosition, true));
		queue.add(new Visit(nodeB, crossingEdge.getLength() - pointPosition, false));
		while (!queue.isEmpty()) {
			Visit visit = queue.poll();
			if (reach.containsKey(visit.node()))
				continue;
			reach.put(visit.node(), new Reach(visit.distance(), visit.sideA()));
			Map<TrackNode, TrackEdge> connections = current.getConnectionsFrom(visit.node());
			if (connections == null)
				continue;
			connections.forEach((next, edge) -> {
				if (edge == crossingEdge || reach.containsKey(next))
					return;
				if ((visit.node() == nodeB && next == nodeA) || (visit.node() == nodeA && next == nodeB))
					return;
				double distance = visit.distance() + edge.getLength();
				if (distance <= maxDistance)
					queue.add(new Visit(next, distance, visit.sideA()));
			});
		}
	}

	@Nullable
	private TrackGraph findGraph(LevelCrossingPoint point) {
		if (graph != null && graph.getPoint(LevelCrossingPoint.TYPE, point.getId()) == point)
			return graph;
		for (TrackGraph candidate : Create.RAILWAYS.trackNetworks.values())
			if (candidate.getPoint(LevelCrossingPoint.TYPE, point.getId()) != null)
				return candidate;
		return null;
	}

	private void reset() {
		graph = null;
		nodeA = nodeB = null;
		crossingEdge = null;
		reach.clear();
		lastDistance.clear();
	}

}
