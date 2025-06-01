package com.guilherme.udprip.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Represents an update message in the UDPRIP protocol. Update messages contain distance vector
 * information from a router to its neighbors.
 */
public class UpdateMessage implements Message {
  @JsonProperty("type")
  private final String type = "update";

  @JsonProperty("source")
  private String source;

  @JsonProperty("destination")
  private String destination;

  @JsonProperty("distances")
  private Map<String, Integer> distances;

  // Required for Jackson deserialization
  public UpdateMessage() {}

  public UpdateMessage(String source, String destination, Map<String, Integer> distances) {
    this.source = source;
    this.destination = destination;
    this.distances = distances;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  @Override
  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public Map<String, Integer> getDistances() {
    return distances;
  }

  public void setDistances(Map<String, Integer> distances) {
    this.distances = distances;
  }
}
