<template>
  <div class="lisaa-paivittainen-merkinta">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('lisaa-merkinta') }}</h1>
          <p>{{ $t('paivittaiset-merkinnat-ingressi') }}</p>
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

  import { getPaivakirjamerkintaLomake, postPaivakirjamerkinta } from '@/api/erikoistuva'
  import PaivittainenMerkintaForm from '@/forms/paivittainen-merkinta-form.vue'
  import { Paivakirjamerkinta, PaivakirjamerkintaLomake } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      PaivittainenMerkintaForm
    }
  })
  export default class UusiPaivittainenMerkinta extends Vue {
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
        text: this.$t('lisaa-paivitatinen-merkinta'),
        active: true
      }
    ]
    paivakirjamerkintaLomake: PaivakirjamerkintaLomake | null = null
    paivakirjamerkinta: Partial<Paivakirjamerkinta> | null = {
      oppimistapahtumanNimi: null,
      muunAiheenNimi: null,
      reflektio: null,
      yksityinen: false,
      teoriakoulutus: null,
      aihekategoriat: []
    }
    loading = true

    async mounted() {
      await this.fetchLomake()
      if (this.$route?.params?.teoriakoulutusId) {
        const teoriakoulutus = this.teoriakoulutukset.find(
          (el) => el.id === Number(this.$route.params.teoriakoulutusId)
        )
        const aihekategoria = this.aihekategoriat.find((el) => el.teoriakoulutus)

        if (teoriakoulutus && aihekategoria) {
          this.paivakirjamerkinta = {
            oppimistapahtumanNimi: null,
            muunAiheenNimi: null,
            reflektio: null,
            yksityinen: false,
            teoriakoulutus,
            aihekategoriat: [aihekategoria]
          }
        }
      }
      this.loading = false
    }

    async fetchLomake() {
      try {
        this.paivakirjamerkintaLomake = (await getPaivakirjamerkintaLomake()).data
      } catch {
        toastFail(this, this.$t('paivittaisen-merkinnan-lomakkeen-hakeminen-epaonnistui'))
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
        const paivakirjamerkinta = (await postPaivakirjamerkinta(value)).data
        toastSuccess(this, this.$t('paivittainen-merkinta-lisatty-onnistuneesti'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'paivittainen-merkinta',
          params: {
            paivakirjamerkintaId: `${paivakirjamerkinta?.id}`
          }
        })
      } catch (err) {
        toastFail(this, this.$t('uuden-paivittaisen-merkinnan-lisaaminen-epaonnistui'))
      }
      params.saving = false
    }

    onCancel() {
      this.$router.push({
        name: 'paivittaiset-merkinnat'
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
  .lisaa-paivittainen-merkinta {
    max-width: 768px;
  }
</style>
