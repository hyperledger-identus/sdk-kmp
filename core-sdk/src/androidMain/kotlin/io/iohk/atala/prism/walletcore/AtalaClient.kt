package io.iohk.atala.prism.walletcore

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*

internal actual fun httpClient(config: HttpClientConfig<*>.() -> Unit) = HttpClient(OkHttp) {
    config(this)
}