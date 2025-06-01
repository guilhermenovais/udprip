package com.guilherme.udprip.model;

/** Base interface for all message types in the UDPRIP protocol. */
public interface Message {
  /** Get the message type ("data", "update", or "trace"). */
  String getType();

  /** Get the source IP address of the message. */
  String getSource();

  /** Get the destination IP address of the message. */
  String getDestination();
}
