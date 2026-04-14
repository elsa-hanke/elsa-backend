import { Module } from 'vuex'

import * as api from '@/api/kouluttaja'

const kouluttaja: Module<any, any> = {
  namespaced: true,
  state: {
    status: '',
    koejaksot: null
  },
  mutations: {
    koejaksotRequest(state) {
      state.status = 'loading'
    },
    koejaksotSuccess(state, koejaksot) {
      state.status = 'success'
      state.koejaksot = koejaksot
    },
    koejaksotError(state) {
      state.status = 'error'
    },
    formRequest(state) {
      state.status = 'loading'
    },
    formSuccess(state) {
      state.status = 'success'
    },
    formError(state) {
      state.status = 'error'
    }
  },
  actions: {
    async getKoejaksot({ commit }) {
      commit('koejaksotRequest')
      try {
        const { data } = await api.getKoejaksot()
        commit('koejaksotSuccess', data)
      } catch (err) {
        commit('koejaksotError')
        throw err
      }
    },
    async putKoulutussopimus({ commit, dispatch }, koulutussopimusLomake) {
      commit('formRequest')
      try {
        await api.putKoulutussopimus(koulutussopimusLomake)
        commit('formSuccess')
        await dispatch('getKoejaksot')
      } catch (err) {
        commit('formError')
        throw err
      }
    },
    async putAloituskeskustelu({ commit, dispatch }, aloituskeskusteluLomake) {
      commit('formRequest')
      try {
        await api.putAloituskeskustelu(aloituskeskusteluLomake)
        commit('formSuccess')
        await dispatch('getKoejaksot')
      } catch (err) {
        commit('formError')
        throw err
      }
    },
    async putValiarviointi({ commit, dispatch }, valiarviointiLomake) {
      commit('formRequest')
      try {
        await api.putValiarviointi(valiarviointiLomake)
        commit('formSuccess')
        await dispatch('getKoejaksot')
      } catch (err) {
        commit('formError')
        throw err
      }
    },
    async putKehittamistoimenpiteet({ dispatch, commit }, kehittamistoimenpiteetLomake) {
      commit('formRequest')
      try {
        await api.putKehittamistoimenpiteet(kehittamistoimenpiteetLomake)
        commit('formSuccess')
        await dispatch('getKoejaksot')
      } catch (err) {
        commit('formError')
        throw err
      }
    },
    async putLoppukeskustelu({ commit, dispatch }, loppukeskusteluLomake) {
      commit('formRequest')
      try {
        await api.putLoppukeskustelu(loppukeskusteluLomake)
        commit('formSuccess')
        await dispatch('getKoejaksot')
      } catch (err) {
        commit('formError')
        throw err
      }
    }
  },
  getters: {
    status: (state) => state.status,
    koejaksot: (state) => state.koejaksot
  }
}

export default kouluttaja
