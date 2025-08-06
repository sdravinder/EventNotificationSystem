package com.ravinder.eventnotificationsystem.config;

import com.ravinder.eventnotificationsystem.processor.EmailEventProcessor;
import com.ravinder.eventnotificationsystem.processor.PushEventProcessor;
import com.ravinder.eventnotificationsystem.processor.SmsEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to automatically start all event processors when the application starts.
 * This ensures that all processor threads are running and ready to process events from their respective queues.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ProcessorStartupConfig {

    private final EmailEventProcessor emailEventProcessor;
    private final SmsEventProcessor smsEventProcessor;
    private final PushEventProcessor pushEventProcessor;

    /**
     * Application runner to start all event processors after the Spring context is fully loaded.
     * 
     * @return ApplicationRunner that starts all processors
     */
    @Bean
    public ApplicationRunner startProcessors() {
        return args -> {
            log.info("Starting all event processors...");
            
            // Start all processors
            emailEventProcessor.start();
            smsEventProcessor.start();
            pushEventProcessor.start();
            
            log.info("All event processors started successfully");
        };
    }
}
