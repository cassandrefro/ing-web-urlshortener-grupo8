@file:Suppress("WildcardImport")

package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.Redirect
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import es.unizar.urlshortener.core.CustomWordService
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest
@ContextConfiguration(
    classes = [
        UrlShortenerControllerImpl::class,
        RestResponseEntityExceptionHandler::class
    ]
)
class UrlShortenerControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var redirectUseCase: RedirectUseCase

    @MockBean
    private lateinit var logClickUseCase: LogClickUseCase

    @MockBean
    private lateinit var createShortUrlUseCase: CreateShortUrlUseCase

    @MockBean
    private lateinit var shortUrlRepositoryService: ShortUrlRepositoryService

    @Test
    fun `redirectTo returns a redirect when the key exists`() {
        given(redirectUseCase.redirectTo("key")).
            willReturn(Redirect(Redirection("http://example.com/"), false))

        mockMvc.perform(get("/{id}", "key"))
            .andExpect(status().isTemporaryRedirect)
            .andExpect(redirectedUrl("http://example.com/"))

        verify(logClickUseCase).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `redirectTo returns forbidden if the key exists but destination URI is not reachable`() {
        given(redirectUseCase.redirectTo("key"))
            .willAnswer { throw RedirectionNotReachableException("http://example.com/") }

        mockMvc.perform(get("/{id}", "key"))
            .andDo(print())
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.statusCode").value(403))

        verify(logClickUseCase, never()).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `redirectTo returns an interstitial page when the key exist and has interstitial`() {
        given(redirectUseCase.redirectTo("key")).
            willReturn(Redirect(Redirection("http://example.com/"), true))

        mockMvc.perform(get("/{id}", "key"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(view().name("interstitial"))
            .andExpect(model().attribute("id", "key"))

        verify(logClickUseCase).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        given(redirectUseCase.redirectTo("key"))
            .willAnswer { throw RedirectionNotFound("key") }

        mockMvc.perform(get("/{id}", "key"))
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))

        verify(logClickUseCase, never()).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `creates returns a basic redirect if it can compute a hash`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1"),
                customWord = ""
            )
        ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .param("customWord", "")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(redirectedUrl("http://localhost/f684a3c4"))
            .andExpect(jsonPath("$.url").value("http://localhost/f684a3c4"))
    }

    

    @Test
    fun `creates returns bad request if it can't compute a hash`() {
        given(
            createShortUrlUseCase.create(
                url = "ftp://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1"),
                customWord = ""
            )
        ).willAnswer { throw InvalidUrlException("ftp://example.com/") }

        mockMvc.perform(
            post("/api/link")
                .param("url", "ftp://example.com/")
                .param("customWord", "")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
    }
    
    @Test
    fun `creates returns a redirect with interstitial if it's asked for it`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1",
                                        interstitial = true),
                customWord = ""
            )
        ).willReturn(ShortUrl("f684a3c4",
                            Redirection("http://example.com/"),
                            properties = ShortUrlProperties(interstitial = true)
        ))

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .param("interstitial", "true")
                .param("customWord", "")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(redirectedUrl("http://localhost/f684a3c4"))
            .andExpect(jsonPath("$.url").value("http://localhost/f684a3c4"))
            .andExpect(jsonPath("$.properties.interstitial").value(true))
    }

    @Test
    fun `creates returns bad request if it can't compute an already used custom word`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1"),
                customWord = "example"
            )
        ).willAnswer { throw CustomWordInUseException("example") }

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .param("customWord", "example")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
    }

    @Test
    fun `creates returns bad request if it can't compute a hash when the URI is not reachable`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1"),
                customWord = ""
            )
        ).willAnswer { throw UrlNotReachableException("http://example.com/") }
        }

    @Test
    fun `creates returns bad request if it can't compute an invalid custom word`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1"),
                customWord = "e xample"
            )
        ).willAnswer { throw InvalidCustomWordException("e xample") }

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .param("customWord", "e xample")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
    }

    @Disabled
    @Test
    fun `check if custom metrics are enabled`() {
        mockMvc.perform(
            get("/actuator")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.names").exists())
            .andExpect(jsonPath("$.names.redirections-executed-counter").exists())
    }

    @Disabled
    @Test
    fun `check if urls shortened counter counts`() {
        given(
            shortUrlRepositoryService.count()
        ).willReturn(1)

        mockMvc.perform(
            post("/api/stats/metrics")
                .param("url", "http://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(redirectedUrl("http://localhost/f684a3c4"))
            .andExpect(jsonPath("$.url").value("http://localhost/f684a3c4"))
    }
}


