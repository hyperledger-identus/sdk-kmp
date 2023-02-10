package io.iohk.atala.prism.walletsdk.prismagent.helpers

import io.iohk.atala.prism.walletsdk.domain.models.HttpResponse
import io.iohk.atala.prism.walletsdk.prismagent.shared.GetRequestBuilder
import io.iohk.atala.prism.walletsdk.prismagent.shared.KeyValue
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.Url


open class ApiImpl(override var client: HttpClient) : Api {
    override suspend fun request(
        httpMethod: String,
        url: String,
        urlParameters: Array<KeyValue>,
        httpHeaders: Array<KeyValue>,
        body: Any?
    ): HttpResponse {
        val request = GetRequestBuilder(
            httpMethod = HttpMethod(httpMethod),
            url = Url(url),
            urlParametersArray = urlParameters,
            httpHeadersArray = httpHeaders,
            body = body
        )
        val response = client.request(request)
        return HttpResponse(
            status = response.status.value,
            jsonString = response.bodyAsText()
        )
    }
}
