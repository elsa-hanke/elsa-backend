<template>
  <div class="esittely">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('arviointityokalut') }}</h1>
          <p>{{ $t('arviointityokalut-esittely') }}</p>
        </b-col>
      </b-row>
      <b-row v-if="loading">
        <b-col>
          <div class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
        </b-col>
      </b-row>
      <b-row v-for="(k, index) in kategoriat" :key="index" lg>
        <b-col v-if="getArviontityokalutForKategoria(k.id).length > 0" lg>
          <h3 v-if="k.id" class="mt-3">{{ k.nimi }}</h3>
          <b-row v-for="(at, atIndex) in getArviontityokalutForKategoria(k.id)" :key="atIndex">
            <b-col>
              <elsa-accordian :ref="k.nimi" :visible="false">
                <template #title>
                  {{ at.nimi }}
                </template>
                <div class="mt-3 mb-3">
                  <asiakirjat-content
                    v-if="at.liite && at.liite.data && at.liite.data.length > 0"
                    :asiakirjat="produceLiite(at)"
                    :sorting-enabled="false"
                    :pagination-enabled="false"
                    :enable-search="false"
                    :show-info-if-empty="false"
                    :enable-delete="false"
                    :asiakirja-data-endpoint-url="asiakirjaDataEndpoint()"
                  />
                  <b-alert v-else variant="dark" show>
                    <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
                    <span>
                      {{ $t('ei-liitetiedostoa') }}
                    </span>
                  </b-alert>
                  <!-- eslint-disable-next-line vue/no-v-html -->
                  <div v-html="at.ohjeteksti"></div>
                  <div
                    v-for="(kysymys, kIndex) in at.kysymykset"
                    :key="kysymys.jarjestysnumero"
                    class="mt-4"
                  >
                    <arviointityokalu-lomake-kysymys-form
                      :kysymys="kysymys"
                      :child-data-received="!loading"
                      :answer-mode="false"
                      :index="kIndex"
                      :arviointityokalu-id="at.id"
                    />
                  </div>
                </div>
              </elsa-accordian>
            </b-col>
          </b-row>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { Vue, Component } from 'vue-property-decorator'

  import { getArviointityokalut, getArviointityokaluKategoriat } from '@/api/kouluttaja'
  import {
    getArviointityokaluKategoriatVastuuhenkilo,
    getArviointityokalutVastuuhenkilo
  } from '@/api/vastuuhenkilo'
  import ElsaAccordian from '@/components/accordian/accordian.vue'
  import AsiakirjatContent from '@/components/asiakirjat/asiakirjat-content.vue'
  import ArviointityokaluLomakeKysymysForm from '@/forms/arviointityokalu-lomake-kysymys-form.vue'
  import { Arviointityokalu, ArviointityokaluKategoria, Asiakirja } from '@/types'
  import { MUU_ARVIOINTITYOKALU_ID } from '@/utils/constants'
  import { sortByAsc } from '@/utils/sort'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      AsiakirjatContent,
      ArviointityokaluLomakeKysymysForm,
      ElsaAccordian
    }
  })
  export default class ArviointityokalutEsittely extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('arviointityokalut'),
        active: true
      }
    ]

    arviointityokalut: Arviointityokalu[] = []
    kategoriat: ArviointityokaluKategoria[] = []
    loading = false

    async mounted() {
      this.loading = true
      try {
        this.arviointityokalut = (
          this.$isVastuuhenkilo()
            ? await getArviointityokalutVastuuhenkilo()
            : await getArviointityokalut()
        ).data.sort((a, b) => sortByAsc(a.nimi, b.nimi))
        this.kategoriat = [
          { nimi: '' },
          ...(this.$isVastuuhenkilo()
            ? await getArviointityokaluKategoriatVastuuhenkilo()
            : await getArviointityokaluKategoriat()
          ).data.sort((a, b) => sortByAsc(a.nimi, b.nimi))
        ]
      } catch {
        toastFail(this, this.$t('arviointityokalujen-kategorioiden-hakeminen-epaonnistui'))
        this.arviointityokalut = []
      }
      this.loading = false
    }

    getArviontityokalutForKategoria(id: number | null | undefined) {
      return id != null
        ? this.arviointityokalut
            .filter((item) => item.id !== MUU_ARVIOINTITYOKALU_ID)
            .filter((a) => a.kategoria?.id === id)
        : this.arviointityokalut
            .filter((item) => item.id !== MUU_ARVIOINTITYOKALU_ID)
            .filter((a) => a.kategoria == null)
    }

    produceLiite(tyokalu: Arviointityokalu): Asiakirja[] {
      return [
        {
          id: tyokalu.liite.id,
          nimi: tyokalu.liitetiedostonNimi || '',
          contentType: tyokalu.liitetiedostonTyyppi,
          data: tyokalu.liite.data,
          isDirty: false
        }
      ]
    }

    asiakirjaDataEndpoint(): string {
      if (this.$isVastuuhenkilo()) {
        return 'vastuuhenkilo/asiakirjat/'
      } else if (this.$isKouluttaja) {
        return 'kouluttaja/asiakirjat/'
      } else {
        return 'erikoistuva-laakari/asiakirjat/'
      }
    }
  }
</script>

<style scoped lang="scss">
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .card.border.mb-2 {
    border-radius: 0.5rem;
  }

  .esittely {
    max-width: 1024px;
  }
</style>
