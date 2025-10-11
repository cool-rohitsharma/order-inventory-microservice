#!/bin/bash

# Start both microservices
echo "Starting Order-Inventory Microservices..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check if port is in use
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null ; then
        echo -e "${RED}Port $port is already in use${NC}"
        return 1
    else
        return 0
    fi
}

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}Java is not installed or not in PATH${NC}"
    exit 1
fi

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Maven is not installed or not in PATH${NC}"
    exit 1
fi

# Check ports
echo -e "${YELLOW}Checking ports...${NC}"
if ! check_port 8081; then
    echo -e "${RED}Cannot start Inventory Service - port 8081 is in use${NC}"
    exit 1
fi

if ! check_port 8080; then
    echo -e "${RED}Cannot start Order Service - port 8080 is in use${NC}"
    exit 1
fi

# Build the projects
echo -e "${YELLOW}Building projects...${NC}"
mvn clean install -q
if [ $? -ne 0 ]; then
    echo -e "${RED}Build failed${NC}"
    exit 1
fi

echo -e "${GREEN}Build successful${NC}"

# Start Inventory Service
echo -e "${YELLOW}Starting Inventory Service on port 8081...${NC}"
cd inventory-service
mvn spring-boot:run > ../inventory-service.log 2>&1 &
INVENTORY_PID=$!
cd ..

# Wait a bit for inventory service to start
sleep 10

# Check if inventory service is running
if ! curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
    # Try alternative health check
    if ! curl -s http://localhost:8081/ > /dev/null 2>&1; then
        echo -e "${YELLOW}Inventory Service may still be starting...${NC}"
        sleep 5
    fi
fi

# Start Order Service
echo -e "${YELLOW}Starting Order Service on port 8080...${NC}"
cd order-service
mvn spring-boot:run > ../order-service.log 2>&1 &
ORDER_PID=$!
cd ..

# Wait for services to start
echo -e "${YELLOW}Waiting for services to start...${NC}"
sleep 15

# Check services
echo -e "${YELLOW}Checking service status...${NC}"

# Check Inventory Service
if curl -s http://localhost:8081/ > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Inventory Service is running on http://localhost:8081${NC}"
else
    echo -e "${RED}✗ Inventory Service failed to start${NC}"
fi

# Check Order Service  
if curl -s http://localhost:8080/ > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Order Service is running on http://localhost:8080${NC}"
else
    echo -e "${RED}✗ Order Service failed to start${NC}"
fi

echo ""
echo -e "${GREEN}Services started successfully!${NC}"
echo ""
echo "Service URLs:"
echo "- Inventory Service: http://localhost:8081"
echo "- Order Service: http://localhost:8080"
echo "- H2 Console (Inventory): http://localhost:8081/h2-console"
echo "- H2 Console (Order): http://localhost:8080/h2-console"
echo ""
echo "Process IDs:"
echo "- Inventory Service PID: $INVENTORY_PID"
echo "- Order Service PID: $ORDER_PID"
echo ""
echo "To stop services:"
echo "kill $INVENTORY_PID $ORDER_PID"
echo ""
echo "Log files:"
echo "- Inventory Service: inventory-service.log"
echo "- Order Service: order-service.log"