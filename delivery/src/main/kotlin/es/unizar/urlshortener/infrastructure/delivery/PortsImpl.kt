package es.unizar.urlshortener.infrastructure.delivery

import com.google.common.hash.Hashing
import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.ValidatorService
import org.apache.commons.validator.routines.UrlValidator
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.net.URI
import java.nio.charset.StandardCharsets

/**
 * Implementation of the port [ValidatorService].
 */
class ValidatorServiceImpl : ValidatorService {
    
    private companion object {
        const val HTTP_STATUS_OK = 200
        const val TIMEOUT_SECONDS = 5L
        const val RETRY_INTERVAL_MILLIS = 1000L
        const val RETRY_ATTEMPTS = 3
        val urlValidator = UrlValidator(arrayOf("http", "https"))
    }

    override fun isValid(url: String) = urlValidator.isValid(url)

    override fun isReachable(url: String): Boolean {
        val client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build()
        val request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(TIMEOUT_SECONDS)) // Timeout for the request
                        .GET()
                        .build()

        repeat(RETRY_ATTEMPTS) {
            val response = client.send(request, HttpResponse.BodyHandlers.discarding())
            if (response.statusCode() == HTTP_STATUS_OK) {
                return true
            }
            Thread.sleep(RETRY_INTERVAL_MILLIS) // Request spaced 1 sec apart before retrying
        }

        return false
    }
}

/**
 * Implementation of the port [HashService].
 */
class HashServiceImpl : HashService {
    override fun hasUrl(url: String) = Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
}
