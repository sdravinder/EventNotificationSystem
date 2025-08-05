# Event Notification System

A Java-based Event Notification System built with Spring Boot that processes EMAIL, SMS, and PUSH notifications asynchronously with FIFO ordering and configurable processing delays.

## Features

### Phase 1: Project Foundation ✅
- **Spring Boot Application**: RESTful API with comprehensive configuration
- **Domain Models**: Event hierarchy with EmailEvent, SmsEvent, and PushEvent
- **DTOs**: Request/Response objects for API communication
- **Configuration**: Type-safe configuration properties with validation
- **Event Factory**: Factory pattern for creating events from requests

### Phase 2: Queue & Processing Infrastructure ✅
- **EventQueueManager**: Thread-safe queue management with separate queues per event type
  - FIFO processing guarantee using LinkedBlockingQueue
  - Configurable queue capacities
  - JMX monitoring for queue sizes and metrics
  
- **Event Processors**: Abstract base class with concrete implementations
  - **EmailEventProcessor**: 5-second processing delay
  - **SmsEventProcessor**: 3-second processing delay  
  - **PushEventProcessor**: 2-second processing delay
  - Random failure simulation (10% configurable rate)
  - Template Method pattern for common processing logic
  - Strategy pattern for event-type-specific processing

- **Processing Features**:
  - Thread-safe event processing
  - Random failure simulation (10% default rate)
  - Failed events marked as FAILED and proceed (no retries)
  - JMX metrics for processing rates and failure tracking
  - Comprehensive error handling and logging

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

Comprehensive unit test coverage including:
- **EventQueueManager**: Queue operations, FIFO ordering, capacity limits
- **EventProcessor**: Template method pattern, failure simulation, metrics
- **Concrete Processors**: Event-specific validation and processing logic
- **Thread Safety**: Concurrent access and processing verification

Run tests:
```bash
mvn test
```

## Usage Example

### Email Event
```json
{
  "eventType": "EMAIL",
  "payload": {
    "recipient": "user@example.com",
    "message": "Welcome to our service!",
    "subject": "Welcome"
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
    "message": "Your verification code is 123456",
    "senderId": "MyApp"
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
    "message": "You have a new message",
    "title": "New Message",
    "badgeCount": 1
  },
  "callbackUrl": "https://your-app.com/webhook"
}
```

## Logging

Structured logging with different levels:
- **INFO**: Processing start/completion, queue status
- **DEBUG**: Detailed event processing information
- **WARN**: Failed queue operations, capacity issues
- **ERROR**: Processing failures, system errors

## Technology Stack

- **Java 17**: Modern Java features and performance
- **Spring Boot 3.1.5**: Auto-configuration and dependency injection
- **Maven**: Build tool and dependency management
- **JUnit 5 + Mockito**: Testing framework
- **Jackson**: JSON processing
- **Lombok**: Reduce boilerplate code
- **JMX**: Monitoring and metrics

## What's Next

### Phase 3: REST API & Callback (Planned)
- REST controller for event submission
- Callback service for status notifications
- Input validation and error handling

### Phase 4: Concurrency & Lifecycle (Planned)
- Application startup/shutdown hooks
- Processor thread lifecycle management
- Graceful shutdown implementation

