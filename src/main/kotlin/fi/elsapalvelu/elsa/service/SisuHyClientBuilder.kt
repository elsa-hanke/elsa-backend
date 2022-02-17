package fi.elsapalvelu.elsa.service

import com.apollographql.apollo3.ApolloClient

interface SisuHyClientBuilder {
    fun apolloClient() : ApolloClient
}
