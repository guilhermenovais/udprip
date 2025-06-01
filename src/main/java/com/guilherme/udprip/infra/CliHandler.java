package com.guilherme.udprip.infra;

import com.guilherme.udprip.app.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * Handles command-line interface input.
 */
public class CliHandler {
    private static final Logger logger = LoggerFactory.getLogger(CliHandler.class);

    private final Router router;

    public CliHandler(Router router) {
        this.router = router;
    }

    /**
     * Start the CLI handler.
     */
    public void start() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("UDPRIP router started. Available commands: add <ip> <weight>, del <ip>, trace <ip>, quit");

            while (true) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) {
                    break;
                }

                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (line.equals("quit")) {
                    System.out.println("Shutting down...");
                    break;
                }

                String[] parts = line.split("\\s+");
                if (parts.length > 0) {
                    try {
                        handleCommand(parts);
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
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
                    System.out.println("Usage: add <ip> <weight>");
                    return;
                }
                String addIp = args[1];
                int weight;
                try {
                    weight = Integer.parseInt(args[2]);
                    if (weight <= 0) {
                        System.out.println("Weight must be positive");
                        return;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Weight must be a valid integer");
                    return;
                }
                router.addNeighbor(addIp, weight);
                System.out.println("Added neighbor " + addIp + " with weight " + weight);
                break;

            case "del":
                if (args.length != 2) {
                    System.out.println("Usage: del <ip>");
                    return;
                }
                String delIp = args[1];
                router.removeNeighbor(delIp);
                System.out.println("Removed neighbor " + delIp);
                break;

            case "trace":
                if (args.length != 2) {
                    System.out.println("Usage: trace <ip>");
                    return;
                }
                String traceIp = args[1];
                router.sendTrace(traceIp);
                System.out.println("Sent trace to " + traceIp);
                break;

            default:
                System.out.println("Unknown command: " + command);
                System.out.println("Available commands: add <ip> <weight>, del <ip>, trace <ip>, quit");
        }
    }
}
