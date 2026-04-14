<template>
  <div class="suorite mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('muokkaa-suoritetta') }}</h1>
          <b-alert variant="danger" show>
            <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="mr-1" />
            {{ $t('suorite-muokkaus-huomio') }}
          </b-alert>
          <hr />
          <suorite-form
            v-if="!loading"
            :editing="true"
            :suorite="suorite"
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

  import { getSuorite, getSuoritteenKategoriat, putSuorite } from '@/api/tekninen-paakayttaja'
  import ElsaButton from '@/components/button/button.vue'
  import SuoriteForm from '@/forms/suorite-form.vue'
  import { ElsaError, SuoriteWithErikoisala, SuoritteenKategoria } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      SuoriteForm
    }
  })
  export default class MuokkaaSuoritetta extends Vue {
    suorite: SuoriteWithErikoisala | null = null

    kategoriat: SuoritteenKategoria[] | null = null

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
          text: this.suorite?.kategoria?.erikoisala.nimi,
          to: { name: 'erikoisala' }
        },
        {
          text: this.$t('muokkaa-suoritetta'),
          active: true
        }
      ]
    }

    async mounted() {
      await Promise.all([this.fetchSuorite(), this.fetchKategoriat()])
      this.loading = false
    }

    async fetchSuorite() {
      try {
        this.suorite = (await getSuorite(this.$route?.params?.suoriteId)).data
      } catch (err) {
        toastFail(this, this.$t('suoritteen-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'opetussuunnitelmat', hash: '#suoritteet' })
      }
    }

    async fetchKategoriat() {
      try {
        this.kategoriat = (await getSuoritteenKategoriat(this.$route.params.erikoisalaId)).data
      } catch (err) {
        toastFail(this, this.$t('kategorioiden-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'opetussuunnitelmat', hash: '#suoritteet' })
      }
    }

    async onSubmit(value: SuoriteWithErikoisala, params: { saving: boolean }) {
      params.saving = true
      try {
        await putSuorite(value)
        toastSuccess(this, this.$t('suoritteen-tallentaminen-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({ name: 'suorite' })
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('suoritteen-tallentaminen-epaonnistui')}: ${this.$t(message)}`
            : this.$t('suoritteen-tallentaminen-epaonnistui')
        )
      }
      params.saving = false
    }
  }
</script>

<style lang="scss" scoped>
  .suorite {
    max-width: 970px;
  }
</style>
