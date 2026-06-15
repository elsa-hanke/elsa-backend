package fi.elsapalvelu.elsa.service.integration

import com.apollographql.apollo.ApolloClient

interface GraphQLClientBuilder : OkHttpClientBuilder {
    fun apolloClient(): ApolloClient
}
