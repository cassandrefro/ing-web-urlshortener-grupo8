@file:Suppress("WildcardImport", "NestedBlockDepth")

package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import java.io.ByteArrayOutputStream

/**
 * Given a key returns a QR code that links to the resource passed.
 */
interface QRCodeUseCase {
    fun generateQRCode(url: String, width: Int = 256, height: Int = 256) : ByteArray
}

/**
 * Implementation of [QRCodeUseCase].
 */
class QRCodeUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService
) : QRCodeUseCase  {
    override fun generateQRCode(url: String, width: Int, height: Int): ByteArray {
        // url has format http://localhost:8080/key
        val key = url.split("/").last()
        // print in the console the key
        println("key: $key")
        val shortUrl = shortUrlRepository.findByKey(key)
        println("shortUrl: $shortUrl")
        //shortUrl?.let {
            //shortUrl.properties.safe?.let { safe ->
                //if (!shortUrl.properties.safe) {
                //    throw RedirectUnsafeException()
                //} else {
                    //shortUrl.properties.qr?.let { qr ->
                      //  if (!qr) {
                        //    throw QrCodeNotFoundException()
                        //} else {
                            val qrCodeWriter = QRCodeWriter()
                            val bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, width, height)
                            
                            val outputStream = ByteArrayOutputStream()
                            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream)
                            return outputStream.toByteArray()
                        //}
                    //} ?: throw QrCodeNotFoundException()
                //}

            //} ?: throw RedirectionNotValidatedException(RETRY_AFTER)

        //} ?: throw RedirectionNotValidatedException(RETRY_AFTER)
        
    }
}

