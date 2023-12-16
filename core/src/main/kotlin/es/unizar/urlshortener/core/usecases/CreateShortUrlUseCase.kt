@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import kotlinx.coroutines.*


/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CreateShortUrlUseCase {
    fun create(url: String, data: ShortUrlProperties, customWord: String): ShortUrl
}

/**
 * Implementation of [CreateShortUrlUseCase].
 */
class CreateShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val validatorService: ValidatorService,
    private val hashService: HashService,
    private val customWordService: CustomWordService
) : CreateShortUrlUseCase {
    override fun create(url: String, data: ShortUrlProperties, customWord: String): ShortUrl {
        //verify if the custom word is valid for an url
        if (customWord.isNotEmpty() && !customWordService.isValid(customWord)) {
            throw InvalidCustomWordException(customWord)
        }

        //verify if the id is in the repository
        val id: String = hashService.hasUrl(url, customWord)
        val shortUrl = shortUrlRepository.findByKey(id)

        // if exists
        if (shortUrl != null) {
            //verify if the url matches
            if (shortUrl.redirection.target == url) {
                return shortUrl
            } else {
                // the id custom Word is in the repository but the url is different
                throw CustomWordInUseException(customWord)
            }
        }
        if (validatorService.isValid(url)) {
            // verify if url is reachable
            if (!validatorService.isReachable(url)) {
                throw UrlNotReachableException(url)
            }
            val hashCalculated: String = hashService.hasUrl(url, customWord)
            val su = ShortUrl(
                hash = hashCalculated,
                redirection = Redirection(target = url),
                properties = ShortUrlProperties(
                    safe = data.safe,
                    ip = data.ip,
                    sponsor = data.sponsor,
                    qr = data.qr,
                    interstitial = data.interstitial
                )
            )
            shortUrlRepository.save(su)
            return su
        } else {
            throw InvalidUrlException(url)
        }
    }
}
