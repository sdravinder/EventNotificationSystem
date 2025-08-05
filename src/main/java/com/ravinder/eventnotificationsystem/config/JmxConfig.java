package com.ravinder.eventnotificationsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.MBeanExporter;

/**
 * Configuration class for JMX monitoring support.
 * Enables JMX monitoring for queue sizes, processing rates, and failure metrics.
 */
@Configuration
public class JmxConfig {

    /**
     * Configure MBean exporter for JMX monitoring
     *
     * @return MBeanExporter configured with annotation support
     */
    @Bean
    public MBeanExporter mBeanExporter() {
        MBeanExporter exporter = new MBeanExporter();
        exporter.setAutodetect(true);
        return exporter;
    }
}
