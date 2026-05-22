package fi.elsapalvelu.elsa.service

import com.apollographql.apollo.ApolloClient

interface PeppiOuluClientBuilder {

    fun apolloClient() : ApolloClient
}
