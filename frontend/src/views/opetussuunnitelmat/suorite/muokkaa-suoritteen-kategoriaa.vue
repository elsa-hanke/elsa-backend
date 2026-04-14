<template>
  <div class="kategoria mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('muokkaa-kategoriaa') }}</h1>
          <hr />
          <suoritteen-kategoria-form
            v-if="!loading"
            :editing="true"
            :kategoria="kategoria"
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
  import { Component, Vue } from 'vue-property-decorator'

  import { getSuoritteenKategoria, putSuoritteenKategoria } from '@/api/tekninen-paakayttaja'
  import ElsaButton from '@/components/button/button.vue'
  import SuoritteenKategoriaForm from '@/forms/suoritteen-kategoria-form.vue'
  import { SuoritteenKategoriaWithErikoisala } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      SuoritteenKategoriaForm
    }
  })
  export default class MuokkaaSuoritteenKategoriaa extends Vue {
    loading = true

    kategoria: SuoritteenKategoriaWithErikoisala | null = null

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
          text: this.$t('muokkaa-kategoriaa'),
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
        this.kategoria = (await getSuoritteenKategoria(this.$route.params.kategoriaId)).data
      } catch (err) {
        toastFail(this, this.$t('suoritteen-kategorian-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'suoritteen-kategoria' })
      }
    }

    async onSubmit(value: SuoritteenKategoriaWithErikoisala, params: { saving: boolean }) {
      params.saving = true
      try {
        await putSuoritteenKategoria(value)
        toastSuccess(this, this.$t('kategorian-tallentaminen-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({ name: 'suoritteen-kategoria' })
      } catch (err) {
        toastFail(this, this.$t('kategorian-tallentaminen-epaonnistui'))
      }
      params.saving = false
    }
  }
</script>

<style lang="scss" scoped>
  .kategoria {
    max-width: 970px;
  }
</style>
