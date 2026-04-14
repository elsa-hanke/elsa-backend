<template>
  <div class="lisaa-suoritemerkinta">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('lisaa-suoritemerkinta') }}</h1>
          <p class="mb-0">{{ $t('suoritteet-kuitataan-seurantakeskusteluissa') }}</p>
          <hr />
          <suoritemerkinta-form
            v-if="!loading && suoritemerkintaLomake"
            :tyoskentelyjaksot="tyoskentelyjaksot"
            :suoritteen-kategoriat="suoritemerkintaLomake.suoritteenKategoriat"
            :arviointiasteikko="suoritemerkintaLomake.arviointiasteikko"
            :kunnat="kunnat"
            :erikoisalat="erikoisalat"
            @submit="onSubmit"
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
  import {
    ArviointiasteikonTaso,
    SuoritemerkinnanSuorite,
    Suoritemerkinta,
    SuoritemerkintaLomake,
    Vaativuustaso
  } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      SuoritemerkintaForm
    }
  })
  export default class UusiSuoritemerkinta extends Vue {
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
        text: this.$t('lisaa-suoritemerkinta'),
        active: true
      }
    ]
    suoritemerkintaLomake: null | SuoritemerkintaLomake = null
    suoritemerkinta: Suoritemerkinta | null = null
    loading = true

    async mounted() {
      await this.fetchLomake()
      this.loading = false
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
      value: Suoritemerkinta,
      params: {
        saving: boolean
        deleting: boolean
      },
      uudetSuoritteet: SuoritemerkinnanSuorite[]
    ) {
      params.saving = true
      try {
        this.suoritemerkinta = (
          await axios.post('erikoistuva-laakari/suoritemerkinnat', {
            suorituspaiva: value.suorituspaiva,
            lisatiedot: value.lisatiedot,
            tyoskentelyjaksoId: value.tyoskentelyjaksoId,
            suoritteet: uudetSuoritteet.map((s) => {
              return {
                vaativuustaso: (s.vaativuustaso as Vaativuustaso)?.arvo,
                arviointiasteikonTaso: (s.arviointiasteikonTaso as ArviointiasteikonTaso)?.taso,
                suoriteId: s.suorite?.id
              }
            })
          })
        ).data
        toastSuccess(this, this.$t('suoritusmerkinnat-lisatty-onnistuneesti'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'suoritemerkinnat'
        })
      } catch {
        toastFail(this, this.$t('uuden-suoritemerkinnan-lisaaminen-epaonnistui'))
      }
      params.saving = false
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

    skipRouteExitConfirm(value: boolean) {
      this.$emit('skipRouteExitConfirm', value)
    }
  }
</script>

<style lang="scss" scoped>
  .lisaa-suoritemerkinta {
    max-width: 970px;
  }
</style>
