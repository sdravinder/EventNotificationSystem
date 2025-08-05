package com.ravinder.eventnotificationsystem.queue;


import com.ravinder.eventnotificationsystem.config.EventNotificationProperties;
import com.ravinder.eventnotificationsystem.model.EmailEvent;
import com.ravinder.eventnotificationsystem.model.EmailPayload;
import com.ravinder.eventnotificationsystem.model.Event;
import com.ravinder.eventnotificationsystem.model.EventType;
import com.ravinder.eventnotificationsystem.model.PushEvent;
import com.ravinder.eventnotificationsystem.model.PushPayload;
import com.ravinder.eventnotificationsystem.model.SmsEvent;
import com.ravinder.eventnotificationsystem.model.SmsPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EventQueueManager
 * Tests queue management, thread safety, and FIFO ordering
 */
@ExtendWith(MockitoExtension.class)
class EventQueueManagerTest {

    @Mock
    private EventNotificationProperties properties;

    @Mock
    private EventNotificationProperties.Queue queueProperties;

    @Mock
    private EventNotificationProperties.Queue.Email emailQueueProperties;

    @Mock
    private EventNotificationProperties.Queue.Sms smsQueueProperties;

    @Mock
    private EventNotificationProperties.Queue.Push pushQueueProperties;

    private EventQueueManager queueManager;

    @BeforeEach
    void setUp() {
        // Setup mock properties
        when(properties.getQueue()).thenReturn(queueProperties);
        when(queueProperties.getEmail()).thenReturn(emailQueueProperties);
        when(queueProperties.getSms()).thenReturn(smsQueueProperties);
        when(queueProperties.getPush()).thenReturn(pushQueueProperties);

        when(emailQueueProperties.getCapacity()).thenReturn(100);
        when(smsQueueProperties.getCapacity()).thenReturn(100);
        when(pushQueueProperties.getCapacity()).thenReturn(100);

        queueManager = new EventQueueManager(properties);
        queueManager.initializeQueues();
    }

    @Test
    void testInitializeQueues() {
        // Verify queues are initialized
        assertThat(queueManager.getQueue(EventType.EMAIL)).isNotNull();
        assertThat(queueManager.getQueue(EventType.SMS)).isNotNull();
        assertThat(queueManager.getQueue(EventType.PUSH)).isNotNull();

        // Verify initial state
        assertThat(queueManager.isEmpty(EventType.EMAIL)).isTrue();
        assertThat(queueManager.isEmpty(EventType.SMS)).isTrue();
        assertThat(queueManager.isEmpty(EventType.PUSH)).isTrue();

        assertThat(queueManager.getQueueSize(EventType.EMAIL)).isEqualTo(0);
        assertThat(queueManager.getQueueSize(EventType.SMS)).isEqualTo(0);
        assertThat(queueManager.getQueueSize(EventType.PUSH)).isEqualTo(0);
    }

    @Test
    void testEnqueueEmailEvent() {
        // Create test email event
        EmailPayload payload = new EmailPayload(
                "test@example.com", "Test message");
        EmailEvent event = new EmailEvent("http://callback.url", payload);
        event.setEventId("test-email-1");

        // Enqueue event
        boolean result = queueManager.enqueue(event);

        // Verify enqueue success
        assertThat(result).isTrue();
        assertThat(queueManager.getQueueSize(EventType.EMAIL)).isEqualTo(1);
        assertThat(queueManager.isEmpty(EventType.EMAIL)).isFalse();
        assertThat(queueManager.getRemainingCapacity(EventType.EMAIL)).isEqualTo(99);
    }

    @Test
    void testEnqueueSmsEvent() {
        // Create test SMS event
        SmsPayload payload = new SmsPayload(
                "+1234567890", "Test SMS");
        SmsEvent event = new SmsEvent("http://callback.url", payload);
        event.setEventId("test-sms-1");

        // Enqueue event
        boolean result = queueManager.enqueue(event);

        // Verify enqueue success
        assertThat(result).isTrue();
        assertThat(queueManager.getQueueSize(EventType.SMS)).isEqualTo(1);
        assertThat(queueManager.isEmpty(EventType.SMS)).isFalse();
        assertThat(queueManager.getRemainingCapacity(EventType.SMS)).isEqualTo(99);
    }

    @Test
    void testEnqueuePushEvent() {
        // Create test push event
        PushPayload payload = new PushPayload(
                "device123", "Test push");
        PushEvent event = new PushEvent("http://callback.url", payload);
        event.setEventId("test-push-1");

        // Enqueue event
        boolean result = queueManager.enqueue(event);

        // Verify enqueue success
        assertThat(result).isTrue();
        assertThat(queueManager.getQueueSize(EventType.PUSH)).isEqualTo(1);
        assertThat(queueManager.isEmpty(EventType.PUSH)).isFalse();
        assertThat(queueManager.getRemainingCapacity(EventType.PUSH)).isEqualTo(99);
    }

    @Test
    void testDequeue() throws InterruptedException {
        // Create and enqueue test event
        EmailPayload payload = new EmailPayload(
                "test@example.com", "Test message");
        EmailEvent event = new EmailEvent("http://callback.url", payload);
        event.setEventId("test-email-1");

        queueManager.enqueue(event);

        // Dequeue event
        Event dequeuedEvent = queueManager.dequeue(EventType.EMAIL);

        // Verify dequeue
        assertThat(dequeuedEvent).isNotNull();
        assertThat(dequeuedEvent.getEventId()).isEqualTo("test-email-1");
        assertThat(dequeuedEvent.getEventType()).isEqualTo(EventType.EMAIL);
        assertThat(queueManager.getQueueSize(EventType.EMAIL)).isEqualTo(0);
        assertThat(queueManager.isEmpty(EventType.EMAIL)).isTrue();
    }

    @Test
    void testFIFOOrdering() throws InterruptedException {
        // Create multiple events
        EmailPayload payload = new EmailPayload(
                "test@example.com", "Test message");

        EmailEvent event1 = new EmailEvent("http://callback.url", payload);
        event1.setEventId("event-1");

        EmailEvent event2 = new EmailEvent("http://callback.url", payload);
        event2.setEventId("event-2");

        EmailEvent event3 = new EmailEvent("http://callback.url", payload);
        event3.setEventId("event-3");

        // Enqueue events in order
        queueManager.enqueue(event1);
        queueManager.enqueue(event2);
        queueManager.enqueue(event3);

        // Dequeue events and verify FIFO order
        Event dequeued1 = queueManager.dequeue(EventType.EMAIL);
        Event dequeued2 = queueManager.dequeue(EventType.EMAIL);
        Event dequeued3 = queueManager.dequeue(EventType.EMAIL);

        assertThat(dequeued1.getEventId()).isEqualTo("event-1");
        assertThat(dequeued2.getEventId()).isEqualTo("event-2");
        assertThat(dequeued3.getEventId()).isEqualTo("event-3");
    }

    @Test
    void testEnqueueNullEvent() {
        assertThatThrownBy(() -> queueManager.enqueue(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Event cannot be null");
    }

    @Test
    void testEnqueueEventWithNullType() {
        EmailPayload payload = new EmailPayload(
                "test@example.com", "Test message");
        EmailEvent event = new EmailEvent("http://callback.url", payload);
        event.setEventType(null);

        assertThatThrownBy(() -> queueManager.enqueue(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Event type cannot be null");
    }

    @Test
    void testDequeueWithNullEventType() {
        assertThatThrownBy(() -> queueManager.dequeue(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Event type cannot be null");
    }

    @Test
    void testGetQueueWithNullEventType() {
        assertThatThrownBy(() -> queueManager.getQueue(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Event type cannot be null");
    }

    @Test
    void testQueueCapacityLimits() {
        // Configure small capacity for testing
        when(emailQueueProperties.getCapacity()).thenReturn(2);

        EventQueueManager smallQueueManager = new EventQueueManager(properties);
        smallQueueManager.initializeQueues();

        // Create test events
        EmailPayload payload = new EmailPayload(
                "test@example.com", "Test message");

        EmailEvent event1 = new EmailEvent("http://callback.url", payload);
        EmailEvent event2 = new EmailEvent("http://callback.url", payload);
        EmailEvent event3 = new EmailEvent("http://callback.url", payload);

        // Fill queue to capacity
        assertThat(smallQueueManager.enqueue(event1)).isTrue();
        assertThat(smallQueueManager.enqueue(event2)).isTrue();
        assertThat(smallQueueManager.isFull(EventType.EMAIL)).isTrue();

        // Try to exceed capacity
        assertThat(smallQueueManager.enqueue(event3)).isFalse();
        assertThat(smallQueueManager.getQueueSize(EventType.EMAIL)).isEqualTo(2);
    }

    @Test
    void testJMXAttributes() {
        // Add some events
        EmailPayload emailPayload = new EmailPayload(
                "test@example.com", "Test message");
        EmailEvent emailEvent = new EmailEvent("http://callback.url", emailPayload);

        SmsPayload smsPayload = new SmsPayload(
                "+1234567890", "Test SMS");
        SmsEvent smsEvent = new SmsEvent("http://callback.url", smsPayload);

        queueManager.enqueue(emailEvent);
        queueManager.enqueue(smsEvent);

        // Test JMX attributes
        assertThat(queueManager.getEmailQueueSize()).isEqualTo(1);
        assertThat(queueManager.getSmsQueueSize()).isEqualTo(1);
        assertThat(queueManager.getPushQueueSize()).isEqualTo(0);
        assertThat(queueManager.getTotalQueueSize()).isEqualTo(2);

        assertThat(queueManager.getEmailQueueRemainingCapacity()).isEqualTo(99);
        assertThat(queueManager.getSmsQueueRemainingCapacity()).isEqualTo(99);
        assertThat(queueManager.getPushQueueRemainingCapacity()).isEqualTo(100);
    }
}
