package fi.elsapalvelu.elsa.service.integration

import okhttp3.OkHttpClient

interface OkHttpClientBuilder {
    fun okHttpClient(): OkHttpClient
}
