<template>
  <div class="kategoria mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('arvioitavan-kokonaisuuden-kategoria') }}</h1>
          <hr />
          <div v-if="!loading && kategoria">
            <arvioitavan-kokonaisuuden-kategoria-form :kategoria="kategoria" />
            <div class="d-flex flex-row-reverse flex-wrap">
              <elsa-button
                :to="{ name: 'muokkaa-arvioitavan-kokonaisuuden-kategoriaa' }"
                variant="outline-primary"
                class="ml-2 mb-3"
              >
                {{ $t('muokkaa-kategoriaa') }}
              </elsa-button>
              <elsa-button
                :to="{
                  name: 'lisaa-arvioitava-kokonaisuus',
                  params: { kategoriaId: kategoria.id }
                }"
                variant="primary"
                class="ml-2 mb-3"
              >
                {{ $t('lisaa-arvioitava-kokonaisuus') }}
              </elsa-button>
              <elsa-button
                :to="{ name: 'erikoisala', hash: '#arvioitavat-kokonaisuudet' }"
                variant="link"
                class="mb-3 mr-auto font-weight-500 arvioitava-kokonaisuus-link"
              >
                {{ $t('palaa-arvioitaviin-kokonaisuuksiin') }}
              </elsa-button>
            </div>
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
  import { Component, Vue } from 'vue-property-decorator'

  import { getArvioitavanKokonaisuudenKategoria } from '@/api/tekninen-paakayttaja'
  import ElsaButton from '@/components/button/button.vue'
  import ArvioitavanKokonaisuudenKategoriaForm from '@/forms/arvioitavan-kokonaisuuden-kategoria-form.vue'
  import { ArvioitavanKokonaisuudenKategoriaWithErikoisala } from '@/types'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      ArvioitavanKokonaisuudenKategoriaForm,
      ElsaButton
    }
  })
  export default class ArvioitavanKokonaisuudenKategoriaView extends Vue {
    kategoria: ArvioitavanKokonaisuudenKategoriaWithErikoisala | null = null

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
          text: this.kategoria?.erikoisala.nimi,
          to: { name: 'erikoisala' }
        },
        {
          text: this.$t('kategoria'),
          active: true
        }
      ]
    }

    async mounted() {
      await this.fetchKategoria()
      this.loading = false
    }

    async fetchKategoria() {
      try {
        this.kategoria = (
          await getArvioitavanKokonaisuudenKategoria(this.$route?.params?.kategoriaId)
        ).data
      } catch (err) {
        toastFail(this, this.$t('arvioitavan-kokonaisuuden-kategorian-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'erikoisala', hash: '#arvioitavat-kokonaisuudet' })
      }
    }
  }
</script>

<style lang="scss" scoped>
  .kategoria {
    max-width: 970px;
  }

  .arvioitava-kokonaisuus-link::before {
    content: '<';
    position: absolute;
    left: 1rem;
  }
</style>
