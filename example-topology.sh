#!/bin/bash

# This script starts the example topology with 5 routers

# Start router 127.0.1.5 (central router)
cat > router5-setup.txt << EOF
add 127.0.1.1 10
add 127.0.1.2 10
add 127.0.1.3 10
add 127.0.1.4 10
EOF

# Start router 127.0.1.1 through 127.0.1.4 (peripheral routers)
cat > router-periphery-setup.txt << EOF
add 127.0.1.5 10
EOF

# Start all routers in separate terminals
echo "Starting all routers..."

# Use appropriate terminal command based on OS
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    open -a Terminal.app "$(pwd)/router.sh 127.0.1.5 5 router5-setup.txt"
    sleep 1
    open -a Terminal.app "$(pwd)/router.sh 127.0.1.1 5 router-periphery-setup.txt"
    sleep 1
    open -a Terminal.app "$(pwd)/router.sh 127.0.1.2 5 router-periphery-setup.txt"
    sleep 1
    open -a Terminal.app "$(pwd)/router.sh 127.0.1.3 5 router-periphery-setup.txt"
    sleep 1
    open -a Terminal.app "$(pwd)/router.sh 127.0.1.4 5 router-periphery-setup.txt"
else
    # Linux - use gnome-terminal if available, otherwise use xterm
    if command -v gnome-terminal &> /dev/null; then
        gnome-terminal -- bash -c "$(pwd)/router.sh 127.0.1.5 5 router5-setup.txt; exec bash"
        sleep 1
        gnome-terminal -- bash -c "$(pwd)/router.sh 127.0.1.1 5 router-periphery-setup.txt; exec bash"
        sleep 1
        gnome-terminal -- bash -c "$(pwd)/router.sh 127.0.1.2 5 router-periphery-setup.txt; exec bash"
        sleep 1
        gnome-terminal -- bash -c "$(pwd)/router.sh 127.0.1.3 5 router-periphery-setup.txt; exec bash"
        sleep 1
        gnome-terminal -- bash -c "$(pwd)/router.sh 127.0.1.4 5 router-periphery-setup.txt; exec bash"
    elif command -v xterm &> /dev/null; then
        xterm -e "$(pwd)/router.sh 127.0.1.5 5 router5-setup.txt" &
        sleep 1
        xterm -e "$(pwd)/router.sh 127.0.1.1 5 router-periphery-setup.txt" &
        sleep 1
        xterm -e "$(pwd)/router.sh 127.0.1.2 5 router-periphery-setup.txt" &
        sleep 1
        xterm -e "$(pwd)/router.sh 127.0.1.3 5 router-periphery-setup.txt" &
        sleep 1
        xterm -e "$(pwd)/router.sh 127.0.1.4 5 router-periphery-setup.txt" &
    else
        echo "Could not find a suitable terminal emulator. Please start routers manually."
        exit 1
    fi
fi

echo "All routers started. You can now use the 'trace' command to test routing."
