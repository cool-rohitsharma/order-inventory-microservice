#!/bin/bash

# Stop microservices
echo "Stopping Order-Inventory Microservices..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to kill process on port
kill_port() {
    local port=$1
    local service_name=$2
    
    echo -e "${YELLOW}Stopping $service_name on port $port...${NC}"
    
    local pid=$(lsof -ti:$port)
    if [ ! -z "$pid" ]; then
        kill -TERM $pid 2>/dev/null
        sleep 3
        
        # Check if still running, force kill if necessary
        if kill -0 $pid 2>/dev/null; then
            echo -e "${YELLOW}Force killing $service_name...${NC}"
            kill -KILL $pid 2>/dev/null
        fi
        
        echo -e "${GREEN}✓ $service_name stopped${NC}"
    else
        echo -e "${YELLOW}$service_name is not running on port $port${NC}"
    fi
}

# Stop services
kill_port 8081 "Inventory Service"
kill_port 8080 "Order Service"

# Kill any remaining Java processes that might be our services
echo -e "${YELLOW}Cleaning up any remaining service processes...${NC}"

# Kill any maven spring-boot:run processes
pkill -f "spring-boot:run" 2>/dev/null

# Kill any java processes running our services
pkill -f "inventory-service" 2>/dev/null
pkill -f "order-service" 2>/dev/null

echo -e "${GREEN}All services stopped${NC}"

# Clean up log files if they exist
if [ -f "inventory-service.log" ]; then
    echo -e "${YELLOW}Log file inventory-service.log is available for review${NC}"
fi

if [ -f "order-service.log" ]; then
    echo -e "${YELLOW}Log file order-service.log is available for review${NC}"
fi

echo -e "${GREEN}Cleanup complete${NC}"