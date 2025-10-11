#!/bin/bash

# Test API endpoints
echo "Testing Order-Inventory Microservices APIs..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Base URLs
INVENTORY_URL="http://localhost:8081"
ORDER_URL="http://localhost:8080"

echo -e "${BLUE}=== API Testing Script ===${NC}"
echo ""

# Check if services are running
echo -e "${YELLOW}Checking if services are running...${NC}"

if ! curl -s $INVENTORY_URL > /dev/null 2>&1; then
    echo -e "${RED}✗ Inventory Service is not running on $INVENTORY_URL${NC}"
    exit 1
fi

if ! curl -s $ORDER_URL > /dev/null 2>&1; then
    echo -e "${RED}✗ Order Service is not running on $ORDER_URL${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Both services are running${NC}"
echo ""

# Test 1: Get inventory for existing product
echo -e "${BLUE}Test 1: Get inventory for product PROD001${NC}"
response=$(curl -s -w "HTTP_STATUS:%{http_code}" $INVENTORY_URL/inventory/PROD001)
http_code=$(echo $response | grep -o "HTTP_STATUS:[0-9]*" | cut -d: -f2)
body=$(echo $response | sed 's/HTTP_STATUS:[0-9]*//g')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✓ Success: Got inventory data${NC}"
    echo "$body" | jq . 2>/dev/null || echo "$body"
else
    echo -e "${RED}✗ Failed: HTTP $http_code${NC}"
    echo "$body"
fi
echo ""

# Test 2: Check inventory availability
echo -e "${BLUE}Test 2: Check inventory availability for PROD001 (quantity: 50)${NC}"
response=$(curl -s -w "HTTP_STATUS:%{http_code}" "$INVENTORY_URL/inventory/PROD001/availability?quantity=50")
http_code=$(echo $response | grep -o "HTTP_STATUS:[0-9]*" | cut -d: -f2)
body=$(echo $response | sed 's/HTTP_STATUS:[0-9]*//g')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✓ Success: Availability check completed${NC}"
    echo "$body" | jq . 2>/dev/null || echo "$body"
else
    echo -e "${RED}✗ Failed: HTTP $http_code${NC}"
    echo "$body"
fi
echo ""

# Test 3: Place a successful order
echo -e "${BLUE}Test 3: Place an order for PROD001 (quantity: 10)${NC}"
order_payload='{
    "productId": "PROD001",
    "quantity": 10,
    "customerEmail": "test@example.com"
}'

response=$(curl -s -w "HTTP_STATUS:%{http_code}" -X POST \
    -H "Content-Type: application/json" \
    -d "$order_payload" \
    $ORDER_URL/order)

http_code=$(echo $response | grep -o "HTTP_STATUS:[0-9]*" | cut -d: -f2)
body=$(echo $response | sed 's/HTTP_STATUS:[0-9]*//g')

if [ "$http_code" = "201" ]; then
    echo -e "${GREEN}✓ Success: Order placed successfully${NC}"
    echo "$body" | jq . 2>/dev/null || echo "$body"
    # Extract order ID for later use
    order_id=$(echo "$body" | jq -r '.id' 2>/dev/null)
elif [ "$http_code" = "409" ]; then
    echo -e "${YELLOW}⚠ Order failed due to business rules (this is expected behavior)${NC}"
    echo "$body" | jq . 2>/dev/null || echo "$body"
else
    echo -e "${RED}✗ Failed: HTTP $http_code${NC}"
    echo "$body"
fi
echo ""

# Test 4: Get all orders
echo -e "${BLUE}Test 4: Get all orders${NC}"
response=$(curl -s -w "HTTP_STATUS:%{http_code}" $ORDER_URL/order)
http_code=$(echo $response | grep -o "HTTP_STATUS:[0-9]*" | cut -d: -f2)
body=$(echo $response | sed 's/HTTP_STATUS:[0-9]*//g')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✓ Success: Retrieved all orders${NC}"
    echo "$body" | jq . 2>/dev/null || echo "$body"
else
    echo -e "${RED}✗ Failed: HTTP $http_code${NC}"
    echo "$body"
fi
echo ""

# Test 5: Update inventory directly
echo -e "${BLUE}Test 5: Update inventory for PROD001 (reduce by 5)${NC}"
inventory_payload='{
    "productId": "PROD001",
    "quantity": 5
}'

response=$(curl -s -w "HTTP_STATUS:%{http_code}" -X POST \
    -H "Content-Type: application/json" \
    -d "$inventory_payload" \
    $INVENTORY_URL/inventory/update)

http_code=$(echo $response | grep -o "HTTP_STATUS:%{http_code}" | cut -d: -f2)
body=$(echo $response | sed 's/HTTP_STATUS:[0-9]*//g')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✓ Success: Inventory updated${NC}"
    echo "$body" | jq . 2>/dev/null || echo "$body"
elif [ "$http_code" = "409" ]; then
    echo -e "${YELLOW}⚠ Inventory update failed due to insufficient stock${NC}"
    echo "$body" | jq . 2>/dev/null || echo "$body"
else
    echo -e "${RED}✗ Failed: HTTP $http_code${NC}"
    echo "$body"
fi
echo ""

# Test 6: Try to place order with insufficient inventory
echo -e "${BLUE}Test 6: Try to place order with large quantity (should fail)${NC}"
large_order_payload='{
    "productId": "PROD001",
    "quantity": 1000,
    "customerEmail": "test2@example.com"
}'

response=$(curl -s -w "HTTP_STATUS:%{http_code}" -X POST \
    -H "Content-Type: application/json" \
    -d "$large_order_payload" \
    $ORDER_URL/order)

http_code=$(echo $response | grep -o "HTTP_STATUS:[0-9]*" | cut -d: -f2)
body=$(echo $response | sed 's/HTTP_STATUS:[0-9]*//g')

if [ "$http_code" = "409" ]; then
    echo -e "${GREEN}✓ Success: Order correctly failed due to insufficient inventory${NC}"
    echo "$body" | jq . 2>/dev/null || echo "$body"
elif [ "$http_code" = "201" ]; then
    echo -e "${YELLOW}⚠ Unexpected: Order succeeded when it should have failed${NC}"
    echo "$body" | jq . 2>/dev/null || echo "$body"
else
    echo -e "${RED}✗ Failed: HTTP $http_code${NC}"
    echo "$body"
fi
echo ""

echo -e "${BLUE}=== API Testing Complete ===${NC}"
echo ""
echo -e "${YELLOW}Note: If you see 'command not found: jq', install jq for better JSON formatting:${NC}"
echo -e "${YELLOW}  - Ubuntu/Debian: sudo apt-get install jq${NC}"
echo -e "${YELLOW}  - macOS: brew install jq${NC}"
echo -e "${YELLOW}  - Or use the raw JSON output above${NC}"