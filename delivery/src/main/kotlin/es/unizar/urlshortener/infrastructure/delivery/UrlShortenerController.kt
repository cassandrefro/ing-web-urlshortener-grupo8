package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.Redirection
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.servlet.ModelAndView
import java.net.URI
import java.util.concurrent.TimeUnit


/**
 * The specification of the controller.
 */
interface UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectTo(id: String, request: HttpServletRequest): ResponseEntity<Unit>

    /**
     * Creates a short url from details provided in [data].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>

    /**
     * Exception handler for a redirection with interstitial.
     */
    fun redirectToInterstitial(ex: UrlShortenerControllerImpl.InterstitialRedirectException,
                               response: HttpServletResponse): ModelAndView
}



/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null,
    val interstitial: Boolean? = null
)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val properties: Map<String, Any> = emptyMap(),
)

const val INTERSTITIAL_CACHE_HOURS: Long = 12

/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@RestController
class UrlShortenerControllerImpl(
    val redirectUseCase: RedirectUseCase,
    val logClickUseCase: LogClickUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase,
) : UrlShortenerController {

    class InterstitialRedirectException(redirection: Redirection) : Exception(redirection.target)

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Unit> {
        val (redirection, banner) = redirectUseCase.redirectTo(id)
        logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
        if (banner) throw InterstitialRedirectException(redirection)
        val h = HttpHeaders()
        h.location = URI.create(redirection.target)
        return ResponseEntity<Unit>(h, HttpStatus.valueOf(redirection.mode))
    }

    @ResponseBody
    @ExceptionHandler(value = [InterstitialRedirectException::class])
    @ResponseStatus(HttpStatus.OK)
    override fun redirectToInterstitial(ex: InterstitialRedirectException,
                                        response: HttpServletResponse) : ModelAndView {
        val modelAndView = ModelAndView()
        modelAndView.viewName = "interstitial"
        modelAndView.addObject("url", ex.message)
        val headerValue = CacheControl.maxAge(INTERSTITIAL_CACHE_HOURS, TimeUnit.HOURS).headerValue
        response.addHeader(HttpHeaders.CACHE_CONTROL, headerValue)
        return modelAndView
    }

    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =
        createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor,
                interstitial = data.interstitial
            )
        ).let {
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url
            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to it.properties.safe,
                    "interstitial" to (it.properties.interstitial == true)
                ),
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }
}
