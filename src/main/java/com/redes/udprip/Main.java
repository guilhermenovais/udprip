package com.redes.udprip;

import com.redes.udprip.app.Router;
import com.redes.udprip.infra.CliHandler;
import com.redes.udprip.infra.UdpClient;
import com.redes.udprip.infra.UdpServer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);
  private static final int UDP_PORT = 55151;

  public static void main(String[] args) {
    try {
      if (args.length < 2 || args.length > 3) {
        System.out.println("Usage: java -jar udprip.jar <address> <period> [startup]");
        System.exit(1);
      }

      String address = args[0];
      int period = Integer.parseInt(args[1]);
      String startupFile = args.length == 3 ? args[2] : null;

      InetAddress localAddress = InetAddress.getByName(address);
      UdpClient udpClient = new UdpClient(UDP_PORT);
      Router router = new Router(localAddress.getHostAddress(), period, udpClient);
      UdpServer udpServer = new UdpServer(localAddress, UDP_PORT, router);
      CliHandler cliHandler = new CliHandler(router);

      Thread serverThread = new Thread(udpServer);
      serverThread.setDaemon(true);
      serverThread.start();
      logger.info("UDP server started on {}:{}", address, UDP_PORT);

      ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
      scheduler.scheduleAtFixedRate(router::sendPeriodicUpdates, period, period, TimeUnit.SECONDS);
      logger.info("Periodic updates scheduled every {} seconds", period);

      if (startupFile != null) {
        processStartupFile(startupFile, router);
      }

      cliHandler.start();

      scheduler.shutdown();
      serverThread.interrupt();
      udpServer.stop();
    } catch (Exception e) {
      logger.error("Error starting the router", e);
      System.exit(1);
    }
  }

  private static void processStartupFile(String filename, Router router) {
    try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }

        String[] parts = line.split("\\s+");
        if (parts.length > 0) {
          switch (parts[0]) {
            case "add":
              if (parts.length == 3) {
                router.addNeighbor(parts[1], Integer.parseInt(parts[2]));
              }
              break;
            case "del":
              if (parts.length == 2) {
                router.removeNeighbor(parts[1]);
              }
              break;
            default:
              logger.warn("Unknown command in startup file: {}", line);
          }
        }
      }
      logger.info("Processed startup file: {}", filename);
    } catch (Exception e) {
      logger.error("Error processing startup file", e);
    }
  }
}
