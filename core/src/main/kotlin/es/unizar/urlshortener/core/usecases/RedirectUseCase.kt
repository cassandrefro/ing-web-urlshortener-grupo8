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
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val validatorService: ValidatorService
) : RedirectUseCase {
    override fun redirectTo(key: String) : Redirection {
        val redirection = shortUrlRepository
        .findByKey(key)
        ?.redirection
        ?: throw RedirectionNotFound(key)

        //verify if url in the db is reachable
        val url = redirection.target
        if (!validatorService.isReachable(url)) {
            throw RedirectionNotReachableException(url)
        }   

        return redirection
    }
             
}

