import { Module } from 'vuex'

import * as api from '@/api/erikoistuva'
import { sortByAsc } from '@/utils/sort'

const erikoistuva: Module<any, any> = {
  namespaced: true,
  state: {
    status: '',
    koejakso: null,
    kouluttajat: null,
    kouluttajatJaVastuuhenkilot: null
  },
  mutations: {
    koejaksoRequest(state) {
      state.status = 'loading'
    },
    koejaksoSuccess(state, koejakso) {
      state.status = 'success'
      state.koejakso = koejakso
    },
    koejaksoError(state) {
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
    },
    kouluttajatRequest(state) {
      state.status = 'loading'
    },
    kouluttajatSuccess(state, kouluttajat) {
      state.status = 'success'
      state.kouluttajat = kouluttajat
    },
    kouluttajatError(state) {
      state.status = 'error'
    },
    kouluttajatJaVastuuhenkilotRequest(state) {
      state.status = 'loading'
    },
    kouluttajatJaVastuuhenkilotSuccess(state, kouluttajatJaVastuuhenkilot) {
      state.status = 'success'
      state.kouluttajatJaVastuuhenkilot = kouluttajatJaVastuuhenkilot
    },
    kouluttajatJaVastuuhenkilotError(state) {
      state.status = 'error'
    }
  },
  actions: {
    async getKoejakso({ commit }) {
      commit('koejaksoRequest')
      try {
        const { data } = await api.getKoejakso()
        commit('koejaksoSuccess', data)
      } catch (err) {
        commit('koejaksoError')
        throw err
      }
    },
    async postKoulutussopimus({ dispatch, commit }, koulutussopimusLomake) {
      commit('formRequest')
      try {
        await api.postKoulutussopimus(koulutussopimusLomake)
        commit('formSuccess')
        await dispatch('getKoejakso')
      } catch (err) {
        commit('formError')
        throw err
      }
    },
    async putKoulutussopimus({ dispatch, commit }, koulutussopimusLomake) {
      commit('formRequest')
      try {
        await api.putKoulutussopimus(koulutussopimusLomake)
        commit('formSuccess')
        await dispatch('getKoejakso')
      } catch (err) {
        commit('formError')
        throw err
      }
    },
    async deleteKoulutussopimus({ dispatch, commit }, koulutussopimusLomake) {
      commit('formRequest')
      try {
        await api.deleteKoulutussopimus(koulutussopimusLomake)
        commit('formSuccess')
        await dispatch('getKoejakso')
      } catch (err) {
        commit('formError')
        throw err
      }
    },
    async postAloituskeskustelu({ dispatch, commit }, aloituskeskusteluLomake) {
      commit('formRequest')
      try {
        await api.postAloituskeskustelu(aloituskeskusteluLomake)
        commit('formSuccess')
        await dispatch('getKoejakso')
      } catch (err) {
        commit('formError')
        throw err
      }
    },
    async putAloituskeskustelu({ dispatch, commit }, aloituskeskusteluLomake) {
      commit('formRequest')
      try {
        await api.putAloituskeskustelu(aloituskeskusteluLomake)
        commit('formSuccess')
        await dispatch('getKoejakso')
      } catch (err) {
        commit('formError')
        throw err
      }
    },

    async deleteAloituskeskustelu({ dispatch, commit }, aloituskeskusteluLomake) {
      commit('formRequest')
      try {
        await api.deleteAloituskeskustelu(aloituskeskusteluLomake)
        commit('formSuccess')
        await dispatch('getKoejakso')
      } catch (err) {
        commit('formError')
        throw err
      }
    },
    async postValiarviointi({ dispatch, commit }, valiarviointiLomake) {
      commit('formRequest')
      try {
        await api.postValiarviointi(valiarviointiLomake)
        commit('formSuccess')
        await dispatch('getKoejakso')
      } catch (err) {
        commit('formError')
        throw err
      }
    },
    async putValiarviointi({ dispatch, commit }, valiarviointiLomake) {
      commit('formRequest')
      try {
        await api.putValiarviointi(valiarviointiLomake)
        commit('formSuccess')
        await dispatch('getKoejakso')
      } catch (err) {
        commit('formError')
        throw err
      }
    },

    async deleteValiarviointi({ dispatch, commit }, valiarviointiLomake) {
      commit('formRequest')
      try {
        await api.deleteValiarviointi(valiarviointiLomake)
        commit('formSuccess')
        await dispatch('getKoejakso')
      } catch (err) {
        commit('formError')
        throw err
      }
    },

    async postKehittamistoimenpiteet({ dispatch, commit }, kehittamistoimenpiteetLomake) {
      commit('formRequest')
      try {
        await api.postKehittamistoimenpiteet(kehittamistoimenpiteetLomake)
        commit('formSuccess')
        await dispatch('getKoejakso')
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
        await dispatch('getKoejakso')
      } catch (err) {
        commit('formError')
        throw err
      }
    },
    async deleteKehittamistoimenpiteet({ dispatch, commit }, kehittamistoimenpiteetLomake) {
      commit('formRequest')
      try {
        await api.deleteKehittamistoimenpiteet(kehittamistoimenpiteetLomake)
        commit('formSuccess')
        await dispatch('getKoejakso')
      } catch (err) {
        commit('formError')
        throw err
      }
    },
    async postLoppukeskustelu({ dispatch, commit }, loppukeskusteluLomake) {
      commit('formRequest')
      try {
        await api.postLoppukeskustelu(loppukeskusteluLomake)
        commit('formSuccess')
        await dispatch('getKoejakso')
      } catch (err) {
        commit('formError')
        throw err
      }
    },
    async putLoppukeskustelu({ dispatch, commit }, loppukeskusteluLomake) {
      commit('formRequest')
      try {
        await api.putLoppukeskustelu(loppukeskusteluLomake)
        commit('formSuccess')
        await dispatch('getKoejakso')
      } catch (err) {
        commit('formError')
        throw err
      }
    },
    async deleteLoppukeskustelu({ dispatch, commit }, loppukeskusteluLomake) {
      commit('formRequest')
      try {
        await api.deleteLoppukeskustelu(loppukeskusteluLomake)
        commit('formSuccess')
        await dispatch('getKoejakso')
      } catch (err) {
        commit('formError')
        throw err
      }
    },
    async postVastuuhenkilonArvio({ dispatch, commit }, vastuuhenkilonArvioLomake) {
      commit('formRequest')
      try {
        await api.postVastuuhenkilonArvio(vastuuhenkilonArvioLomake)
        commit('formSuccess')
        await dispatch('getKoejakso')
      } catch (err) {
        commit('formError')
        throw err
      }
    },
    async putVastuuhenkilonArvio({ dispatch, commit }, vastuuhenkilonArvioLomake) {
      commit('formRequest')
      try {
        await api.putVastuuhenkilonArvio(vastuuhenkilonArvioLomake)
        commit('formSuccess')
        await dispatch('getKoejakso')
      } catch (err) {
        commit('formError')
        throw err
      }
    },
    async getKouluttajat({ commit }) {
      commit('kouluttajatRequest')
      try {
        const { data } = await api.getKouluttajat()
        commit(
          'kouluttajatSuccess',
          data.sort((a, b) => sortByAsc(a.sukunimi, b.sukunimi))
        )
      } catch (err) {
        commit('kouluttajatError')
        throw err
      }
    },
    async getKouluttajatJaVastuuhenkilot({ commit }) {
      commit('kouluttajatJaVastuuhenkilotRequest')
      try {
        const { data } = await api.getKouluttajatJaVastuuhenkilot()
        commit(
          'kouluttajatJaVastuuhenkilotSuccess',
          data.sort((a, b) => sortByAsc(a.sukunimi, b.sukunimi))
        )
      } catch (err) {
        commit('kouluttajatJaVastuuhenkilotError')
        throw err
      }
    }
  },
  getters: {
    status: (state) => state.status,
    koejakso: (state) => state.koejakso,
    kouluttajat: (state) => state.kouluttajat,
    kouluttajatJaVastuuhenkilot: (state) => state.kouluttajatJaVastuuhenkilot
  }
}

export default erikoistuva
