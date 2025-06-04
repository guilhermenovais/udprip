package com.redes.udprip.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Manages the virtual topology by maintaining neighbor relationships and link weights. */
public class TopologyManager {
  private static final Logger logger = LoggerFactory.getLogger(TopologyManager.class);

  private final Map<String, Integer> neighbors = new ConcurrentHashMap<>();
  private final Map<String, Long> lastUpdated = new ConcurrentHashMap<>();

  private final int updatePeriod;

  /**
   * Creates a new TopologyManager with the specified update period.
   *
   * @param updatePeriod The update period in seconds
   */
  public TopologyManager(int updatePeriod) {
    this.updatePeriod = updatePeriod;
  }

  /**
   * Add a neighbor with the specified link weight.
   *
   * @param neighborIp The neighbor's IP address
   * @param weight The link weight
   * @return true if the neighbor was added, false if it already existed
   */
  public boolean addNeighbor(String neighborIp, int weight) {
    Integer oldWeight = neighbors.put(neighborIp, weight);
    lastUpdated.put(neighborIp, System.currentTimeMillis());

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
   * Records that an update was received from a neighbor. This updates the timestamp for that
   * neighbor.
   *
   * @param neighborIp The neighbor's IP address
   * @return true if the neighbor exists and was updated, false otherwise
   */
  public boolean recordNeighborUpdate(String neighborIp) {
    if (neighbors.containsKey(neighborIp)) {
      lastUpdated.put(neighborIp, System.currentTimeMillis());
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
      lastUpdated.remove(neighborIp);
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

  /**
   * Finds neighbors that haven't been updated within the timeout period.
   *
   * @return A list of IPs of neighbors considered stale
   */
  public List<String> findStaleNeighbors() {
    long timeout = updatePeriod * 4 * 1000L;
    long now = System.currentTimeMillis();
    List<String> staleNeighbors = new ArrayList<>();

    for (Map.Entry<String, Long> entry : lastUpdated.entrySet()) {
      String neighborIp = entry.getKey();
      long lastUpdate = entry.getValue();

      if (now - lastUpdate > timeout) {
        staleNeighbors.add(neighborIp);
        logger.info("Detected stale neighbor: {}", neighborIp);
      }
    }

    return staleNeighbors;
  }
}
