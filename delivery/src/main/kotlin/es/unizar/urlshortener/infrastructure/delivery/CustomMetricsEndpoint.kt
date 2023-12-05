package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.infrastructure.repositories.ClickEntityRepository
import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Endpoint(id="interstitial-visits-counter")
@Component
class InterstitialUrlEndpoint(
    val clickEntityRepository: ClickRepositoryService
) {
    @ReadOperation
    @Bean
    fun getInterstitialVisitsCounter(): String {
        val str = "Interstitial urls have been used " + clickEntityRepository.count() + " times";
        return str;
    }
}

class CustomMetricsEndpoint() {
    @Endpoint(id="qr-urls-used-counter")
    @Component
    public class QRUrlEndpoint {

        @ReadOperation
        @Bean
        fun getQRUrlUsedCounter(): String {
            val str = "QR urls have been used " + "times";
            return str;
        }
    }
}