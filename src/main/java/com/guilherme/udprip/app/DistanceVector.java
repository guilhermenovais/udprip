package com.guilherme.udprip.app;

import com.guilherme.udprip.model.RoutingEntry;
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
    // Make sure we have a routing entry for this neighbor
    RoutingEntry neighborEntry = routingTable.get(neighborIp);
    if (neighborEntry == null) {
      // Add the neighbor to the routing table if not already present
      routingTable.put(
          neighborIp, new RoutingEntry(neighborIp, linkWeight, neighborIp, neighborIp));
    }

    // Process each destination in the neighbor's distance vector
    for (Map.Entry<String, Integer> entry : neighborDistances.entrySet()) {
      String destination = entry.getKey();
      int distanceThroughNeighbor = linkWeight + entry.getValue();

      // Bellman-Ford: Update if we found a better route or this is from the same neighbor we
      // learned from
      RoutingEntry currentEntry = routingTable.get(destination);
      if (currentEntry == null) {
        // New destination
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
        // Update from the same neighbor we learned this route from
        currentEntry.setDistance(distanceThroughNeighbor);
        currentEntry.updateTimestamp();
        logger.debug(
            "Updated route to {} via {} with new distance {}",
            destination,
            neighborIp,
            distanceThroughNeighbor);
      } else if (distanceThroughNeighbor < currentEntry.getDistance()) {
        // Better route found
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

    // Add all other destinations, applying split horizon
    for (RoutingEntry entry : routingTable.values()) {
      String destination = entry.getDestination();

      // Skip routes learned from this neighbor (split horizon)
      if (entry.getLearnedFrom().equals(neighborIp)) {
        continue;
      }

      // Skip the neighbor itself
      if (destination.equals(neighborIp)) {
        continue;
      }

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
      // Remove all routes that use this neighbor as next hop
      removeRoutesVia(neighborIp);
    }
  }

  /**
   * Adds a direct route to a neighbor.
   *
   * @param neighborIp The neighbor IP address
   * @param weight The link weight
   */
  public synchronized void addDirectRoute(String neighborIp, int weight) {
    RoutingEntry neighborEntry = routingTable.get(neighborIp);
    if (neighborEntry == null || weight <= neighborEntry.getDistance()) {
      routingTable.put(neighborIp, new RoutingEntry(neighborIp, weight, neighborIp, neighborIp));
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
    // If destination is self, return self
    if (destination.equals(localAddress)) {
      return localAddress;
    }

    RoutingEntry entry = routingTable.get(destination);
    return entry != null ? entry.getNextHop() : null;
  }
}
