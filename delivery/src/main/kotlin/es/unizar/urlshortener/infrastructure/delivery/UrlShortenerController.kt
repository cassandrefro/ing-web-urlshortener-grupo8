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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
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
    val qr: Boolean? = false,
    val customWord: String? = "",
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
@Tag(name = "URL-Shortener", description = "URL-Shortener API")
@RestController
class UrlShortenerControllerImpl(
    val redirectUseCase: RedirectUseCase,
    val logClickUseCase: LogClickUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase,
    val qrCodeUseCase: QRCodeUseCase,
    val shortUrlRepository: ShortUrlRepositoryService
) : UrlShortenerController {
    class InterstitialRedirectException(id: String) : Exception(id)
    val logger : Logger = LoggerFactory.getLogger(UrlShortenerController::class.java)

    @Operation(
        summary = "Redirect to a specified URI identified by the parameter id",
        description = "If the shortened URI exists and the redirection is possible, " +
                "if there is no interstitial, it will return a Temporal Redirect code (307) " +
                "and into the Location Header the location of the destiny URI, " +
                "if there is interstitial, it will return a OK code (200) " +
                "an interstitial HTML, the redirection will be made by te user agent using the" +
                " information located at the HTML\n\n" +
                "If the shortened URI does not exist it will return a Not Found code (404)\n\n" +
                "If the shortened URI is not reachable it will return a Bad Request code (400)",
    )
    @ApiResponse(responseCode = "307")
    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@Parameter(description = "Id that identifies an URI") @PathVariable id: String,
                            request: HttpServletRequest): ResponseEntity<Unit> {
        logger.info("Endpoint: /{id:(?!api|index).*}")
        val (redirection, banner) = redirectUseCase.redirectTo(id)
        logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
        if (banner) throw InterstitialRedirectException(id)
        val h = HttpHeaders()
        h.location = URI.create(redirection.target)
        return ResponseEntity<Unit>(h, HttpStatus.valueOf(redirection.mode))
    }

    @ExceptionHandler(value = [InterstitialRedirectException::class])
    @ResponseStatus(HttpStatus.OK)
    @ApiResponse(responseCode = "200", description = "HTML for the interstitial page that automatically redirect " +
            "to the destiny URI after 5 seconds")
    override fun redirectToInterstitial(ex: InterstitialRedirectException, request: HttpServletRequest,
                                        response: HttpServletResponse) : ModelAndView {
        logger.info("ExceptionHandler: InterstitialRedirectException with ${ex.message}")
        val modelAndView = ModelAndView()
        modelAndView.viewName = "interstitial"
        modelAndView.addObject("id", ex.message)
        val completeUrl = ex.message?.let { request.requestURL.toString().replace("http://", "ws://").replace(it, "") }
        modelAndView.addObject("url", completeUrl + "ws/")
        val headerValue = CacheControl.maxAge(INTERSTITIAL_CACHE_HOURS, TimeUnit.HOURS).headerValue
        response.addHeader(HttpHeaders.CACHE_CONTROL, headerValue)
        return modelAndView
    }

    @Operation(
        summary = "Create a shortened URI",
        description = "If the request is completed successfully  it will return a Created code (201) " +
                ", into the Location header the location of the shortened URI created" +
                ", with a Content-Type header = \"application/json\" and with information in JSON" +
                " about the shortened URI created\n\n" +
                "If the shortened URI can not be create it will return a Bad Request code (400), with a " +
                "Content-Type header = \"application/json\" and with information in JSON explaining the error"
    )
    @ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = ShortUrlDataOut::class),
        mediaType = "application/json")])
    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> {
        logger.info("Endpoint: /api/link")
        createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor,
                qr = data.qr == true,
                interstitial = data.interstitial
            ),
            customWord = data.customWord.orEmpty()
        ).let {
            logger.info("Created shortUrl")
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url
            val qrUrl = if (data.qr == true) "$url/qr" else ""
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

    @Operation(
        summary = "Create a QR code that redirect to a specified URI identified by the parameter id",
        description = "If the shortened URI exists and the redirection is possible, " +
                "it will return an OK code (200) with Content-Type=\"image/png\" and the appropriate QR\n\n" +
                "If the shortened URI does not exist it will return a Not Found code (404)\n\n" +
                "If the shortened URI is not reachable it will return a Bad Request code (400)",
    )
    @GetMapping("/{id}/qr", produces = [MediaType.IMAGE_PNG_VALUE])
    override fun getQr(@Parameter(description = "Id that identifies an URI") @PathVariable id: String,
                       request: HttpServletRequest): ResponseEntity<ByteArray> {
        //If the id is not in the db, return 404
        val shortUrl = shortUrlRepository.findByKey(id) ?: throw ShortUrlNotFoundException(id)

        //If the id is in the db but the qr property is false, return 404
        if (!shortUrl.properties.qr) {
            throw QrCodeNotEnabledException(id)
        }

        val url = linkTo<UrlShortenerControllerImpl> { redirectTo(id, request) }.toUri()
        //val qrCodeUrl = request.requestURL.toString().replace("/qr", "")
        val qrCodeImage = qrCodeUseCase.generateQRCode(url.toString())
        val h = HttpHeaders()
        h.contentType = MediaType.IMAGE_PNG
        return ResponseEntity(qrCodeImage, h, HttpStatus.OK)
    }
}
