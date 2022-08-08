package fi.elsapalvelu.elsa.service

import com.apollographql.apollo3.ApolloClient

interface GraphQLClientBuilder : OkHttpClientBuilder {
    fun apolloClient(): ApolloClient
}
