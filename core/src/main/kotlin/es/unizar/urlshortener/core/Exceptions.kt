package es.unizar.urlshortener.core

const val RETRY_AFTER = 5


class InvalidUrlException(url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(key: String) : Exception("[$key] is not known")

/*class RedirectionNotValidatedException(retryAfter: Int) : Exception("The URL has not been validated yet") {
    val retryAfter = retryAfter
}*/

class UrlNotReachableException(url: String) : Exception("[$url] is not reachable")

class RedirectionNotReachableException(url: String) : Exception("[$url] is not reachable")
