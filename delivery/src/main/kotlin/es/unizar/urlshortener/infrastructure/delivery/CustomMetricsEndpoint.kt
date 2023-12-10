package es.unizar.urlshortener.infrastructure.delivery

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.web.bind.annotation.RestController


@RestController
public class InterstitialCountController(registry: MeterRegistry?) {
    private val interstitialCounter: Counter

    init {
        interstitialCounter = Counter.builder("interstitial-visits-counter")
            .description("Counts interstitial visits")
            .register(registry);
    }

    fun incrementCounter() {
        interstitialCounter.increment()
    }
}

@RestController
public class QRUrlsUsedCountController(registry: MeterRegistry?) {
    private val qrUsedCounter: Counter

    init {
        qrUsedCounter = Counter.builder("qr-urls-used-counter")
            .description("Counts the number of urls with qr codes used")
            .register(registry);
    }

    fun incrementCounter() {
        qrUsedCounter.increment()
    }
}

@RestController
public class RedirectionsExecutedCountController(registry: MeterRegistry?) {
    private val redirectionsCounter: Counter

    init {
        redirectionsCounter = Counter.builder("redirections-executed-counter")
            .description("Counts the number of redirections executed using our url shortener")
            .register(registry);
    }

    fun incrementCounter() {
        redirectionsCounter.increment()
    }
}

@RestController
public class UrlsShortenedCountController(registry: MeterRegistry?) {
    private val urlsShortenedCounter: Counter

    init {
        urlsShortenedCounter = Counter.builder("urls-shortened-counter")
            .description("Counts the number of urls shortened using our url shortener")
            .register(registry);
    }

    fun incrementCounter() {
        urlsShortenedCounter.increment()
    }
}
