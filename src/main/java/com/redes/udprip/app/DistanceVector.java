package com.redes.udprip.app;

import com.redes.udprip.model.RoutingEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Implements the distance vector routing algorithm. */
public class DistanceVector {
  private static final Logger logger = LoggerFactory.getLogger(DistanceVector.class);

  private final String localAddress;
  private final Map<String, RoutingEntry> routingTable = new ConcurrentHashMap<>();
  private static final Integer MAX_ROUTE_LENGTH = 255;

  public DistanceVector(String localAddress) {
    this.localAddress = localAddress;
    routingTable.put(localAddress, new RoutingEntry(localAddress, 0, localAddress, localAddress));
  }

  /**
   * Applies updates from a neighbor's distance vector.
   *
   * @param neighborIp The IP address of the neighbor
   * @param neighborDistances The distance vector received from the neighbor
   * @param linkWeight The weight of the link to the neighbor
   */
  public synchronized void applyUpdate(
      String neighborIp, Map<String, Integer> neighborDistances, int linkWeight) {
    RoutingEntry neighborEntry = routingTable.get(neighborIp);
    if (neighborEntry == null) {
      routingTable.put(
          neighborIp, new RoutingEntry(neighborIp, linkWeight, neighborIp, neighborIp));
    }

    for (Map.Entry<String, Integer> entry : neighborDistances.entrySet()) {
      String destination = entry.getKey();
      int distanceThroughNeighbor = linkWeight + entry.getValue();

      if (distanceThroughNeighbor > MAX_ROUTE_LENGTH) {
        continue;
      }

      RoutingEntry currentEntry = routingTable.get(destination);
      if (currentEntry == null) {
        routingTable.put(
            destination,
            new RoutingEntry(destination, distanceThroughNeighbor, neighborIp, neighborIp));
        logger.debug(
            "Added new route to {} via {} with distance {}",
            destination,
            neighborIp,
            distanceThroughNeighbor);
      } else if (currentEntry.getLearnedFrom().equals(neighborIp)
          && distanceThroughNeighbor != currentEntry.getDistance()) {
        currentEntry.setDistance(distanceThroughNeighbor);
        currentEntry.updateTimestamp();
        logger.debug(
            "Updated route to {} via {} with new distance {}",
            destination,
            neighborIp,
            distanceThroughNeighbor);
      } else if (distanceThroughNeighbor < currentEntry.getDistance()) {
        currentEntry.setDistance(distanceThroughNeighbor);
        currentEntry.setNextHop(neighborIp);
        currentEntry.setLearnedFrom(neighborIp);
        currentEntry.updateTimestamp();
        logger.debug(
            "Found better route to {} via {} with distance {}",
            destination,
            neighborIp,
            distanceThroughNeighbor);
      }
    }
  }

  /**
   * Get the distances to all known destinations for a specific neighbor, applying split-horizon
   * rule.
   *
   * @param neighborIp The neighbor IP address
   * @return A map of destination IPs to distances
   */
  public synchronized Map<String, Integer> getDistancesForNeighbor(String neighborIp) {
    Map<String, Integer> distances = new HashMap<>();

    for (RoutingEntry entry : routingTable.values()) {
      if (entry.getLearnedFrom().equals(neighborIp)) {
        continue;
      }

      String destination = entry.getDestination();

      distances.put(destination, entry.getDistance());
    }

    return distances;
  }

  /**
   * Removes routes learned from a specific neighbor.
   *
   * @param neighborIp The neighbor IP address
   */
  public synchronized void removeRoutesVia(String neighborIp) {
    routingTable.entrySet().removeIf(entry -> entry.getValue().getNextHop().equals(neighborIp));
  }

  /**
   * Removes routes for stale neighbors.
   *
   * @param staleNeighbors List of neighbors considered stale
   */
  public synchronized void removeRoutesForStaleNeighbors(List<String> staleNeighbors) {
    for (String neighborIp : staleNeighbors) {
      removeRoutesVia(neighborIp);
    }
  }

  /**
   * Checks if a route exists to a destination.
   *
   * @param destination The destination IP address
   * @return true if a route exists, false otherwise
   */
  public synchronized boolean hasRoute(String destination) {
    return destination.equals(localAddress) || routingTable.containsKey(destination);
  }

  /**
   * Get the next hop for a destination.
   *
   * @param destination The destination IP address
   * @return The next hop IP address or null if no route exists
   */
  public synchronized String getNextHop(String destination) {
    if (destination.equals(localAddress)) {
      return localAddress;
    }

    RoutingEntry entry = routingTable.get(destination);
    return entry != null ? entry.getNextHop() : null;
  }
}
