package com.redes.udprip.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redes.udprip.infra.UdpClient;
import com.redes.udprip.model.DataMessage;
import com.redes.udprip.model.Message;
import com.redes.udprip.model.TraceMessage;
import com.redes.udprip.model.UpdateMessage;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Central component that handles all the routing logic. */
public class Router {
  private static final Logger logger = LoggerFactory.getLogger(Router.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final String localAddress;
  private final DistanceVector distanceVector;
  private final TopologyManager topologyManager;
  private final UdpClient udpClient;

  public Router(String localAddress, int updatePeriod, UdpClient udpClient) {
    this.localAddress = localAddress;
    this.udpClient = udpClient;

    this.distanceVector = new DistanceVector(localAddress);
    this.topologyManager = new TopologyManager(updatePeriod);
  }

  /**
   * Handle an incoming message.
   *
   * @param messageJson The JSON string representing the message
   */
  public void handleMessage(String messageJson) {
    try {
      Map<String, Object> jsonMap = objectMapper.readValue(messageJson, Map.class);
      String type = (String) jsonMap.get("type");

      if (type == null) {
        logger.warn("Received message with no type: {}", messageJson);
        return;
      }

      switch (type) {
        case "data":
          handleDataMessage(objectMapper.readValue(messageJson, DataMessage.class));
          break;
        case "update":
          handleUpdateMessage(objectMapper.readValue(messageJson, UpdateMessage.class));
          break;
        case "trace":
          handleTraceMessage(objectMapper.readValue(messageJson, TraceMessage.class));
          break;
        default:
          logger.warn("Unknown message type: {}", type);
      }
    } catch (Exception e) {
      logger.error("Error handling message: {}", e.getMessage(), e);
    }
  }

  /**
   * Handle a data message.
   *
   * @param message The data message
   */
  private void handleDataMessage(DataMessage message) {
    if (message.getDestination().equals(localAddress)) {
      System.out.println(message.getPayload());
      return;
    }

    forwardMessage(message);
  }

  /**
   * Handle an update message.
   *
   * @param message The update message
   */
  private void handleUpdateMessage(UpdateMessage message) {
    if (!message.getDestination().equals(localAddress)) {
      return;
    }

    String neighborIp = message.getSource();
    Integer linkWeight = topologyManager.getLinkWeight(neighborIp);

    if (linkWeight != null) {
      topologyManager.recordNeighborUpdate(neighborIp);
      distanceVector.applyUpdate(neighborIp, message.getDistances(), linkWeight);
    } else {
      logger.debug("Ignoring update from unknown neighbor: {}", neighborIp);
    }
  }

  /**
   * Handle a trace message.
   *
   * @param message The trace message
   */
  private void handleTraceMessage(TraceMessage message) {
    message.addRouter(localAddress);

    if (message.getDestination().equals(localAddress)) {
      sendTraceResponse(message);
    } else {
      forwardMessage(message);
    }
  }

  /**
   * Send a trace response.
   *
   * @param traceMessage The completed trace message
   */
  private void sendTraceResponse(TraceMessage traceMessage) {
    try {
      String traceJson = objectMapper.writeValueAsString(traceMessage);
      DataMessage response = new DataMessage(localAddress, traceMessage.getSource(), traceJson);

      forwardMessage(response);
    } catch (JsonProcessingException e) {
      logger.error("Error creating trace response: {}", e.getMessage(), e);
    }
  }

  /**
   * Forward a message to the next hop.
   *
   * @param message The message to forward
   */
  private void forwardMessage(Message message) {
    String destination = message.getDestination();
    String nextHop = distanceVector.getNextHop(destination);

    if (nextHop == null) {
      logger.warn("No route to destination: {}", destination);
      return;
    }

    try {
      String messageJson = objectMapper.writeValueAsString(message);
      udpClient.sendMessage(nextHop, messageJson);
    } catch (JsonProcessingException e) {
      logger.error("Error serializing message: {}", e.getMessage(), e);
    }
  }

  /** Send periodic updates to all neighbors. */
  public void sendPeriodicUpdates() {
    List<String> staleNeighbors = topologyManager.findStaleNeighbors();
    distanceVector.removeRoutesForStaleNeighbors(staleNeighbors);

    for (String neighborIp : topologyManager.getAllNeighbors()) {
      sendUpdateToNeighbor(neighborIp);
    }
  }

  /**
   * Send an update to a specific neighbor.
   *
   * @param neighborIp The neighbor's IP address
   */
  private void sendUpdateToNeighbor(String neighborIp) {
    Map<String, Integer> distances = distanceVector.getDistancesForNeighbor(neighborIp);

    UpdateMessage updateMessage = new UpdateMessage(localAddress, neighborIp, distances);
    try {
      String updateJson = objectMapper.writeValueAsString(updateMessage);
      udpClient.sendMessage(neighborIp, updateJson);
    } catch (JsonProcessingException e) {
      logger.error("Error serializing update message: {}", e.getMessage(), e);
    }
  }

  /**
   * Add a neighbor.
   *
   * @param neighborIp The neighbor's IP address
   * @param weight The link weight
   */
  public void addNeighbor(String neighborIp, int weight) {
    if (topologyManager.addNeighbor(neighborIp, weight)) {
      sendUpdateToNeighbor(neighborIp);
    }
  }

  /**
   * Remove a neighbor.
   *
   * @param neighborIp The neighbor's IP address
   */
  public void removeNeighbor(String neighborIp) {
    if (topologyManager.removeNeighbor(neighborIp)) {
      distanceVector.removeRoutesVia(neighborIp);
    }
  }

  /**
   * Send a trace message to a destination.
   *
   * @param destinationIp The destination IP address
   */
  public void sendTrace(String destinationIp) {
    if (!distanceVector.hasRoute(destinationIp)) {
      logger.warn("No route to destination: {}", destinationIp);
      return;
    }

    TraceMessage traceMessage = new TraceMessage(localAddress, destinationIp);
    forwardMessage(traceMessage);
  }
}
