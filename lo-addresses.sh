#!/bin/bash

# Check if running as root
if [ "$(id -u)" -ne 0 ]; then
    echo "This script must be run as root" 1>&2
    exit 1
 fi

# Detect operating system
os=$(uname)

# Function to add IP addresses
add_addresses() {
    if [ "$os" == "Linux" ]; then
        for i in $(seq 1 16); do
            ip addr add 127.0.1.$i/32 dev lo
        done
        echo "Added IP addresses 127.0.1.1 through 127.0.1.16 to lo interface"
    elif [ "$os" == "Darwin" ]; then
        for i in $(seq 1 16); do
            ifconfig lo0 alias 127.0.1.$i/32
        done
        echo "Added IP addresses 127.0.1.1 through 127.0.1.16 to lo0 interface"
    else
        echo "Unsupported operating system: $os"
        exit 1
    fi
}

# Function to remove IP addresses
remove_addresses() {
    if [ "$os" == "Linux" ]; then
        for i in $(seq 1 16); do
            ip addr del 127.0.1.$i/32 dev lo 2>/dev/null
        done
        echo "Removed IP addresses 127.0.1.1 through 127.0.1.16 from lo interface"
    elif [ "$os" == "Darwin" ]; then
        for i in $(seq 1 16); do
            ifconfig lo0 -alias 127.0.1.$i 2>/dev/null
        done
        echo "Removed IP addresses 127.0.1.1 through 127.0.1.16 from lo0 interface"
    else
        echo "Unsupported operating system: $os"
        exit 1
    fi
}

# Process arguments
if [ "$#" -lt 1 ]; then
    echo "Usage: $0 [add|remove]"
    exit 1
fi

case "$1" in
    "add")
        add_addresses
        ;;
    "remove")
        remove_addresses
        ;;
    *)
        echo "Unknown option: $1. Use 'add' or 'remove'"
        exit 1
        ;;
esac

exit 0
