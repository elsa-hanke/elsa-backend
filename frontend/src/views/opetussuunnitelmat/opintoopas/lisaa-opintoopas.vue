<template>
  <div class="opintoopas mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('lisaa-opintoopas') }}</h1>
          <hr />
          <div v-if="!loading">
            <yek-opintoopas-form
              v-if="onkoYEK"
              :opas="opas"
              :erikoisala-id="$route.params.erikoisalaId"
              :editing="true"
              @submit="onSubmit"
            />
            <opintoopas-form
              v-else
              :opas="opas"
              :erikoisala-id="$route.params.erikoisalaId"
              :arviointiasteikot="arviointiasteikot"
              :editing="true"
              @submit="onSubmit"
            />
          </div>
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
  import Avatar from 'vue-avatar'
  import { Component, Vue } from 'vue-property-decorator'
  import { required } from 'vuelidate/lib/validators'

  import { getArviointiasteikot, getErikoisala, postOpinoopas } from '@/api/tekninen-paakayttaja'
  import ElsaButton from '@/components/button/button.vue'
  import OpintoopasForm from '@/forms/opintoopas-form.vue'
  import YekOpintoopasForm from '@/forms/yek-opintoopas-form.vue'
  import { Arviointiasteikko, ElsaError, Opintoopas, UusiOpintoopas } from '@/types'
  import { ERIKOISALA_YEK_ID } from '@/utils/constants'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      Avatar,
      ElsaButton,
      OpintoopasForm,
      YekOpintoopasForm
    },
    validations: {
      form: {
        email: {
          required
        }
      }
    }
  })
  export default class LisaaOpintoopas extends Vue {
    opas: UusiOpintoopas = {
      nimi: null,
      nimiSv: null,
      voimassaoloAlkaa: null,
      voimassaoloPaattyy: null,
      kaytannonKoulutuksenVahimmaispituusVuodet: null,
      kaytannonKoulutuksenVahimmaispituusKuukaudet: null,
      terveyskeskuskoulutusjaksonVahimmaispituusVuodet: null,
      terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet: 9,
      terveyskeskuskoulutusjaksonMaksimipituusKuukaudet: this.onkoYEK ? null : 9,
      yliopistosairaalajaksonVahimmaispituusVuodet: null,
      yliopistosairaalajaksonVahimmaispituusKuukaudet: null,
      yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet: null,
      yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet: null,
      erikoisalanVaatimaJohtamisopintojenVahimmaismaara: null,
      erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara: 10,
      erikoisalanVaatimaTeoriakoulutustenVahimmaismaara: null,
      erikoisala: null,
      arviointiasteikkoId: null
    }

    arviointiasteikot: Arviointiasteikko[] = []

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
          text: this.opas.erikoisala?.nimi,
          to: { name: 'erikoisala' }
        },
        {
          text: this.$t('opintoopas'),
          active: true
        }
      ]
    }

    async mounted() {
      await Promise.all([this.fetchArviointiasteikot(), this.fetchErikoisala()])
      this.loading = false
    }

    async fetchErikoisala() {
      try {
        this.opas.erikoisala = (await getErikoisala(this.$route.params.erikoisalaId)).data

        if (this.$route.params.erikoisalaId == ERIKOISALA_YEK_ID.toString()) {
          this.opas.erikoisalanVaatimaJohtamisopintojenVahimmaismaara = 0
          this.opas.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara = 0
        }
      } catch (err) {
        toastFail(this, this.$t('erikoisalan-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'opetussuunnitelmat' })
      }
    }

    async fetchArviointiasteikot() {
      try {
        this.arviointiasteikot = (await getArviointiasteikot()).data

        if (this.$route.params.erikoisalaId == ERIKOISALA_YEK_ID.toString()) {
          this.opas.arviointiasteikkoId = this.arviointiasteikot[0].id || null
        }
      } catch (err) {
        toastFail(this, this.$t('arviointiasteikkojen-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'opetussuunnitelmat' })
      }
    }

    async onSubmit(value: Opintoopas, params: { saving: boolean }) {
      params.saving = true
      try {
        await postOpinoopas(value)
        toastSuccess(this, this.$t('opintooppaan-tallentaminen-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'erikoisala'
        })
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('opintooppaan-tallentaminen-epaonnistui')}: ${this.$t(message)}`
            : this.$t('opintooppaan-tallentaminen-epaonnistui')
        )
      }
      params.saving = false
    }

    get onkoYEK() {
      return this.$route.params.erikoisalaId == ERIKOISALA_YEK_ID.toString()
    }
  }
</script>

<style lang="scss" scoped>
  .opintoopas {
    max-width: 970px;
  }
</style>
