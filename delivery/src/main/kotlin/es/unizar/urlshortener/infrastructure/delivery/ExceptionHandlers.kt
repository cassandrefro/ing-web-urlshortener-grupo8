package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.InvalidUrlException
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.RedirectionNotReachableException
import es.unizar.urlshortener.core.UrlNotReachableException
import es.unizar.urlshortener.core.InvalidCustomWordException
import es.unizar.urlshortener.core.CustomWordInUseException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@ControllerAdvice
class RestResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {

    @ResponseBody
    @ExceptionHandler(value = [InvalidUrlException::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun invalidUrls(ex: InvalidUrlException) = ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [RedirectionNotFound::class])
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun redirectionNotFound(ex: RedirectionNotFound) = ErrorMessage(HttpStatus.NOT_FOUND.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [UrlNotReachableException::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun urlNotReachable(ex: UrlNotReachableException) = ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [RedirectionNotReachableException::class])
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun redirectionNotReachable(ex: RedirectionNotReachableException) = 
                    ErrorMessage(HttpStatus.FORBIDDEN.value(), ex.message)
    @ExceptionHandler(value = [InvalidCustomWordException::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun invalidCustomWord(ex: InvalidCustomWordException) = ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [CustomWordInUseException::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun customWordInUse(ex: CustomWordInUseException) = ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)
}

data class ErrorMessage(
    val statusCode: Int,
    val message: String?,
    val timestamp: String = DateTimeFormatter.ISO_DATE_TIME.format(OffsetDateTime.now())
)
