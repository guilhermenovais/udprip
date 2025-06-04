package com.redes.udprip.model;

/** Represents an entry in the routing table. */
public class RoutingEntry {
  private final String destination;
  private int distance;
  private String nextHop;
  private String learnedFrom;
  private long lastUpdated;

  public RoutingEntry(String destination, int distance, String nextHop, String learnedFrom) {
    this.destination = destination;
    this.distance = distance;
    this.nextHop = nextHop;
    this.learnedFrom = learnedFrom;
    this.lastUpdated = System.currentTimeMillis();
  }

  public String getDestination() {
    return destination;
  }

  public int getDistance() {
    return distance;
  }

  public void setDistance(int distance) {
    this.distance = distance;
  }

  public String getNextHop() {
    return nextHop;
  }

  public void setNextHop(String nextHop) {
    this.nextHop = nextHop;
  }

  public String getLearnedFrom() {
    return learnedFrom;
  }

  public void setLearnedFrom(String learnedFrom) {
    this.learnedFrom = learnedFrom;
  }

  public long getLastUpdated() {
    return lastUpdated;
  }

  public void updateTimestamp() {
    this.lastUpdated = System.currentTimeMillis();
  }

  @Override
  public String toString() {
    return String.format(
        "RoutingEntry{destination='%s', distance=%d, nextHop='%s', learnedFrom='%s', lastUpdated=%d}",
        destination, distance, nextHop, learnedFrom, lastUpdated);
  }
}
