<template>
  <div class="kokonaisuus mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('muokkaa-arvioitavaa-kokonaisuutta') }}</h1>
          <b-alert variant="danger" show>
            <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="mr-1" />
            {{ $t('arvioitava-kokonaisuus-muokkaus-huomio') }}
          </b-alert>
          <hr />
          <arvioitava-kokonaisuus-form
            v-if="!loading"
            :editing="true"
            :kokonaisuus="kokonaisuus"
            :kategoriat="kategoriat"
            @submit="onSubmit"
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
  import { AxiosError } from 'axios'
  import { Component, Vue } from 'vue-property-decorator'

  import {
    getArvioitavaKokonaisuus,
    getArvioitavanKokonaisuudenKategoriat,
    putArvioitavaKokonaisuus
  } from '@/api/tekninen-paakayttaja'
  import ElsaButton from '@/components/button/button.vue'
  import ArvioitavaKokonaisuusForm from '@/forms/arvioitava-kokonaisuus-form.vue'
  import {
    ArvioitavaKokonaisuusWithErikoisala,
    ArvioitavanKokonaisuudenKategoria,
    ElsaError
  } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ArvioitavaKokonaisuusForm,
      ElsaButton
    }
  })
  export default class MuokkaaArvioitavaaKokonaisuutta extends Vue {
    kokonaisuus: ArvioitavaKokonaisuusWithErikoisala | null = null

    kategoriat: ArvioitavanKokonaisuudenKategoria[] | null = null

    loading = true

    get items() {
      return [
        {
          text: this.$t('etusivu'),
          to: { name: 'etusivu' }
        },
        {
          text: this.$t('opetussuunnitelmat'),
          to: { name: 'opetussuunnitelmat' }
        },
        {
          text: this.kokonaisuus?.kategoria?.erikoisala.nimi,
          to: { name: 'erikoisala' }
        },
        {
          text: this.$t('muokkaa-arvioitavaa-kokonaisuutta'),
          active: true
        }
      ]
    }

    async mounted() {
      await Promise.all([this.fetchKokonaisuus(), this.fetchKategoriat()])
      this.loading = false
    }

    async fetchKokonaisuus() {
      try {
        this.kokonaisuus = (await getArvioitavaKokonaisuus(this.$route?.params?.kokonaisuusId)).data
      } catch (err) {
        toastFail(this, this.$t('arvioitavan-kokonaisuuden-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'opetussuunnitelmat', hash: '#arvioitavat-kokonaisuudet' })
      }
    }

    async fetchKategoriat() {
      try {
        this.kategoriat = (
          await getArvioitavanKokonaisuudenKategoriat(this.$route.params.erikoisalaId)
        ).data
      } catch (err) {
        toastFail(this, this.$t('kategorioiden-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'opetussuunnitelmat', hash: '#arvioitavat-kokonaisuudet' })
      }
    }

    async onSubmit(value: ArvioitavaKokonaisuusWithErikoisala, params: { saving: boolean }) {
      params.saving = true
      try {
        await putArvioitavaKokonaisuus(value)
        toastSuccess(this, this.$t('arvioitavan-kokonaisuuden-tallentaminen-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({ name: 'arvioitava-kokonaisuus' })
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('arvioitavan-kokonaisuuden-tallentaminen-epaonnistui')}: ${this.$t(
                message
              )}`
            : this.$t('arvioitavan-kokonaisuuden-tallentaminen-epaonnistui')
        )
      }
      params.saving = false
    }
  }
</script>

<style lang="scss" scoped>
  .kokonaisuus {
    max-width: 970px;
  }
</style>
