package es.unizar.urlshortener.core

const val RETRY_AFTER : Long = 5

class InvalidUrlException(url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(key: String) : Exception("[$key] is not known")

class RedirectUnsafeException : Exception("URI is not safe")

class RedirectionNotValidatedException(refillTime: Long): Exception("URI has not been validated yet") {
    val refillTime : Long = refillTime
}

class QrCodeNotFoundException : Exception("QR code has not been generated yet")
