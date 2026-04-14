<template>
  <div class="lisaa-koulutusjakso">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <div v-if="!loading" class="mb-4">
            <h1>{{ $t('lisaa-koulutusjakso') }}</h1>
            <p>{{ $t('koulutusjakso-lisays-ingressi-1') }}</p>
            <p>{{ $t('koulutusjakso-lisays-ingressi-2') }}</p>
            <hr />
            <koulutusjakso-form
              :tyoskentelyjaksot="tyoskentelyjaksot"
              :kunnat="kunnat"
              :arvioitavan-kokonaisuuden-kategoriat="arvioitavanKokonaisuudenKategoriat"
              @submit="onSubmit"
              @cancel="onCancel"
              @skipRouteExitConfirm="skipRouteExitConfirm"
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
  import { Vue, Component } from 'vue-property-decorator'

  import { getKoulutusjaksoLomake, postKoulutusjakso } from '@/api/erikoistuva'
  import KoulutusjaksoForm from '@/forms/koulutusjakso-form.vue'
  import { Koulutusjakso, KoulutusjaksoLomake } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      KoulutusjaksoForm
    }
  })
  export default class UusiKoulutusjakso extends Vue {
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
        text: this.$t('lisaa-koulutusjakso'),
        active: true
      }
    ]
    koulutusjaksoLomake: null | KoulutusjaksoLomake = null
    loading = true

    async mounted() {
      await this.fetchLomake()
      this.loading = false
    }

    async fetchLomake() {
      try {
        this.koulutusjaksoLomake = (await getKoulutusjaksoLomake()).data
      } catch {
        toastFail(this, this.$t('koulutusjakson-lomakkeen-hakeminen-epaonnistui'))
      }
    }

    async onSubmit(data: Koulutusjakso, params: { saving: boolean }) {
      params.saving = true
      try {
        const koulutusjakso = (await postKoulutusjakso(data)).data
        toastSuccess(this, this.$t('koulutusjakso-lisatty'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'koulutusjakso',
          params: {
            koulutusjaksoId: `${koulutusjakso?.id}`
          }
        })
      } catch {
        toastFail(this, this.$t('uuden-koulutusjakson-lisaaminen-epaonnistui'))
      }
      params.saving = false
    }

    onCancel() {
      this.$router.push({
        name: 'koulutussuunnitelma'
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
  .lisaa-koulutusjakso {
    max-width: 768px;
  }
</style>
