package fi.elsapalvelu.elsa.service.integration.peppi.oulu

import com.apollographql.apollo.ApolloClient

interface PeppiOuluClientBuilder {

    fun apolloClient() : ApolloClient
}
