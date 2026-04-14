import axios, { AxiosError } from 'axios'
import { Module } from 'vuex'

import * as api from '@/api'
import { getErikoistuvaLaakari } from '@/api/erikoistuva'
import { getVastuualueet } from '@/api/vastuuhenkilo'
import { ELSA_ROLE } from '@/utils/roles'

const auth: Module<any, any> = {
  namespaced: true,
  state: {
    status: '',
    account: null,
    loggedIn: false,
    sloEnabled: true
  },
  mutations: {
    authRequest(state) {
      state.status = 'loading'
    },
    authSuccess(state, account) {
      state.status = 'success'
      state.account = account
      state.loggedIn = true
    },
    authUnauthorized(state) {
      state.status = 'unauthorized'
      state.loggedIn = false
    },
    authError(state) {
      state.status = 'error'
      state.loggedIn = false
    },
    logoutRequest(state) {
      state.loggedIn = false
    },
    logoutSuccess(state) {
      state.status = 'success'
    },
    logoutError(state) {
      state.status = 'error'
    },
    formRequest(state) {
      state.status = 'loading'
    },
    formSuccess(state, account) {
      state.status = 'success'
      state.account = account
    },
    formError(state) {
      state.status = 'error'
    },
    sloStatus(state, status) {
      state.sloEnabled = status
    }
  },
  actions: {
    async authorize({ commit }) {
      commit('authRequest')
      try {
        const { data } = await api.getKayttaja()
        if (
          data.authorities.includes(ELSA_ROLE.ErikoistuvaLaakari) ||
          data.authorities.includes(ELSA_ROLE.YEKKoulutettava)
        ) {
          data.erikoistuvaLaakari = (await getErikoistuvaLaakari()).data
        }
        if (data.impersonated) {
          data.originalUser = (await axios.get('kayttaja-impersonated')).data
        }
        if (data.authorities.includes(ELSA_ROLE.Vastuuhenkilo)) {
          const vastuualueet = (await getVastuualueet()).data
          data.terveyskeskuskoulutusjaksoVastuuhenkilo = vastuualueet.terveyskeskuskoulutusjakso
          data.yekTerveyskeskuskoulutusjaksoVastuuhenkilo =
            vastuualueet.yekTerveyskeskuskoulutusjakso
          data.valmistumisenVastuuhenkilo = vastuualueet.valmistuminen
          data.yekValmistumisenVastuuhenkilo = vastuualueet.yekValmistuminen
        }
        commit('authSuccess', data)
      } catch (err) {
        if ((err as AxiosError<unknown>).response?.status === 401) {
          commit('authUnauthorized')
        } else {
          commit('authError')
        }
      }
    },
    async logout({ commit }) {
      const { data } = await api.sloKaytossa()
      commit('sloStatus', data)
      if (data === false) {
        await api.localLogout()
        window.location.href = '/uloskirjaus'
      }
      commit('logoutRequest')
    },
    async putErikoistuvaLaakari({ commit }, userDetails) {
      commit('formRequest')
      try {
        const { data } = await api.putErikoistuvaLaakari(userDetails)
        data.erikoistuvaLaakari = (await getErikoistuvaLaakari()).data
        commit('formSuccess', data)
      } catch (err) {
        commit('formError')
        throw err
      }
    },
    async putUser({ commit }, userDetails) {
      commit('formRequest')
      try {
        const { data } = await api.putKayttaja(userDetails)
        commit('formSuccess', data)
      } catch (err) {
        commit('formError')
        throw err
      }
    }
  },
  getters: {
    status: (state) => state.status,
    account: (state) => state.account,
    isLoggedIn: (state) => state.loggedIn,
    sloEnabled: (state) => state.sloEnabled
  }
}

export default auth
