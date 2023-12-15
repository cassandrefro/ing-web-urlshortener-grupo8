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

        val shortUrl = shortUrlRepository.findByKey(key)
        println("shortUrl: $shortUrl")
        
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, width, height)
        
        val outputStream = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream)
        return outputStream.toByteArray()
        
    }
}

