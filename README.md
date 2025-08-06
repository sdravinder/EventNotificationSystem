# Event Notification System

A Java-based Event Notification System built with Spring Boot that processes EMAIL, SMS, and PUSH notifications asynchronously with FIFO ordering, configurable processing delays, REST API endpoints, and callback notifications.

## Architecture

### SOLID Principles Implementation
- **Single Responsibility**: Each class has one reason to change
  - `EventQueueManager`: Only manages queues
  - `EventProcessor`: Only processes events
  - `EmailEventProcessor`: Only handles email-specific logic
  
- **Open/Closed**: Open for extension, closed for modification
  - New event types can be added by extending `EventProcessor`
  - New processors registered via `EventProcessorFactory`
  
- **Liskov Substitution**: All processors can substitute the base `EventProcessor`
- **Interface Segregation**: Interfaces separated by concern
- **Dependency Inversion**: Depends on abstractions, not concretions

### Design Patterns Used
- **Template Method**: `EventProcessor` defines processing algorithm structure
- **Strategy Pattern**: Different processing strategies per event type
- **Factory Pattern**: `EventFactory` and `EventProcessorFactory`
- **Builder Pattern**: Event creation and configuration

## Configuration

### Processing Delays
```properties
# Processing delays per event type
event.processing.email.delay-seconds=5
event.processing.sms.delay-seconds=3
event.processing.push.delay-seconds=2
event.processing.failure-rate-percent=10
```

### Queue Configuration
```properties
# Queue capacities
event.queue.email.capacity=1000
event.queue.sms.capacity=1000
event.queue.push.capacity=1000
```

### Callback Configuration
```properties
# Callback retry and timeout settings
callback.retry.maxAttempts=3
callback.retry.delayMs=1000
callback.timeout.connectionMs=5000
callback.timeout.readMs=10000
```

### JMX Monitoring
The system exposes JMX metrics for monitoring:
- Queue sizes per event type
- Processing rates and counts
- Success/failure rates
- Thread status

Access via JConsole or other JMX tools at:
- `com.eventnotification:type=EventQueueManager`
- `com.eventnotification:type=EmailEventProcessor`
- `com.eventnotification:type=SmsEventProcessor`
- `com.eventnotification:type=PushEventProcessor`

## Event Processing Rules

### Queue Management
- Each event type has a **separate queue** (EMAIL, SMS, PUSH)
- Each event type has a **separate processing thread**
- Events processed in **FIFO order** within each queue
- Configurable queue capacities with overflow handling

### Processing Characteristics
- **EMAIL**: 5 seconds processing time per event
- **SMS**: 3 seconds processing time per event  
- **PUSH**: 2 seconds processing time per event
- **Random Failures**: 10% of events fail randomly (configurable)
- **Failed Event Handling**: Mark as FAILED and proceed (no retries)

### Thread Safety
- Thread-safe queue operations using `LinkedBlockingQueue`
- Atomic counters for metrics
- Proper thread lifecycle management

## Testing

Run tests:
```bash
mvn test
```

## API Documentation

### POST /api/events
Submit a new event for processing.

**Request Body:**
```json
{
  "eventType": "EMAIL|SMS|PUSH",
  "payload": {
    // Event-specific payload (see examples below)
  },
  "callbackUrl": "https://your-app.com/webhook"
}
```

**Response (201 Created):**
```json
{
  "eventId": "evt_a1b2c3d4e5f6",
  "message": "Event accepted for processing."
}
```

**Error Response (400 Bad Request):**
```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid request data",
  "validationErrors": [
    "Event type is required",
    "Callback URL must be a valid HTTP/HTTPS URL"
  ],
  "timestamp": "2025-08-07T10:30:00",
  "path": "/api/events"
}
```

## Usage Example

### Email Event
```json
{
  "eventType": "EMAIL",
  "payload": {
    "recipient": "user@example.com",
    "message": "Welcome to our service!"
  },
  "callbackUrl": "https://your-app.com/webhook"
}
```

### SMS Event
```json
{
  "eventType": "SMS", 
  "payload": {
    "phoneNumber": "+1234567890",
    "message": "Your verification code is 123456"
  },
  "callbackUrl": "https://your-app.com/webhook"
}
```

### Push Event
```json
{
  "eventType": "PUSH",
  "payload": {
    "deviceId": "device-token-123",
    "message": "You have a new message"
  },
  "callbackUrl": "https://your-app.com/webhook"
}
```

## Callback Notifications

When event processing completes (successfully or fails), the system sends a callback notification to the provided `callbackUrl`.

### Successful Callback
```json
{
  "eventId": "evt_a1b2c3d4e5f6",
  "eventType": "EMAIL",
  "status": "COMPLETED",
  "processedAt": "2025-08-07T10:35:24"
}
```

### Failed Callback
```json
{
  "eventId": "evt_a1b2c3d4e5f6",
  "eventType": "EMAIL", 
  "status": "FAILED",
  "processedAt": "2025-08-07T10:35:24",
  "errorMessage": "Invalid email format: invalid-email"
}
```

### Callback Features
- **Retry Mechanism**: Up to 3 attempts with exponential backoff (1s, 2s, 3s delays)
- **Asynchronous Processing**: Non-blocking callback execution
- **Error Handling**: Comprehensive handling of network, client, and server errors
- **Timeout Configuration**: Configurable connection and read timeouts
- **Structured Payloads**: Consistent callback request format

## Logging

Structured logging with different levels:
- **INFO**: Processing start/completion, queue status
- **DEBUG**: Detailed event processing information
- **WARN**: Failed queue operations, capacity issues
- **ERROR**: Processing failures, system errors

## Technology Stack

- **Java 17**: Modern Java features and performance
- **Spring Boot 3.1.5**: Auto-configuration and dependency injection  
- **Spring Web**: RESTful API endpoints and HTTP client support
- **Spring Validation**: Bean validation with comprehensive error handling
- **Maven**: Build tool and dependency management
- **JUnit 5 + Mockito**: Testing framework with comprehensive test coverage
- **Jackson**: JSON processing for API requests/responses and callbacks
- **Lombok**: Reduce boilerplate code and improve readability
- **JMX**: Monitoring and metrics for queue and processor health
- **RestTemplate**: HTTP client for callback notifications

