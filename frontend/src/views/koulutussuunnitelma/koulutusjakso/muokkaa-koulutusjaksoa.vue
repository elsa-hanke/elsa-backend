<template>
  <div class="muokkaa-koulutusjaksoa">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('muokkaa-koulutusjaksoa') }}</h1>
          <hr />
          <koulutusjakso-form
            v-if="!loading"
            :value="koulutusjakso"
            :tyoskentelyjaksot="tyoskentelyjaksot"
            :kunnat="kunnat"
            :arvioitavan-kokonaisuuden-kategoriat="arvioitavanKokonaisuudenKategoriat"
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
  import { Vue, Component } from 'vue-property-decorator'

  import { getKoulutusjakso, getKoulutusjaksoLomake, putKoulutusjakso } from '@/api/erikoistuva'
  import KoulutusjaksoForm from '@/forms/koulutusjakso-form.vue'
  import { Koulutusjakso, KoulutusjaksoLomake } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      KoulutusjaksoForm
    }
  })
  export default class MuokkaaKoulutusjaksoa extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('koulutussuunnitelma'),
        to: { name: 'koulutussuunnitelma' }
      },
      {
        text: this.$t('muokkaa-koulutusjaksoa'),
        active: true
      }
    ]
    koulutusjaksoLomake: KoulutusjaksoLomake | null = null
    koulutusjakso: Koulutusjakso | null = null
    loading = true

    async mounted() {
      await Promise.all([this.fetchLomake(), this.fetchKoulutusjakso()])
      if (this.koulutusjakso != null && this.koulutusjakso.lukittu) {
        toastFail(this, this.$t('koulutusjakso-on-lukittu'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'koulutusjakso',
          params: { koulutusjaksoId: this.$route.params.koulutusjaksoId }
        })
      }
      this.loading = false
    }

    async fetchLomake() {
      try {
        this.koulutusjaksoLomake = (await getKoulutusjaksoLomake()).data
      } catch {
        toastFail(this, this.$t('koulutusjakson-lomakkeen-hakeminen-epaonnistui'))
      }
    }

    async fetchKoulutusjakso() {
      try {
        this.koulutusjakso = (await getKoulutusjakso(this.$route?.params?.koulutusjaksoId)).data
      } catch {
        toastFail(this, this.$t('koulutusjakson-hakeminen-epaonnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.replace({ name: 'koulutussuunnitelma' })
      }
    }

    async onSubmit(data: Koulutusjakso, params: { saving: boolean }) {
      params.saving = true
      try {
        const koulutusjakso = (await putKoulutusjakso(data)).data
        toastSuccess(this, this.$t('koulutusjakso-tallennettu'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'koulutusjakso',
          params: {
            koulutusjaksoId: `${koulutusjakso?.id}`
          }
        })
      } catch {
        toastFail(this, this.$t('koulutusjakson-muokkaus-epaonnistui'))
      }
      params.saving = false
    }

    onCancel() {
      this.$router.push({
        name: 'koulutusjakso',
        params: {
          koulutusjaksoId: `${this.koulutusjakso?.id}`
        }
      })
    }

    get tyoskentelyjaksot() {
      if (this.koulutusjaksoLomake) {
        return this.koulutusjaksoLomake.tyoskentelyjaksot
      } else {
        return []
      }
    }

    get kunnat() {
      if (this.koulutusjaksoLomake) {
        return this.koulutusjaksoLomake.kunnat
      } else {
        return []
      }
    }

    get arvioitavanKokonaisuudenKategoriat() {
      return this.koulutusjaksoLomake?.arvioitavanKokonaisuudenKategoriat ?? []
    }

    skipRouteExitConfirm(value: boolean) {
      this.$emit('skipRouteExitConfirm', value)
    }
  }
</script>

<style lang="scss" scoped>
  .muokkaa-koulutusjaksoa {
    max-width: 970px;
  }
</style>
