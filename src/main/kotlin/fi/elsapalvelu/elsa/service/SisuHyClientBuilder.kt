package fi.elsapalvelu.elsa.service

import com.apollographql.apollo3.ApolloClient
import okhttp3.OkHttpClient

interface SisuHyClientBuilder {
    fun apolloClient() : ApolloClient

    fun okHttpClient(): OkHttpClient
}
