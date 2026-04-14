<template>
  <div class="muokkaa-paivittaista-merkintaa">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('muokkaa-merkintaa') }}</h1>
          <hr />
          <paivittainen-merkinta-form
            v-if="!loading"
            :value="paivakirjamerkinta"
            :aihekategoriat="aihekategoriat"
            :teoriakoulutukset="teoriakoulutukset"
            @submit="onSubmit"
            @cancel="onCancel"
            @skipRouteExitConfirm="skipRouteExitConfirm"
          />
          <div v-else class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import {
    getPaivakirjamerkinta,
    getPaivakirjamerkintaLomake,
    putPaivakirjamerkinta
  } from '@/api/erikoistuva'
  import PaivittainenMerkintaForm from '@/forms/paivittainen-merkinta-form.vue'
  import { Paivakirjamerkinta, PaivakirjamerkintaLomake } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      PaivittainenMerkintaForm
    }
  })
  export default class MuokkaaPaivittaistaMerkintaa extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('paivittaiset-merkinnat'),
        to: { name: 'paivittaiset-merkinnat' }
      },
      {
        text: this.$t('muokkaa-merkintaa'),
        active: true
      }
    ]
    paivakirjamerkintaLomake: PaivakirjamerkintaLomake | null = null
    paivakirjamerkinta: Paivakirjamerkinta | null = null
    loading = true

    async mounted() {
      await Promise.all([this.fetchLomake(), this.fetchPaivakirjamerkinta()])
      this.loading = false
    }

    async fetchLomake() {
      try {
        this.paivakirjamerkintaLomake = (await getPaivakirjamerkintaLomake()).data
      } catch {
        toastFail(this, this.$t('paivittaisen-merkinnan-lomakkeen-hakeminen-epaonnistui'))
      }
    }

    async fetchPaivakirjamerkinta() {
      try {
        this.paivakirjamerkinta = (
          await getPaivakirjamerkinta(this.$route?.params?.paivakirjamerkintaId)
        ).data
      } catch (err) {
        toastFail(this, this.$t('paivittaisen-merkinnan-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'paivittaiset-merkinnat' })
      }
    }

    async onSubmit(
      value: Paivakirjamerkinta,
      params: {
        saving: boolean
      }
    ) {
      params.saving = true
      try {
        const paivakirjamerkinta = (await putPaivakirjamerkinta(value)).data
        toastSuccess(this, this.$t('paivittaisen-merkinnan-muokkaus-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'paivittainen-merkinta',
          params: {
            paivakirjamerkintaId: `${paivakirjamerkinta?.id}`
          }
        })
      } catch (err) {
        toastFail(this, this.$t('paivittaisen-merkinnan-muokkaaminen-epaonnistui'))
      }
      params.saving = false
    }

    onCancel() {
      this.$router.push({
        name: 'paivittainen-merkinta',
        params: { paivakirjamerkintaId: this.$route?.params?.paivakirjamerkintaId }
      })
    }

    get aihekategoriat() {
      return this.paivakirjamerkintaLomake?.aihekategoriat ?? []
    }

    get teoriakoulutukset() {
      return this.paivakirjamerkintaLomake?.teoriakoulutukset ?? []
    }

    skipRouteExitConfirm(value: boolean) {
      this.$emit('skipRouteExitConfirm', value)
    }
  }
</script>

<style lang="scss" scoped>
  .muokkaa-paivittaista-merkintaa {
    max-width: 768px;
  }
</style>
