package app.deltacord.manager.di

import app.deltacord.manager.network.service.HttpService
import app.deltacord.manager.network.service.RestService
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val httpModule = module {

    fun provideJson() = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Android's own HTTP stack: Ktor's CIO engine rejects header lines over 8192 characters, and
    // github.com's download redirects now carry a ~9200 character Content-Security-Policy.
    fun provideHttpClient(json: Json) = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            socketTimeoutMillis = 15000
            connectTimeoutMillis = 15000
        }
    }

    singleOf(::provideJson)
    singleOf(::provideHttpClient)
    singleOf(::HttpService)
    singleOf(::RestService)

}