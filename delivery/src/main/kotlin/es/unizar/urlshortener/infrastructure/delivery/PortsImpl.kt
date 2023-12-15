package es.unizar.urlshortener.infrastructure.delivery

import com.google.common.hash.Hashing
import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.ValidatorService
import org.apache.commons.validator.routines.UrlValidator
import es.unizar.urlshortener.core.CustomWordService
import java.nio.charset.StandardCharsets

/**
 * Implementation of the port [ValidatorService].
 */
class ValidatorServiceImpl : ValidatorService {
    override fun isValid(url: String) = urlValidator.isValid(url)

    companion object {
        val urlValidator = UrlValidator(arrayOf("http", "https"))
    }
}

/**
 * Implementation of the port [HashService].
 */
class HashServiceImpl : HashService {
    override fun hasUrl(url: String, customWord: String): String {
        return if (customWord != "") {
            customWord
        } else {
            Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
        }
    } 
}

/**
 * Implementation of the port [CustomWordService].
 */
class CustomWordServiceImpl : CustomWordService {
    override fun isValid(customWord: String): Boolean {
        return customWord.matches(Regex("[a-zA-Z0-9]+"))
    }
}
