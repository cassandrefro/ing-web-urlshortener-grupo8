package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.ShortUrlRepositoryService
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
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
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectToInterstitial(@PathVariable id: String, request: HttpServletRequest, response: HttpServletResponse): ModelAndView
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
    val interstitial: Boolean?
)

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
    private val shortUrlRepository: ShortUrlRepositoryService,
) : UrlShortenerController {

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Unit> {
        val (redirection, banner) = redirectUseCase.redirectTo(id)
        return if (banner)  {
            val h = HttpHeaders()
            h.location = URI.create(redirection.target)
            ResponseEntity<Unit>(h, HttpStatus.valueOf(redirection.mode))
        } else {
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
            val h = HttpHeaders()
            h.location = URI.create(redirection.target)
            ResponseEntity<Unit>(h, HttpStatus.valueOf(redirection.mode))
        }
        /*
        val shortUrl = shortUrlRepository.findByKey(id) ?: throw RedirectionNotFound(id)
        return if (shortUrl.properties.interstitial == true) {
            val h = HttpHeaders()
            val originUrl = request.requestURL.toString().replace(id,"")
            h.location = URI.create(originUrl + "interstitial/$id")
            ResponseEntity<Unit>(h, HttpStatus.TEMPORARY_REDIRECT)
        } else {
            val (redirection, banner) = redirectUseCase.redirectTo(id)
            if (banner)  {
                val h = HttpHeaders()
                h.location = URI.create(redirection.target)
                return ResponseEntity<Unit>(h, HttpStatus.valueOf(redirection.mode))
            }
            else {
                logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
                val h = HttpHeaders()
                h.location = URI.create(redirection.target)
                return ResponseEntity<Unit>(h, HttpStatus.valueOf(redirection.mode))
            }
        }

         */
    }


    @GetMapping("/interstitial/{id}")
    override fun redirectToInterstitial(@PathVariable id: String, request: HttpServletRequest, response: HttpServletResponse): ModelAndView {
        val modelAndView = ModelAndView()
        TODO()
        /*
        modelAndView.viewName = "interstitial"
        redirectUseCase.redirectTo(id).let {
            modelAndView.addObject("url", it.target)
            val headerValue = CacheControl.maxAge(12, TimeUnit.HOURS).headerValue
            response.addHeader(HttpHeaders.CACHE_CONTROL, headerValue)
            return modelAndView
        }
         */

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
                    "safe" to it.properties.safe
                ),
                interstitial = data.interstitial
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }
}
