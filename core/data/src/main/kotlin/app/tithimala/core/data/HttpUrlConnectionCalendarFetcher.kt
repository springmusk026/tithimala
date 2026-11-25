package app.tithimala.core.data

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import javax.inject.Inject

internal class CalendarHttpException(val statusCode: Int, message: String) : IOException(message) {
    val isTransient: Boolean
        get() = statusCode == HTTP_TOO_MANY_REQUESTS || statusCode in SERVER_ERROR_MIN..SERVER_ERROR_MAX

    private companion object {
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val SERVER_ERROR_MIN = 500
        const val SERVER_ERROR_MAX = 599
    }
}

internal class CalendarPayloadTooLargeException(message: String) : IOException(message)

internal class HttpUrlConnectionCalendarFetcher @Inject constructor() {

    fun fetchBytes(
        baseUrl: String,
        relativePath: String,
        maxPayloadBytes: Int = DataLayerConfig.MAX_REMOTE_FILE_BYTES
    ): ByteArray {
        val url = buildValidatedUrl(baseUrl, relativePath)
        val connection = openHttpConnection(url)

        return try {
            configureConnection(connection)
            validateResponse(connection, relativePath, maxPayloadBytes)
            connection.inputStream.use { input ->
                readBoundedBytes(input, maxPayloadBytes, relativePath)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun buildValidatedUrl(baseUrl: String, relativePath: String): URL {
        CalendarDatasetStorage.validateSafeRelativePath(relativePath)
        val normalizedBase = baseUrl.trimEnd('/')
        val url = try {
            URI("$normalizedBase/$relativePath").toURL()
        } catch (e: URISyntaxException) {
            throw IllegalArgumentException("Malformed calendar update URL for $relativePath", e)
        }
        require(url.protocol == SCHEME_HTTPS || url.protocol == SCHEME_HTTP) {
            "Unsupported calendar update URL scheme: ${url.protocol}"
        }
        return url
    }

    private fun openHttpConnection(url: URL): HttpURLConnection = (url.openConnection() as? HttpURLConnection)
        ?: throw IOException("Expected HttpURLConnection for $url")

    private fun validateResponse(connection: HttpURLConnection, relativePath: String, maxPayloadBytes: Int) {
        val statusCode = connection.responseCode
        if (statusCode != HttpURLConnection.HTTP_OK) {
            throw CalendarHttpException(
                statusCode = statusCode,
                message = "Remote calendar request failed with HTTP $statusCode for $relativePath"
            )
        }
        val declaredLength = connection.contentLengthLong
        if (declaredLength > maxPayloadBytes) {
            throw CalendarPayloadTooLargeException(
                "Remote file $relativePath declared $declaredLength bytes (max $maxPayloadBytes)"
            )
        }
    }

    private fun configureConnection(connection: HttpURLConnection) {
        connection.requestMethod = HTTP_METHOD_GET
        connection.connectTimeout = DataLayerConfig.HTTP_CONNECT_TIMEOUT_MS
        connection.readTimeout = DataLayerConfig.HTTP_READ_TIMEOUT_MS
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        connection.setRequestProperty(HEADER_ACCEPT, ACCEPT_VALUE)
        connection.setRequestProperty(HEADER_USER_AGENT, USER_AGENT)
    }

    private fun readBoundedBytes(input: InputStream, maxPayloadBytes: Int, relativePath: String): ByteArray {
        val buffer = ByteArray(READ_BUFFER_SIZE)
        val output = ByteArrayOutputStream()
        var totalRead = 0
        while (true) {
            if (Thread.currentThread().isInterrupted) {
                throw InterruptedException("Interrupted while downloading $relativePath")
            }
            val read = input.read(buffer)
            if (read == EOF) break
            totalRead += read
            if (totalRead > maxPayloadBytes) {
                throw CalendarPayloadTooLargeException(
                    "Remote file $relativePath exceeded max payload size of $maxPayloadBytes bytes"
                )
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private companion object {
        const val SCHEME_HTTPS = "https"
        const val SCHEME_HTTP = "http"
        const val HTTP_METHOD_GET = "GET"
        const val HEADER_ACCEPT = "Accept"
        const val ACCEPT_VALUE = "application/json, text/plain"
        const val HEADER_USER_AGENT = "User-Agent"
        const val USER_AGENT = "Tithimala-Android"
        const val READ_BUFFER_SIZE = 8 * 1024
        const val EOF = -1
    }
}
