package com.guilherme.udprip.infra;

import com.guilherme.udprip.app.Router;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Handles command-line interface input. */
public class CliHandler {
  private static final Logger logger = LoggerFactory.getLogger(CliHandler.class);

  private final Router router;

  public CliHandler(Router router) {
    this.router = router;
  }

  /** Start the CLI handler. */
  public void start() {
    try (Scanner scanner = new Scanner(System.in)) {
      while (true) {
        if (!scanner.hasNextLine()) {
          break;
        }

        String line = scanner.nextLine().trim();
        if (line.isEmpty()) {
          continue;
        }

        if (line.equals("quit")) {
          break;
        }

        String[] parts = line.split("\\s+");
        if (parts.length > 0) {
          try {
            handleCommand(parts);
          } catch (Exception e) {
            logger.error("Error executing command: {}", e.getMessage(), e);
          }
        }
      }
    }
  }

  /**
   * Handle a command from the CLI.
   *
   * @param args The command arguments
   */
  private void handleCommand(String[] args) {
    String command = args[0].toLowerCase();

    switch (command) {
      case "add":
        if (args.length != 3) {
          return;
        }
        String addIp = args[1];
        int weight;
        try {
          weight = Integer.parseInt(args[2]);
          if (weight <= 0) {
            return;
          }
        } catch (NumberFormatException e) {
          return;
        }
        router.addNeighbor(addIp, weight);
        break;

      case "del":
        if (args.length != 2) {
          return;
        }
        String delIp = args[1];
        router.removeNeighbor(delIp);
        break;

      case "trace":
        if (args.length != 2) {
          return;
        }
        String traceIp = args[1];
        router.sendTrace(traceIp);
        break;

      default:
    }
  }
}
