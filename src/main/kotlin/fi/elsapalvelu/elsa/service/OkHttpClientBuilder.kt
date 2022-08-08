package fi.elsapalvelu.elsa.service

import okhttp3.OkHttpClient

interface OkHttpClientBuilder {
    fun okHttpClient(): OkHttpClient
}
