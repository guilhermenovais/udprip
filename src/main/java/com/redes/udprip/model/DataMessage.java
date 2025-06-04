package com.redes.udprip.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a data message in the UDPRIP protocol. Data messages carry a payload from source to
 * destination.
 */
public class DataMessage implements Message {
  @JsonProperty("type")
  private final String type = "data";

  @JsonProperty("source")
  private String source;

  @JsonProperty("destination")
  private String destination;

  @JsonProperty("payload")
  private String payload;

  // Required for Jackson deserialization
  public DataMessage() {}

  public DataMessage(String source, String destination, String payload) {
    this.source = source;
    this.destination = destination;
    this.payload = payload;
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

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }
}
