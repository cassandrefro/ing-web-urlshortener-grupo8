package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RestController


@RestController
@Component
class RedirectionsExecutedCountController(registry: MeterRegistry, val clickEntityRepository: ClickRepositoryService) {
    private val redirectionsCounter: Gauge = Gauge.builder("redirections-executed-counter", clickEntityRepository::count)
        .description("Counts the number of redirections executed using our url shortener")
        .register(registry)
}

@RestController
@Component
class UrlsShortenedCountController(registry: MeterRegistry, val shortUrlEntityRepository: ShortUrlRepositoryService) {
    private val redirectionsCounter: Gauge = Gauge.builder("urls-shortened-counter", shortUrlEntityRepository::count)
        .description("Counts the number of urls shortened using our url shortener")
        .register(registry)
}