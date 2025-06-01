package com.guilherme.udprip.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * UDP client for sending messages to other routers.
 */
public class UdpClient {
    private static final Logger logger = LoggerFactory.getLogger(UdpClient.class);

    private final int port;

    public UdpClient(int port) {
        this.port = port;
    }

    /**
     * Send a message to a destination IP address.
     * 
     * @param destinationIp The destination IP address
     * @param message The message to send
     */
    public void sendMessage(String destinationIp, String message) {
        try (DatagramSocket socket = new DatagramSocket()) {
            // Convert message to bytes
            byte[] data = message.getBytes(StandardCharsets.UTF_8);

            // Create packet
            InetAddress address = InetAddress.getByName(destinationIp);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);

            // Send packet
            socket.send(packet);
            logger.debug("Sent {} bytes to {}", data.length, destinationIp);
        } catch (IOException e) {
            logger.error("Error sending message to {}: {}", destinationIp, e.getMessage(), e);
        }
    }
}
