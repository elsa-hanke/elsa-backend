<template>
  <div class="opintoopas mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('muokkaa-opintoopasta') }}</h1>
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
  import { Component, Vue } from 'vue-property-decorator'
  import { required } from 'vuelidate/lib/validators'

  import { getArviointiasteikot, getOpinoopas, putOpinoopas } from '@/api/tekninen-paakayttaja'
  import OpintoopasForm from '@/forms/opintoopas-form.vue'
  import YekOpintoopasForm from '@/forms/yek-opintoopas-form.vue'
  import { Arviointiasteikko, ElsaError, Opintoopas } from '@/types'
  import { ERIKOISALA_YEK_ID } from '@/utils/constants'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
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
  export default class MuokkaaOpintoopasta extends Vue {
    opas: Opintoopas | null = null

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
          text: this.opas?.erikoisala.nimi,
          to: { name: 'erikoisala' }
        },
        {
          text: this.$t('muokkaa-opintoopasta'),
          active: true
        }
      ]
    }

    async mounted() {
      await this.fetchOpas()
      await this.fetchArviointiasteikot()
      this.loading = false
    }

    async fetchOpas() {
      try {
        this.opas = (await getOpinoopas(this.$route?.params?.opintoopasId)).data
      } catch (err) {
        toastFail(this, this.$t('opintooppaan-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'opetussuunnitelmat' })
      }
    }

    async fetchArviointiasteikot() {
      try {
        this.arviointiasteikot = (await getArviointiasteikot()).data
      } catch (err) {
        toastFail(this, this.$t('arviointiasteikkojen-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'opetussuunnitelmat' })
      }
    }

    async onSubmit(value: Opintoopas, params: { saving: boolean }) {
      params.saving = true
      try {
        await putOpinoopas(value)
        toastSuccess(this, this.$t('opintooppaan-tallentaminen-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'opintoopas',
          params: { opintoopasId: this.$route.params.opintoopasId }
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

  .opintoopas-link::before {
    content: '<';
    position: absolute;
    left: 1rem;
  }
</style>
