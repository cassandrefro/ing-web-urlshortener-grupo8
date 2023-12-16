package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import es.unizar.urlshortener.core.usecases.QRCodeUseCase
import es.unizar.urlshortener.core.ShortUrlNotFoundException
import es.unizar.urlshortener.core.QrCodeNotEnabledException
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
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

     * Returns a QR code for the short url identified by its [id].
     *
     * **Note**: Delivery of use case [QRCodeUseCase].
     */
    fun getQr(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<ByteArray>

    /**
     * Exception handler for a redirection with interstitial.
     */
    fun redirectToInterstitial(ex: UrlShortenerControllerImpl.InterstitialRedirectException
                               , request: HttpServletRequest, response: HttpServletResponse): ModelAndView
}



/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null,
    val qr: Boolean = false,
    val customWord: String,
    val interstitial: Boolean? = null
)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val properties: Map<String, Any> = emptyMap(),
    val qr: String? = ""
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
    val qrCodeUseCase: QRCodeUseCase,
    val shortUrlRepository: ShortUrlRepositoryService
) : UrlShortenerController {
    class InterstitialRedirectException(id: String) : Exception(id)
    val logger = LoggerFactory.getLogger(UrlShortenerController::class.java)

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Unit> {
        logger.info("Endpoint: /{id:(?!api|index).*}")
        val (redirection, banner) = redirectUseCase.redirectTo(id)
        logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
        if (banner) throw InterstitialRedirectException(id)
        val h = HttpHeaders()
        h.location = URI.create(redirection.target)
        return ResponseEntity<Unit>(h, HttpStatus.valueOf(redirection.mode))
    }

    @ResponseBody
    @ExceptionHandler(value = [InterstitialRedirectException::class])
    @ResponseStatus(HttpStatus.OK)
    override fun redirectToInterstitial(ex: InterstitialRedirectException, request: HttpServletRequest,
                                        response: HttpServletResponse) : ModelAndView {
        logger.info("ExceptionHandler: InterstitialRedirectException with ${ex.message}")
        val modelAndView = ModelAndView()
        modelAndView.viewName = "interstitial"
        modelAndView.addObject("id", ex.message)
        //modelAndView.addObject("url", "ws://localhost:8080/ws/")
        val completeUrl = ex.message?.let { request.requestURL.toString().replace("http://", "ws://").replace(it, "") }
        modelAndView.addObject("url", completeUrl + "ws/")
        val headerValue = CacheControl.maxAge(INTERSTITIAL_CACHE_HOURS, TimeUnit.HOURS).headerValue
        response.addHeader(HttpHeaders.CACHE_CONTROL, headerValue)
        return modelAndView
    }

    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> {
        logger.info("Endpoint: /api/link")
        createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor,
                qr = data.qr,
                interstitial = data.interstitial
            ),
            customWord = data.customWord
        ).let {
            logger.info("Created shortUrl")
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url
            val qrUrl = if (data.qr) "$url/qr" else ""
            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to it.properties.safe,
                    "interstitial" to (it.properties.interstitial == true)
                ),
                qr = qrUrl
            )
            return ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }
    }

    @GetMapping("/{id}/qr", produces = [MediaType.IMAGE_PNG_VALUE])
    override fun getQr(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<ByteArray> {
        //If the id is not in the db, return 404
        val shortUrl = shortUrlRepository.findByKey(id) ?: throw ShortUrlNotFoundException(id)

        //If the id is in the db but the qr property is false, return 404
        if (!shortUrl.properties.qr) {
            throw QrCodeNotEnabledException(id)
        }

        val url = linkTo<UrlShortenerControllerImpl> { redirectTo(id, request) }.toUri()
        //val qrCodeUrl = request.requestURL.toString().replace("/qr", "")
        var qrCodeImage = qrCodeUseCase.generateQRCode(url.toString())
        val h = HttpHeaders()
        h.contentType = MediaType.IMAGE_PNG
        return ResponseEntity(qrCodeImage, h, HttpStatus.OK)
    }
}
