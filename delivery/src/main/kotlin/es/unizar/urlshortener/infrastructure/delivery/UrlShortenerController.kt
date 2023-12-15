package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import es.unizar.urlshortener.core.usecases.QRCodeUseCase
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI


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
    fun getQr(id: String, request: HttpServletRequest): ResponseEntity<ByteArray>
}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null,
    val qr: Boolean? = null
)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val properties: Map<String, Any> = emptyMap(),
    val qr: String? = null
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
    val qrCodeUseCase: QRCodeUseCase,
    val shortUrlRepository: ShortUrlRepositoryService
) : UrlShortenerController {

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Unit> =
        redirectUseCase.redirectTo(id).let {
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
            val h = HttpHeaders()
            h.location = URI.create(it.target)
            ResponseEntity<Unit>(h, HttpStatus.valueOf(it.mode))
        }

    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =
        createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor,
                qr = data.qr
            )
        ).let {
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url
            val qrUrl = if (data.qr) "$url/qr" else null 
            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to it.properties.safe
                ),
                qr = qrUrl
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }
    
    @GetMapping("/{id}/qr", produces = [MediaType.IMAGE_PNG_VALUE])
    override fun getQr(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<ByteArray> {
        //If the id is not in the db, return 404
        val shortUrl = shortUrlRepository.findByKey(id) ?: throw QrCodeNotFoundException(id)

        //If the id is in the db but the qr property is false, return 404

        //val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it, null) }.toUri()
        val completeUrl = request.requestURL.toString().replace("/qr", "")
        var qrCodeImage = qrCodeUseCase.generateQRCode(completeUrl)
        val h = HttpHeaders()
        h.contentType = MediaType.IMAGE_PNG
        return ResponseEntity(qrCodeImage, h, HttpStatus.OK)

        /*if(shortUrl == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        } else {
            //if(shortUrl.properties.qr == false || shortUrl.properties.qr == null) {
              //  println("QR code not found")
                //return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
           // } else {
                //val baseUrl = request.requestURL.substring(0, request.requestURL.indexOf(request.requestURI))
                //val shortenedUrl = baseUrl + "/" + id
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it, null) }.toUri()

                val completeUrl = request.requestURL.toString().replace("/qr", "")

                var qrCodeImage = qrCodeUseCase.generateQRCode(completeUrl)
                //val headers = HttpHeaders().apply { contentType = MediaType.IMAGE_PNG }
                val h = HttpHeaders()
                h.contentType = MediaType.IMAGE_PNG
                return ResponseEntity(qrCodeImage, h, HttpStatus.OK)
           // }
        }  */ 
    }
}
