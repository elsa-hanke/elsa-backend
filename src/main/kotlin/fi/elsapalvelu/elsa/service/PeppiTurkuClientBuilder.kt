package fi.elsapalvelu.elsa.service

import okhttp3.OkHttpClient

interface PeppiTurkuClientBuilder {
    fun okHttpClient(): OkHttpClient
}
