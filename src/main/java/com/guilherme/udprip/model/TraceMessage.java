package com.guilherme.udprip.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a trace message in the UDPRIP protocol.
 * Trace messages are used to trace the route from source to destination.
 */
public class TraceMessage implements Message {
    @JsonProperty("type")
    private final String type = "trace";

    @JsonProperty("source")
    private String source;

    @JsonProperty("destination")
    private String destination;

    @JsonProperty("routers")
    private List<String> routers;

    // Required for Jackson deserialization
    public TraceMessage() {
        this.routers = new ArrayList<>();
    }

    public TraceMessage(String source, String destination) {
        this.source = source;
        this.destination = destination;
        this.routers = new ArrayList<>();
        this.routers.add(source); // Initialize with source router
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

    public List<String> getRouters() {
        return routers;
    }

    public void setRouters(List<String> routers) {
        this.routers = routers;
    }

    public void addRouter(String router) {
        if (this.routers == null) {
            this.routers = new ArrayList<>();
        }
        this.routers.add(router);
    }
}
