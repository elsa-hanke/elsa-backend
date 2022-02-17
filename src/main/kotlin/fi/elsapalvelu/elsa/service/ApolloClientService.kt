package fi.elsapalvelu.elsa.service

import com.apollographql.apollo3.ApolloClient

interface ApolloClientService {
    fun getSisuHyApolloClient() : ApolloClient
}
