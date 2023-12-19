package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RestController


@Component
class RedirectionsExecutedCountController(registry: MeterRegistry,
                                          private final val clickEntityRepository: ClickRepositoryService) {
    private var cacheValue = clickEntityRepository.count()

    fun getCount() = cacheValue

    @Scheduled(fixedDelay = 10000)
    fun updateCount(){
        cacheValue = clickEntityRepository.count()
    }

    val redirectionsCounter: Gauge = Gauge.builder("redirections-executed-counter", this::getCount)
        .description("Counts the number of redirections executed using our url shortener")
        .register(registry)
}

@Component
class UrlsShortenedCountController(registry: MeterRegistry,
                                   private final val shortUrlEntityRepository: ShortUrlRepositoryService) {
    private var cacheValue = shortUrlEntityRepository.count()

    fun getCount() = cacheValue

    @Scheduled(fixedDelay = 10000)
    fun updateCount(){
        cacheValue = shortUrlEntityRepository.count()
    }

    val redirectionsCounter: Gauge = Gauge.builder("urls-shortened-counter", this::getCount)
        .description("Counts the number of urls shortened using our url shortener")
        .register(registry)
}
