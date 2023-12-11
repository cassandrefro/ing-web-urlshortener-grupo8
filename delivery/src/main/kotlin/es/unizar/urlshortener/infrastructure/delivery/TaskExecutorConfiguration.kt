package es.unizar.urlshortener.infrastructure.delivery

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/**
 * Wires use cases with service implementations, and services implementations with repositories.
 *
 * **Note**: Spring Boot is able to discover this [Configuration] without further configuration.
 */
@Configuration
class TaskExecutorConfiguration() {
    @Bean(name = ["taskExecutor"])
    fun taskExecutor(): TaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 5
        executor.maxPoolSize = 10
        executor.queueCapacity = 25
        return executor
    }

    /*
    @Bean
    fun interstitialWS() : InterstitialWS = InterstitialWS()
     */
}
