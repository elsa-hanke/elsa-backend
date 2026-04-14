<template>
  <div class="muokkaa-suoritemerkintaa">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('muokkaa-suoritemerkintaa') }}</h1>
          <hr />
          <suoritemerkinta-form
            v-if="!loading && suoritemerkinta"
            :value="suoritemerkintaWrapper"
            :tyoskentelyjaksot="tyoskentelyjaksot"
            :kunnat="kunnat"
            :erikoisalat="erikoisalat"
            :arviointiasteikko="suoritemerkinta.arviointiasteikko"
            :arviointiasteikon-taso="arviointiasteikonTaso"
            @submit="onSubmit"
            @delete="onDelete"
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
  import axios from 'axios'
  import { Component, Vue } from 'vue-property-decorator'

  import SuoritemerkintaForm from '@/forms/suoritemerkinta-form.vue'
  import { Suoritemerkinta, SuoritemerkintaLomake } from '@/types'
  import { confirmDelete } from '@/utils/confirm'
  import { toastFail, toastSuccess } from '@/utils/toast'
  import { tyoskentelyjaksoLabel } from '@/utils/tyoskentelyjakso'

  @Component({
    components: {
      SuoritemerkintaForm
    }
  })
  export default class MuokkaaSuoritemerkintaa extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('suoritemerkinnat'),
        to: { name: 'suoritemerkinnat' }
      },
      {
        text: this.$t('muokkaa-suoritemerkintaa'),
        active: true
      }
    ]
    suoritemerkintaLomake: null | SuoritemerkintaLomake = null
    suoritemerkinta: Suoritemerkinta | null = null
    loading = true

    async mounted() {
      await Promise.all([this.fetchLomake(), this.fetchSuoritemerkinta()])
      if (this.suoritemerkinta?.lukittu) {
        toastFail(this, this.$t('suoritemerkinta-on-lukittu'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'suoritemerkinta',
          params: {
            suoritemerkintaId: `${this.suoritemerkinta.id}`
          }
        })
      }
      this.loading = false
    }

    async fetchSuoritemerkinta() {
      const suoritemerkintaId = this.$route?.params?.suoritemerkintaId
      if (suoritemerkintaId) {
        try {
          this.suoritemerkinta = (
            await axios.get(`erikoistuva-laakari/suoritemerkinnat/${suoritemerkintaId}`)
          ).data
        } catch {
          toastFail(this, this.$t('suoritemerkinnan-hakeminen-epaonnistui'))
          this.$emit('skipRouteExitConfirm', true)
          this.$router.replace({ name: 'suoritemerkinnat' })
        }
      }
    }

    async fetchLomake() {
      try {
        this.suoritemerkintaLomake = (
          await axios.get(`erikoistuva-laakari/suoritemerkinta-lomake`)
        ).data
      } catch {
        toastFail(this, this.$t('suoritemerkinnan-lomakkeen-hakeminen-epaonnistui'))
      }
    }

    async onSubmit(
      value: {
        tyoskentelyjaksoId: number
        suoriteId: number
        vaativuustaso: number
        arviointiasteikonTaso: number
        suorituspaiva: string
        lisatiedot: string
      },
      params: {
        saving: boolean
        deleting: boolean
      }
    ) {
      params.saving = true
      try {
        this.suoritemerkinta = (
          await axios.put('erikoistuva-laakari/suoritemerkinnat', {
            id: this.suoritemerkinta?.id,
            tyoskentelyjaksoId: value.tyoskentelyjaksoId,
            suoriteId: value.suoriteId,
            vaativuustaso: value.vaativuustaso,
            arviointiasteikonTaso: value.arviointiasteikonTaso,
            suorituspaiva: value.suorituspaiva,
            lisatiedot: value.lisatiedot
          })
        ).data
        toastSuccess(this, this.$t('suoritemerkinnan-tallentaminen-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'suoritemerkinta',
          params: {
            suoritemerkintaId: `${this.suoritemerkinta?.id}`
          }
        })
      } catch {
        toastFail(this, this.$t('suoritemerkinnan-tallentaminen-epaonnistui'))
      }
      params.saving = false
    }

    async onDelete(params: { saving: boolean; deleting: boolean }) {
      if (
        await confirmDelete(
          this,
          this.$t('poista-suoritemerkinta') as string,
          (this.$t('suoritemerkinnan') as string).toLowerCase()
        )
      ) {
        params.deleting = true
        try {
          await axios.delete(`erikoistuva-laakari/suoritemerkinnat/${this.suoritemerkinta?.id}`)
          toastSuccess(this, this.$t('suoritemerkinta-poistettu-onnistuneesti'))
          this.$emit('skipRouteExitConfirm', true)
          this.$router.push({
            name: 'suoritemerkinnat'
          })
        } catch {
          toastFail(this, this.$t('suoritemerkinnan-poistaminen-epaonnistui'))
        }
        params.deleting = false
      }
    }

    get tyoskentelyjaksot() {
      if (this.suoritemerkintaLomake) {
        return this.suoritemerkintaLomake.tyoskentelyjaksot
      } else {
        return []
      }
    }

    get kunnat() {
      if (this.suoritemerkintaLomake) {
        return this.suoritemerkintaLomake.kunnat
      } else {
        return []
      }
    }

    get erikoisalat() {
      if (this.suoritemerkintaLomake) {
        return this.suoritemerkintaLomake.erikoisalat
      } else {
        return []
      }
    }

    get oppimistavoitteenKategoriat() {
      if (this.suoritemerkintaLomake) {
        return this.suoritemerkintaLomake.suoritteenKategoriat.map((kategoria) => ({
          ...kategoria,
          nimi: `${kategoria.nimi} / ${(this.$t('toimenpiteet') as string).toLowerCase()}`
        }))
      } else {
        return []
      }
    }

    get arviointiasteikonTaso() {
      return this.suoritemerkinta?.arviointiasteikko?.tasot.find(
        (taso) => taso.taso === this.suoritemerkinta?.arviointiasteikonTaso
      )
    }

    get suoritemerkintaWrapper() {
      if (this.suoritemerkinta) {
        return {
          ...this.suoritemerkinta,
          tyoskentelyjakso: {
            ...this.suoritemerkinta.tyoskentelyjakso,
            label: tyoskentelyjaksoLabel(this, this.suoritemerkinta.tyoskentelyjakso)
          }
        }
      } else {
        return undefined
      }
    }

    skipRouteExitConfirm(value: boolean) {
      this.$emit('skipRouteExitConfirm', value)
    }
  }
</script>

<style lang="scss" scoped>
  .muokkaa-suoritemerkintaa {
    max-width: 970px;
  }
</style>
