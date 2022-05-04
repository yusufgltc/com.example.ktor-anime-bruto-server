package com.example

import com.example.models.ApiResponse
import com.example.plugins.configureRouting
import com.example.repository.HeroRepository
import com.example.repository.NEXT_PAGE_KEY
import com.example.repository.PREVIOUS_PAGE_KEY
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.koin.java.KoinJavaComponent.inject
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    private val heroRepository: HeroRepository by inject(HeroRepository::class.java)

    @Test
    fun accessRootEndPoint_AssertCorrectInformation() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(
                    expected = "Welcome to Boruto Api",
                    actual = response.content
                )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `access all heroes endpoint, assert correct information`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes").apply {
                assertEquals(
                    HttpStatusCode.OK,
                    response.status()
                )
                val expected = ApiResponse(
                    success = true,
                    message = "ok",
                    prevPage = null,
                    nextPage = 2,
                    heroes = heroRepository.page1
                )

                val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                assertEquals(
                    expected = expected,
                    actual = actual
                )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `access all heroes endpoint, query non existing page number assert error`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes?page=6").apply {
                assertEquals(
                    HttpStatusCode.NotFound,
                    response.status()
                )
                val expected = ApiResponse(
                    success = false,
                    message = "Heroes not found"
                )

                val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                assertEquals(
                    expected = expected,
                    actual = actual
                )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `access all heroes endpoint, query invalid page number assert error`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes?page=invalid").apply {
                assertEquals(
                    HttpStatusCode.BadRequest,
                    response.status()
                )
                val expected = ApiResponse(
                    success = false,
                    message = "Only numbers allowed"
                )

                val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                assertEquals(
                    expected = expected,
                    actual = actual
                )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `access search heroes endpoint, query hero name, assert single hero result`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes/search?name=sas").apply {
                assertEquals(
                    HttpStatusCode.OK,
                    response.status()
                )

                val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                    .heroes.size
                assertEquals(
                    expected = 1,
                    actual = actual
                )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `access search heroes endpoint, query hero name, assert multiple hero result`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes/search?name=sa").apply {
                assertEquals(
                    HttpStatusCode.OK,
                    response.status()
                )

                val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                    .heroes.size
                assertEquals(
                    expected = 3,
                    actual = actual
                )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `access search heroes endpoint, query an empty text, assert empty list as a result`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes/search?name=").apply {
                assertEquals(
                    HttpStatusCode.OK,
                    response.status()
                )

                val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                    .heroes
                assertEquals(
                    expected = emptyList(),
                    actual = actual
                )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `access search heroes endpoint, query non existing hero, assert empty list as a result`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes/search?name=unknown").apply {
                assertEquals(
                    HttpStatusCode.OK,
                    response.status()
                )

                val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                    .heroes
                assertEquals(
                    expected = emptyList(),
                    actual = actual
                )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `access non existing endpoint, query non existing hero, assert not found`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/unknown").apply {
                assertEquals(
                    HttpStatusCode.NotFound,
                    response.status()
                )
                assertEquals(
                    expected = "Page not found", actual = response.content
                )

            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `access all heroes endpoint, all queries, assert correct information`() {
        withTestApplication(moduleFunction = Application::module) {
            val pages = 1..5
            val heroes = listOf(
                heroRepository.page1,
                heroRepository.page2,
                heroRepository.page3,
                heroRepository.page4,
                heroRepository.page5
            )
            pages.forEach { page ->
                handleRequest(HttpMethod.Get, "/boruto/heroes?=page=$page").apply {
                    assertEquals(
                        HttpStatusCode.OK,
                        response.status()
                    )

                    val expected = ApiResponse(
                        success = true,
                        message = "ok",
                        prevPage = calculatePage(page = page)[PREVIOUS_PAGE_KEY],
                        nextPage = calculatePage(page = page)[NEXT_PAGE_KEY],
                        heroes = heroes[page]
                    )

                    val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                    assertEquals(
                        expected = expected,
                        actual = actual
                    )
                }
            }
        }
    }

    private fun calculatePage(page: Int): Map<String, Int?> {
        var nextPage: Int? = page
        var prevPage: Int? = page

        if (page in 1..4) {
            nextPage = nextPage?.plus(1)
        }
        if (page in 2..5) {
            prevPage = prevPage?.minus(1)
        }
        if (page == 1) {
            prevPage = null
        }
        if (page == 5) {
            nextPage = null
        }
        return mapOf(
            PREVIOUS_PAGE_KEY to prevPage,
            NEXT_PAGE_KEY to nextPage
        )
    }
}