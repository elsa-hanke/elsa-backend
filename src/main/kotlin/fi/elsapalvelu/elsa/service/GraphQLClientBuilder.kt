package fi.elsapalvelu.elsa.service

import com.apollographql.apollo.ApolloClient

interface GraphQLClientBuilder : OkHttpClientBuilder {
    fun apolloClient(): ApolloClient
}
