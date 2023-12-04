package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.Redirection
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlRepositoryService

/**
 * Given a key returns a [Redirection] that contains a [URI target][Redirection.target]
 * and an [HTTP redirection mode][Redirection.mode].
 *
 * **Note**: This is an example of functionality.
 */
interface RedirectUseCase {
    fun redirectTo(key: String): Redirect
}

data class Redirect(val value: Redirection, val interstitial: Boolean)
fun ShortUrl.toRedirect() = Redirect(value = redirection, interstitial = properties.interstitial ?: false)

class RedirectUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService
) : RedirectUseCase {
    override fun redirectTo(key: String) : Redirect = shortUrlRepository
        .findByKey(key)
        ?.toRedirect()
        ?: throw RedirectionNotFound(key)
}

