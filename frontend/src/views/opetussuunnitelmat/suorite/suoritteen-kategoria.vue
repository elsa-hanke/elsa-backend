<template>
  <div class="kategoria mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('suoritteen-kategoria') }}</h1>
          <hr />
          <div v-if="!loading && kategoria">
            <suoritteen-kategoria-form :kategoria="kategoria" />
            <div class="d-flex flex-row-reverse flex-wrap">
              <elsa-button
                :to="{ name: 'muokkaa-suoritteen-kategoriaa' }"
                variant="outline-primary"
                class="ml-2 mb-3"
              >
                {{ $t('muokkaa-kategoriaa') }}
              </elsa-button>
              <elsa-button
                :to="{
                  name: 'lisaa-suorite',
                  params: { kategoriaId: kategoria.id }
                }"
                variant="primary"
                class="ml-2 mb-3"
              >
                {{ $t('lisaa-suorite') }}
              </elsa-button>
              <elsa-button
                :to="{ name: 'erikoisala', hash: '#suoritteet' }"
                variant="link"
                class="mb-3 mr-auto font-weight-500 suorite-link"
              >
                {{ $t('palaa-suoritteisiin') }}
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

  import { getSuoritteenKategoria } from '@/api/tekninen-paakayttaja'
  import ElsaButton from '@/components/button/button.vue'
  import SuoritteenKategoriaForm from '@/forms/suoritteen-kategoria-form.vue'
  import { SuoritteenKategoriaWithErikoisala } from '@/types'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      SuoritteenKategoriaForm
    }
  })
  export default class SuoritteenKategoriaView extends Vue {
    kategoria: SuoritteenKategoriaWithErikoisala | null = null

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
        this.kategoria = (await getSuoritteenKategoria(this.$route?.params?.kategoriaId)).data
      } catch (err) {
        toastFail(this, this.$t('suoritteen-kategorian-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'erikoisala', hash: '#suoritteet' })
      }
    }
  }
</script>

<style lang="scss" scoped>
  .kategoria {
    max-width: 970px;
  }

  .suorite-link::before {
    content: '<';
    position: absolute;
    left: 1rem;
  }
</style>
