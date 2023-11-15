package es.unizar.urlshortener.infrastructure.delivery

import com.google.common.hash.Hashing
import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.ValidatorService
import org.apache.commons.validator.routines.UrlValidator
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Implementation of the port [ValidatorService].
 */
class ValidatorServiceImpl : ValidatorService {
    override fun isValid(url: String) = urlValidator.isValid(url)

    override fun isReachable(url: String) : Boolean {
        
        //val url = URL("http://www.example.com")
        val connection: HttpURLConnection = URL(url).openConnection() as HttpURLConnection

        //disables automatic following of redirects
        connection.setInstanceFollowRedirects(false)

        val responseCode: Int = connection.getResponseCode()

        //Checking if the Response Code is HTTP_OK
        return responseCode.equals(HttpURLConnection.HTTP_OK)
    }

    companion object {
        val urlValidator = UrlValidator(arrayOf("http", "https"))
    }
}

/**
 * Implementation of the port [HashService].
 */
class HashServiceImpl : HashService {
    override fun hasUrl(url: String) = Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
}
