package com.guilherme.udprip.infra;

import com.guilherme.udprip.app.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/**
 * UDP server that listens for incoming packets and forwards them to the router.
 */
public class UdpServer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(UdpServer.class);
    private static final int BUFFER_SIZE = 8192;

    private final InetAddress bindAddress;
    private final int port;
    private final Router router;
    private DatagramSocket socket;
    private volatile boolean running = true;

    public UdpServer(InetAddress bindAddress, int port, Router router) {
        this.bindAddress = bindAddress;
        this.port = port;
        this.router = router;
    }

    @Override
    public void run() {
        try {
            // Create and bind the socket
            socket = new DatagramSocket(port, bindAddress);
            byte[] buffer = new byte[BUFFER_SIZE];

            logger.info("UDP server listening on {}:{}", bindAddress.getHostAddress(), port);

            while (running) {
                try {
                    // Receive packet
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    // Extract message
                    String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                    // Log source IP and message size
                    logger.debug("Received {} bytes from {}", packet.getLength(), packet.getAddress().getHostAddress());

                    // Forward to router
                    router.handleMessage(message);
                } catch (IOException e) {
                    if (running) {
                        logger.error("Error receiving packet: {}", e.getMessage(), e);
                    }
                }
            }
        } catch (SocketException e) {
            logger.error("Error creating UDP socket: {}", e.getMessage(), e);
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    /**
     * Stop the server.
     */
    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
