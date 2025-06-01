package com.guilherme.udprip.app;

import com.guilherme.udprip.model.RoutingEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Implements the distance vector routing algorithm. */
public class DistanceVector {
  private static final Logger logger = LoggerFactory.getLogger(DistanceVector.class);

  private final Map<String, Integer> neighbors = new ConcurrentHashMap<>();
  private final String localAddress;
  private final int updatePeriod;
  private final Map<String, RoutingEntry> routingTable = new ConcurrentHashMap<>();

  public DistanceVector(String localAddress, int updatePeriod) {
    this.localAddress = localAddress;
    this.updatePeriod = updatePeriod;
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

  /**
   * Applies updates from a neighbor's distance vector.
   *
   * @param neighborIp The IP address of the neighbor
   * @param neighborDistances The distance vector received from the neighbor
   * @param linkWeight The weight of the link to the neighbor
   */
  public synchronized void applyUpdate(
      String neighborIp, Map<String, Integer> neighborDistances, int linkWeight) {
    // Update timestamp for the neighbor itself
    RoutingEntry neighborEntry = routingTable.get(neighborIp);
    if (neighborEntry != null) {
      neighborEntry.updateTimestamp();
    } else {
      // Add the neighbor to the routing table if not already present
      routingTable.put(
          neighborIp, new RoutingEntry(neighborIp, linkWeight, neighborIp, neighborIp));
    }

    // Process each destination in the neighbor's distance vector
    for (Map.Entry<String, Integer> entry : neighborDistances.entrySet()) {
      String destination = entry.getKey();
      int distanceThroughNeighbor = linkWeight + entry.getValue();

      // Skip if the destination is the local router
      if (destination.equals(localAddress)) {
        continue;
      }

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
      } else if (currentEntry.getLearnedFrom().equals(neighborIp)) {
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

    // Always include the local router with distance based on link weight
    RoutingEntry neighborEntry = routingTable.get(neighborIp);
    if (neighborEntry != null) {
      distances.put(localAddress, neighborEntry.getDistance());
    }

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
    routingTable
        .entrySet()
        .removeIf(
            entry ->
                entry.getValue().getLearnedFrom().equals(neighborIp)
                    && !entry.getKey().equals(neighborIp));
  }

  /** Invalidates routes that haven't been updated within the timeout period. */
  public synchronized void invalidateStaleRoutes() {
    long timeout = updatePeriod * 4 * 1000L; // Convert to milliseconds (4 * update period)
    long now = System.currentTimeMillis();

    // Find stale entries
    Map<String, RoutingEntry> staleEntries =
        routingTable.entrySet().stream()
            .filter(entry -> now - entry.getValue().getLastUpdated() > timeout)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // Remove stale entries and their dependent routes
    for (String staleIp : staleEntries.keySet()) {
      logger.info("Removing stale route to {}", staleIp);
      routingTable.remove(staleIp);
      removeRoutesVia(staleIp);
    }
  }

  /**
   * Adds a direct route to a neighbor.
   *
   * @param neighborIp The neighbor IP address
   * @param weight The link weight
   */
  public synchronized void addDirectRoute(String neighborIp, int weight) {
    routingTable.put(neighborIp, new RoutingEntry(neighborIp, weight, neighborIp, neighborIp));
  }

  /**
   * Removes a direct route to a neighbor and all routes learned through it.
   *
   * @param neighborIp The neighbor IP address
   */
  public synchronized void removeDirectRoute(String neighborIp) {
    routingTable.remove(neighborIp);
    removeRoutesVia(neighborIp);
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
   * Add a neighbor with the specified link weight.
   *
   * @param neighborIp The neighbor's IP address
   * @param weight The link weight
   * @return true if the neighbor was added or updated, false otherwise
   */
  public synchronized boolean addNeighbor(String neighborIp, int weight) {
    Integer oldWeight = neighbors.put(neighborIp, weight);
    boolean updated = false;

    if (oldWeight == null) {
      logger.info("Added neighbor {} with weight {}", neighborIp, weight);
      updated = true;
    } else if (oldWeight != weight) {
      logger.info("Updated neighbor {} weight from {} to {}", neighborIp, oldWeight, weight);
      updated = true;
    }

    if (updated) {
      // Update routing table with direct route to this neighbor
      addDirectRoute(neighborIp, weight);
    }

    return updated;
  }

  /**
   * Remove a neighbor.
   *
   * @param neighborIp The neighbor's IP address
   */
  public synchronized void removeNeighbor(String neighborIp) {
    Integer weight = neighbors.remove(neighborIp);
    if (weight != null) {
      logger.info("Removed neighbor {}", neighborIp);
      // Remove route to this neighbor and all routes learned from it
      removeDirectRoute(neighborIp);
    }
  }

  /**
   * Get the weight of the link to a neighbor.
   *
   * @param neighborIp The neighbor's IP address
   * @return The link weight, or null if the neighbor doesn't exist
   */
  public Integer getLinkWeight(String neighborIp) {
    return neighbors.get(neighborIp);
  }

  /**
   * Check if a router is a neighbor.
   *
   * @param ip The IP address to check
   * @return true if the IP is a neighbor, false otherwise
   */
  public boolean isNeighbor(String ip) {
    return neighbors.containsKey(ip);
  }

  /**
   * Get all neighbors.
   *
   * @return A set of all neighbor IP addresses
   */
  public Set<String> getAllNeighbors() {
    return neighbors.keySet();
  }
}
