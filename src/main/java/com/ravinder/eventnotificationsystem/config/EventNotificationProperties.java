package com.ravinder.eventnotificationsystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the Event Notification System.
 * This class binds application properties to Java objects for easy access
 * throughout the application.
 */
@Configuration
@ConfigurationProperties(prefix = "event")
@Data
public class EventNotificationProperties {


    private Queue queue;
    private Processing processing;

    @Data
    public static class Queue {
        private Email email;
        private Sms sms;
        private Push push;

        @Data
        public static class Email {
            private int capacity;
        }

        @Data
        public static class Sms {
            private int capacity;
        }

        @Data
        public static class Push {
            private int capacity;
        }
    }

    @Data
    public static class Processing {
        private Email email;
        private Sms sms;
        private Push push;
        private int failureRatePercent = 10;

        @Data
        public static class Email {
            private int delaySeconds = 5;
        }

        @Data
        public static class Sms {
            private int delaySeconds = 3;
        }

        @Data
        public static class Push {
            private int delaySeconds = 2;
        }
    }
}
