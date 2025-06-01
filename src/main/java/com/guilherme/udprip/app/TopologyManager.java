package com.guilherme.udprip.app;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Manages the virtual topology by maintaining neighbor relationships and link weights. */
public class TopologyManager {
  private static final Logger logger = LoggerFactory.getLogger(TopologyManager.class);

  // Maps neighbor IP to link weight
  private final Map<String, Integer> neighbors = new ConcurrentHashMap<>();

  /**
   * Add a neighbor with the specified link weight.
   *
   * @param neighborIp The neighbor's IP address
   * @param weight The link weight
   * @return true if the neighbor was added, false if it already existed
   */
  public boolean addNeighbor(String neighborIp, int weight) {
    Integer oldWeight = neighbors.put(neighborIp, weight);
    if (oldWeight == null) {
      logger.info("Added neighbor {} with weight {}", neighborIp, weight);
      return true;
    } else if (oldWeight != weight) {
      logger.info("Updated neighbor {} weight from {} to {}", neighborIp, oldWeight, weight);
      return true;
    }
    return false;
  }

  /**
   * Remove a neighbor.
   *
   * @param neighborIp The neighbor's IP address
   * @return true if the neighbor was removed, false if it didn't exist
   */
  public boolean removeNeighbor(String neighborIp) {
    Integer weight = neighbors.remove(neighborIp);
    if (weight != null) {
      logger.info("Removed neighbor {}", neighborIp);
      return true;
    }
    return false;
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
