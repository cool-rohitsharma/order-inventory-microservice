# Order-Inventory Microservices Assignment

## Overview
This project implements two Spring Boot microservices - Order Service and Inventory Service - that communicate via REST APIs. The system uses Factory Design Pattern for extensibility and includes comprehensive testing.

## Architecture
- **Inventory Service** (Port 8081): Manages product inventory with batch tracking and expiry dates
- **Order Service** (Port 8080): Processes orders and communicates with Inventory Service
- **Factory Pattern**: Implemented in Inventory Service for extensible inventory handling logic
- **Database**: H2 in-memory database for both services
- **Communication**: REST APIs using RestTemplate

## Project Structure
```
order-inventory-microservices-assignment/
├── inventory-service/
│   ├── src/main/java/com/microservices/inventory/
│   │   ├── InventoryServiceApplication.java
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   └── factory/
│   ├── src/main/resources/
│   └── src/test/java/
├── order-service/
│   ├── src/main/java/com/microservices/order/
│   │   ├── OrderServiceApplication.java
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   └── config/
│   ├── src/main/resources/
│   └── src/test/java/
└── README.md
```

## Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- Git

## Setup Instructions

### Option 1: Using Maven (Recommended for Development)

#### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/order-inventory-microservices-assignment.git
cd order-inventory-microservices-assignment
```

#### 2. Quick Start with Scripts
```bash
# Make scripts executable (Linux/Mac)
chmod +x scripts/*.sh

# Start both services
./scripts/start-services.sh

# Test APIs
./scripts/test-apis.sh

# Stop services
./scripts/stop-services.sh
```

#### 3. Manual Setup

##### Build Both Services
```bash
# Build all modules from root
mvn clean install

# Or build individually
cd inventory-service && mvn clean install
cd ../order-service && mvn clean install
```

##### Start Services

**Start Inventory Service (Terminal 1)**
```bash
cd inventory-service
mvn spring-boot:run
```
The service will start on `http://localhost:8081`

**Start Order Service (Terminal 2)**
```bash
cd order-service
mvn spring-boot:run
```
The service will start on `http://localhost:8080`

### Option 2: Using Docker (Recommended for Production)

#### 1. Build and Run with Docker Compose
```bash
# Build and start both services
docker-compose up --build

# Run in background
docker-compose up -d --build

# Stop services
docker-compose down
```

#### 2. Check Service Health
```bash
# Check if services are running
docker-compose ps

# View logs
docker-compose logs inventory-service
docker-compose logs order-service
```

## API Documentation

### Inventory Service (Port 8081)

#### GET /inventory/{productId}
Returns list of inventory batches sorted by expiry date for a given product.

**Example:**
```bash
curl -X GET http://localhost:8081/inventory/PROD001
```

**Response:**
```json
[
  {
    "id": 1,
    "productId": "PROD001",
    "batchNumber": "BATCH001",
    "quantity": 100,
    "expiryDate": "2024-12-31",
    "createdDate": "2024-01-15T10:30:00"
  }
]
```

#### POST /inventory/update
Updates inventory after an order is placed.

**Example:**
```bash
curl -X POST http://localhost:8081/inventory/update \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "PROD001",
    "quantity": 10
  }'
```

### Order Service (Port 8080)

#### POST /order
Places an order and updates inventory accordingly.

**Example:**
```bash
curl -X POST http://localhost:8080/order \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "PROD001",
    "quantity": 5,
    "customerEmail": "customer@example.com"
  }'
```

**Response:**
```json
{
  "id": 1,
  "productId": "PROD001",
  "quantity": 5,
  "customerEmail": "customer@example.com",
  "orderDate": "2024-01-15T10:30:00",
  "status": "COMPLETED"
}
```

## Testing

### Automated API Testing
```bash
# Run the API test script (requires services to be running)
./scripts/test-apis.sh
```

### Unit and Integration Tests
```bash
# Test all services from root
mvn test

# Test individual services
cd inventory-service && mvn test
cd order-service && mvn test

# Run all tests including integration tests
mvn verify
```

### Manual Testing with Postman
1. Import `postman_collection.json` into Postman
2. Set environment variables:
   - `inventory_url`: http://localhost:8081
   - `order_url`: http://localhost:8080
3. Run the collection to test all endpoints

## Database Access
Both services use H2 in-memory databases. Access the H2 console:

- **Inventory Service**: http://localhost:8081/h2-console
- **Order Service**: http://localhost:8080/h2-console

**Connection Details:**
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (empty)

## Design Patterns

### Factory Pattern Implementation
The Inventory Service implements the Factory Pattern to allow future extension of inventory handling logic:

- `InventoryHandlerFactory`: Creates appropriate inventory handlers
- `InventoryHandler`: Interface for different inventory handling strategies
- `DefaultInventoryHandler`: Default implementation
- `ExpiryBasedInventoryHandler`: Handles inventory based on expiry dates

## Technologies Used
- Spring Boot 3.2.0
- Spring Data JPA
- Spring Web
- H2 Database
- JUnit 5
- Mockito
- Lombok
- Maven

## Project Files
- `README.md` - This documentation
- `pom.xml` - Parent Maven configuration
- `docker-compose.yml` - Docker orchestration
- `postman_collection.json` - API testing collection
- `scripts/` - Utility scripts for starting/stopping services
- `inventory-service/` - Inventory microservice
- `order-service/` - Order microservice

## Key Features Implemented
- ✅ Spring Boot microservices architecture
- ✅ Factory Design Pattern for inventory handling
- ✅ REST API communication between services
- ✅ H2 in-memory databases with JPA
- ✅ Comprehensive unit and integration tests
- ✅ FEFO (First Expired, First Out) inventory strategy
- ✅ Error handling and validation
- ✅ Docker containerization
- ✅ Automated testing scripts
- ✅ Postman API collection

## Future Enhancements
- Add authentication and authorization (Spring Security)
- Implement distributed tracing (Zipkin/Jaeger)
- Add circuit breaker pattern (Resilience4j)
- Implement event-driven communication (RabbitMQ/Kafka)
- Add API Gateway (Spring Cloud Gateway)
- Add service discovery (Eureka)
- Implement database migrations (Flyway/Liquibase)
- Add monitoring and metrics (Micrometer/Prometheus)

## Contributing
1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request