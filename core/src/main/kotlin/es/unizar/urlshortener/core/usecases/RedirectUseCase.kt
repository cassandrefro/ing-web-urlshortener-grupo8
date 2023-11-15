@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

/**
 * Given a key returns a [Redirection] that contains a [URI target][Redirection.target]
 * and an [HTTP redirection mode][Redirection.mode].
 *
 * **Note**: This is an example of functionality.
 */
interface RedirectUseCase {
    fun redirectTo(key: String): Redirection
}

/**
 * Implementation of [RedirectUseCase].
 */
class RedirectUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService
) : RedirectUseCase {
    override fun redirectTo(key: String) : Redirection {
        val redirection = shortUrlRepository
            .findByKey(key)
            ?.redirection
            ?: throw RedirectionNotFound(key)

        val shortUrl = shortUrlRepository.findByKey(key)

        if (shortUrl != null) {
            if (shortUrl.properties.safe != null) {
                // Validated
                if (!shortUrl.properties.safe) {
                    throw RedirectUnsafeException()
                }
            } else {
                // Not yet validated
                throw RedirectionNotValidatedException(RETRY_AFTER)
            }
        }

        return redirection
    }
}

