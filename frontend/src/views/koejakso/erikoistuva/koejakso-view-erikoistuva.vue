<template>
  <div>
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('koejakso') }}</h1>
          <p>
            {{ $t('koejakso-kuvaus') }}
            <b-link :to="{ name: 'koejakso-yleiset-tavoitteet' }">
              {{ $t('koejakso-tavoitteet-linkki') }}
            </b-link>
          </p>
        </b-col>
      </b-row>
      <div v-if="!loading">
        <b-row lg>
          <b-col>
            <div class="d-flex justify-content-center border rounded pt-3 pb-3 mb-4">
              <div class="container-fluid">
                <h2>{{ $t('koejakson-suorituspaikka') }}</h2>
                <p>{{ $t('koejakson-suorituspaikka-kuvaus') }}</p>
                <b-row>
                  <b-col>
                    <b-row v-if="!account.impersonated">
                      <b-col class="mt-2" cols="12" md="9">
                        <elsa-form-group
                          :label="$t('tyoskentelyjakso')"
                          :add-new-enabled="true"
                          :add-new-label="$t('lisaa-tyoskentelyjakso')"
                          :required="false"
                          @submit="onTyoskentelyjaksoSubmit"
                        >
                          <template #modal-content="{ submit, cancel }">
                            <tyoskentelyjakso-form
                              :kunnat="kunnat"
                              :erikoisalat="erikoisalat"
                              @submit="submit"
                              @cancel="cancel"
                            />
                          </template>
                          <template #default="{ uid }">
                            <elsa-form-multiselect
                              :id="uid"
                              v-model="form.tyoskentelyjakso"
                              :options="tyoskentelyjaksotFormatted"
                              label="label"
                              track-by="id"
                              @select="onTyoskentelyjaksoSelect"
                            />
                            <b-form-invalid-feedback :id="`${uid}-feedback`">
                              {{ $t('pakollinen-tieto') }}
                            </b-form-invalid-feedback>
                          </template>
                        </elsa-form-group>
                      </b-col>
                      <b-col
                        align-self="center"
                        class="mt-md-3 mb-3 mb-md-0 pl-3 pl-md-0"
                        cols="9"
                        md="3"
                      >
                        <elsa-button
                          variant="primary"
                          :disabled="!form.tyoskentelyjakso"
                          :loading="tyoskentelyjaksoUpdating"
                          @click="onTyoskentelyjaksoAttached"
                        >
                          {{ $t('liita-koejaksoon') }}
                        </elsa-button>
                      </b-col>
                    </b-row>
                  </b-col>
                  <b-table
                    v-if="tyoskentelyjaksotKoejakso.length > 0"
                    class="tyoskentelyjaksot-koejakso-table mt-1 mr-3 mr-md-4 ml-3"
                    :items="tyoskentelyjaksotKoejakso"
                    :fields="fields"
                    :sort-by.sync="sortBy"
                    :sort-desc.sync="sortDesc"
                    stacked="sm"
                  >
                    <template #table-colgroup="scope">
                      <col
                        v-for="field in scope.fields"
                        :key="field.key"
                        :style="{ width: `${field.width}%` }"
                      />
                    </template>
                    <template #cell(formattedNimi)="row">
                      <elsa-button
                        :to="{
                          name: 'tyoskentelyjakso',
                          params: { tyoskentelyjaksoId: row.item.id }
                        }"
                        variant="link"
                        class="shadow-none p-0 text-left"
                      >
                        {{ row.item.formattedNimi }}
                      </elsa-button>
                    </template>
                    <template #cell(delete)="row">
                      <elsa-button
                        v-if="!account.impersonated"
                        variant="outline-primary"
                        class="border-0 p-0"
                        :loading="row.item.disableDelete"
                        @click="onTyoskentelyjaksoDetached(row.item)"
                      >
                        <font-awesome-icon
                          :hidden="row.item.disableDelete"
                          :icon="['far', 'trash-alt']"
                          fixed-width
                          size="lg"
                        />
                      </elsa-button>
                    </template>
                  </b-table>
                </b-row>
              </div>
            </div>
          </b-col>
        </b-row>

        <b-row lg>
          <b-col>
            <elsa-koulutussopimus-card></elsa-koulutussopimus-card>
          </b-col>
        </b-row>

        <b-row lg>
          <b-col>
            <h1>{{ $t('koejakson-arviointi') }}</h1>
            <elsa-aloituskeskustelu-card></elsa-aloituskeskustelu-card>

            <elsa-valiarviointi-card></elsa-valiarviointi-card>

            <elsa-kehittamistoimenpiteet-card></elsa-kehittamistoimenpiteet-card>

            <elsa-loppukeskustelu-card></elsa-loppukeskustelu-card>

            <elsa-vastuuhenkilon-arvio-card></elsa-vastuuhenkilon-arvio-card>
          </b-col>
        </b-row>
      </div>
      <div v-else class="text-center">
        <b-spinner variant="primary" :label="$t('ladataan')" />
      </div>
    </b-container>
  </div>
</template>

<script lang="ts">
  import axios from 'axios'
  import Vue from 'vue'
  import Component from 'vue-class-component'
  import { Mixins } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaAloituskeskusteluCard from '@/components/koejakso-cards/aloituskeskustelu-card.vue'
  import ElsaKehittamistoimenpiteetCard from '@/components/koejakso-cards/kehittamistoimenpiteet-card.vue'
  import ElsaKoulutussopimusCard from '@/components/koejakso-cards/koulutussopimus-card.vue'
  import ElsaLoppukeskusteluCard from '@/components/koejakso-cards/loppukeskustelu-card.vue'
  import ElsaValiarviointiCard from '@/components/koejakso-cards/valiarviointi-card.vue'
  import ElsaVastuuhenkilonArvioCard from '@/components/koejakso-cards/vastuuhenkilon-arvio-card.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import TyoskentelyjaksoForm from '@/forms/tyoskentelyjakso-form.vue'
  import TyoskentelyjaksoMixin from '@/mixins/tyoskentelyjakso'
  import store from '@/store'
  import { KoejaksonTyoskentelyjakso, Tyoskentelyjakso } from '@/types'
  import { confirmDelete } from '@/utils/confirm'
  import { sortByDateDesc } from '@/utils/date'
  import { toastSuccess, toastFail } from '@/utils/toast'
  import { tyoskentelyjaksoLabel } from '@/utils/tyoskentelyjakso'

  @Component({
    components: {
      ElsaButton,
      ElsaPopover,
      ElsaFormGroup,
      ElsaFormMultiselect,
      ElsaKoulutussopimusCard,
      ElsaAloituskeskusteluCard,
      ElsaValiarviointiCard,
      ElsaKehittamistoimenpiteetCard,
      ElsaLoppukeskusteluCard,
      ElsaVastuuhenkilonArvioCard,
      TyoskentelyjaksoForm
    }
  })
  export default class KoejaksoViewErikoistuva extends Mixins(TyoskentelyjaksoMixin) {
    tyoskentelyjaksotMutable: Tyoskentelyjakso[] = []
    tyoskentelyjaksotKoejakso: KoejaksonTyoskentelyjakso[] = []
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('koejakso'),
        active: true
      }
    ]
    loading = true
    tyoskentelyjaksoUpdating = false
    sortBy = 'paattymispaiva'
    sortDesc = true

    fields = [
      {
        key: 'formattedNimi',
        label: '',
        class: 'btn-tyoskentelyjakso'
      },
      {
        key: 'delete',
        label: '',
        width: 5
      },
      {
        key: 'paattymispaiva',
        thClass: 'd-none',
        tdClass: 'd-none'
      }
    ]

    async mounted() {
      this.loading = true
      await store.dispatch('erikoistuva/getKoejakso')
      this.tyoskentelyjaksotMutable = store.getters['erikoistuva/koejakso'].tyoskentelyjaksot
      this.tyoskentelyjaksotKoejakso = this.mapTyoskentelyjaksotKoejakso(
        this.tyoskentelyjaksotMutable?.filter((t: any) => t.liitettyKoejaksoon) ?? []
      )
      this.loading = false
    }

    get account() {
      return store.getters['auth/account']
    }

    mapTyoskentelyjaksotKoejakso(tyoskentelyjaksot: any[]): KoejaksonTyoskentelyjakso[] {
      return tyoskentelyjaksot.map((t) => this.mapTyoskentelyjaksoKoejakso(t))
    }

    mapTyoskentelyjaksoKoejakso(tyoskentelyjakso: any): KoejaksonTyoskentelyjakso {
      return {
        id: tyoskentelyjakso.id,
        formattedNimi: tyoskentelyjaksoLabel(this, tyoskentelyjakso),
        paattymispaiva: tyoskentelyjakso.paattymispaiva
      } as KoejaksonTyoskentelyjakso
    }

    async onTyoskentelyjaksoAttached() {
      this.tyoskentelyjaksoUpdating = true
      try {
        const tyoskentelyjakso = (
          await axios.patch('erikoistuva-laakari/tyoskentelyjaksot/koejakso', {
            id: this.form.tyoskentelyjakso.id,
            liitettyKoejaksoon: true
          })
        ).data

        this.tyoskentelyjaksotKoejakso = [
          this.mapTyoskentelyjaksoKoejakso(tyoskentelyjakso),
          ...this.tyoskentelyjaksotKoejakso
        ] as any
        this.form.tyoskentelyjakso = null

        toastSuccess(this, this.$t('tyoskentelyjakson-lisaaminen-koejaksolle-onnistui'))
      } catch {
        toastFail(this, this.$t('tyoskentelyjakson-lisaaminen-koejaksolle-epaonnistui'))
      }
      this.tyoskentelyjaksoUpdating = false
    }

    async onTyoskentelyjaksoDetached(tyoskentelyjakso: KoejaksonTyoskentelyjakso) {
      if (
        await confirmDelete(
          this,
          this.$t('poista-tyoskentelyjakso-koejaksolta') as string,
          (this.$t('tyoskentelyjakson-koejaksolta') as string).toLowerCase()
        )
      ) {
        Vue.set(tyoskentelyjakso, 'disableDelete', true)
        try {
          await axios.patch('erikoistuva-laakari/tyoskentelyjaksot/koejakso', {
            id: tyoskentelyjakso.id,
            liitettyKoejaksoon: false
          })
          this.tyoskentelyjaksotKoejakso = this.tyoskentelyjaksotKoejakso.filter(
            (t: any) => t.id !== tyoskentelyjakso.id
          )

          toastSuccess(this, this.$t('tyoskentelyjakson-poistaminen-koejaksolta-onnistui'))
        } catch {
          toastFail(this, this.$t('tyoskentelyjakson-poistaminen-koejaksolta-epaonnistui'))
        }
        Vue.set(tyoskentelyjakso, 'disableDelete', false)
      }
    }

    get tyoskentelyjaksotFormatted() {
      return this.tyoskentelyjaksotMutable
        .concat(this.tyoskentelyjaksot)
        .filter((t: any) => !this.tyoskentelyjaksotKoejakso.map((tk) => tk.id).includes(t.id))
        .map((t: any) => ({
          ...t,
          label: tyoskentelyjaksoLabel(this, t)
        }))
        .sort((t1: Tyoskentelyjakso, t2: Tyoskentelyjakso) => {
          return sortByDateDesc(t1.paattymispaiva, t2.paattymispaiva)
        })
    }

    get kunnat() {
      return store.getters['erikoistuva/koejakso'].kunnat
    }

    get erikoisalat() {
      return store.getters['erikoistuva/koejakso'].erikoisalat
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';

  .form-order {
    font-weight: bold;
  }
</style>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  ::v-deep {
    .tyoskentelyjaksot-koejakso-table {
      border: $table-border-width solid $table-border-color;
      thead,
      &tr {
        display: none;
      }
      td {
        vertical-align: middle;
        padding-top: 0.5rem;
        padding-bottom: 0.5rem;
      }
    }

    @include media-breakpoint-down(xs) {
      .tyoskentelyjaksot-koejakso-table {
        border-bottom: none;
        margin-bottom: 0;
        tr {
          padding: 0.375rem 0 0.375rem 0;
          border: $table-border-width solid $table-border-color;
          border-radius: 0.25rem;
          margin-bottom: 0.375rem;
        }
        td {
          width: 100%;
          padding: 0.25rem 0 0.25rem 0.25rem;
          border: none;
          &.btn-tyoskentelyjakso > div {
            width: 100% !important;
          }
        }
      }
    }
  }
</style>
