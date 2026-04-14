<template>
  <div class="kokonaisuus mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('lisaa-arvioitava-kokonaisuus') }}</h1>
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
    getArvioitavanKokonaisuudenKategoriat,
    getErikoisala,
    postArvioitavaKokonaisuus
  } from '@/api/tekninen-paakayttaja'
  import ElsaButton from '@/components/button/button.vue'
  import ArvioitavaKokonaisuusForm from '@/forms/arvioitava-kokonaisuus-form.vue'
  import {
    ArvioitavaKokonaisuusWithErikoisala,
    ArvioitavanKokonaisuudenKategoria,
    ElsaError,
    Erikoisala,
    UusiArvioitavaKokonaisuus
  } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ArvioitavaKokonaisuusForm,
      ElsaButton
    }
  })
  export default class LisaaArvioitavaKokonaisuus extends Vue {
    kokonaisuus: UusiArvioitavaKokonaisuus = {
      nimi: null,
      nimiSv: null,
      voimassaoloAlkaa: null,
      voimassaoloLoppuu: null,
      kuvaus: null,
      kuvausSv: null,
      kategoria: null
    }

    erikoisala: Erikoisala | null = null

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
          text: this.erikoisala?.nimi,
          to: { name: 'erikoisala' }
        },
        {
          text: this.$t('lisaa-arvioitava-kokonaisuus'),
          active: true
        }
      ]
    }

    async mounted() {
      await Promise.all([this.fetchErikoisala(), this.fetchKategoriat()])
      const kategoria = this.kategoriat?.find(
        (k) => k.id === parseInt(this.$route.params.kategoriaId)
      )
      if (kategoria != null) {
        this.kokonaisuus.kategoria = {
          ...kategoria,
          erikoisala: this.erikoisala as Erikoisala
        }
      }
      this.loading = false
    }

    async fetchErikoisala() {
      try {
        this.erikoisala = (await getErikoisala(this.$route.params.erikoisalaId)).data
      } catch (err) {
        toastFail(this, this.$t('erikoisalan-hakeminen-epaonnistui'))
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
        const uusiKokonaisuus = (await postArvioitavaKokonaisuus(value)).data
        toastSuccess(this, this.$t('arvioitavan-kokonaisuuden-tallentaminen-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'arvioitava-kokonaisuus',
          params: { kokonaisuusId: `${uusiKokonaisuus.id}` }
        })
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
