<template>
  <div class="arviointityokalu">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('arviointityokalu') }}</h1>
          <hr />
          <b-form-row>
            <elsa-form-group :label="$t('tila')" class="col-sm-12 col-md-6 pr-md-3">
              <template #default="{ uid }">
                <template v-if="!form.kaytossa">
                  <span :id="uid" class="text-danger">
                    {{ $t('poistettu') }}
                  </span>
                </template>
                <template v-else-if="form.tila.toLowerCase() === 'julkaistu'">
                  <span :id="uid" class="text-success">
                    {{ $t('arviointityokalu-tila-' + form.tila.toLowerCase()) }}
                  </span>
                </template>
                <template v-else>
                  {{ $t('arviointityokalu-tila-' + form.tila.toLowerCase()) }}
                </template>
              </template>
            </elsa-form-group>
          </b-form-row>
          <hr />
          <div v-if="form">
            <b-form-row>
              <elsa-form-group
                :label="$t('arviointityokalun-nimi')"
                class="col-sm-12 col-md-6 pr-md-3"
              >
                <template #default="{ uid }">
                  <span :id="uid">{{ form.nimi }}</span>
                </template>
              </elsa-form-group>
            </b-form-row>
            <b-form-row>
              <elsa-form-group :label="$t('kategoria')" class="col-sm-12 col-md-6 pr-md-3">
                <template #default="{ uid }">
                  <span :id="uid">
                    {{ form.kategoria ? form.kategoria.nimi : $t('ei-kategoriaa') }}
                  </span>
                </template>
              </elsa-form-group>
            </b-form-row>
            <b-form-row>
              <elsa-form-group
                :label="$t('ohjeteksti-arviointityokalun-kayttoon')"
                class="col-sm-12 col-md-12 pr-md-3"
              >
                <!-- eslint-disable-next-line vue/no-v-html -->
                <div v-html="form.ohjeteksti"></div>
              </elsa-form-group>
            </b-form-row>
            <b-form-row>
              <elsa-form-group
                class="col-sm-12 col-md-12 pr-md-3"
                :label="$t('arviointityokalu-liitetiedostona')"
              >
                <asiakirjat-content
                  v-if="asiakirjat && asiakirjat.length > 0"
                  :asiakirjat="asiakirjat"
                  :sorting-enabled="false"
                  :pagination-enabled="false"
                  :enable-search="false"
                  :show-info-if-empty="false"
                  :enable-delete="false"
                />
                <b-alert v-else variant="dark" show>
                  <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
                  <span>
                    {{ $t('ei-liitetiedostoa') }}
                  </span>
                </b-alert>
              </elsa-form-group>
            </b-form-row>
            <hr />
            <h1>{{ $t('kysymykset') }}</h1>
            <div
              v-for="(kysymys, index) in form.kysymykset"
              :key="kysymys.jarjestysnumero"
              class="mt-4"
            >
              <arviointityokalu-lomake-kysymys-form
                :kysymys="kysymys"
                :child-data-received="!loading"
                :answer-mode="false"
                :index="index"
                :arviointityokalu-id="form.id"
              />
            </div>
            <hr />
            <div class="d-flex flex-row-reverse flex-wrap">
              <elsa-button
                v-if="!form.kaytossa"
                variant="primary"
                class="mb-3 ml-3"
                @click="palautaLuonnostilaan()"
              >
                {{ $t('palauta-luonnostilaan') }}
              </elsa-button>
              <elsa-button
                v-if="form.kaytossa"
                variant="primary"
                class="mb-3 ml-3"
                :to="{
                  name: 'lisaa-arviointityokalu',
                  params: { arviointityokaluId: form.id }
                }"
              >
                {{ $t('muokkaa-arviointityokalua') }}
              </elsa-button>
              <elsa-button
                v-if="form.id && form.kaytossa"
                variant="outline-danger"
                class="mb-3 ml-3"
                @click="showDeleteConfirm()"
              >
                {{ $t('poista-arviointityokalu') }}
              </elsa-button>
              <elsa-button
                :to="{ name: 'arviointityokalut' }"
                variant="link"
                class="mb-3 mr-auto font-weight-500 kayttajahallinta-link"
              >
                {{ $t('palaa-arviointityokaluihin') }}
              </elsa-button>
            </div>
          </div>
          <div v-else class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
        </b-col>
      </b-row>

      <elsa-confirmation-modal
        id="confirm-dialog"
        :title="$t('arviointityokalu-poista-arviointityokalu')"
        :submit-text="$t('arviointityokalu-poista-arviointityokalu')"
        :text="$t('arviointityokalu-poista-arviointityokalu-selite')"
        submit-variant="outline-danger"
        :hide-on-submit="false"
        @submit="onArviointityokaluDelete"
        @cancel="onCancelConfirm"
      >
        <template #modal-content></template>
      </elsa-confirmation-modal>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { Component, Mixins } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { required } from 'vuelidate/lib/validators'

  import {
    deleteArviointityokalu,
    getArviointityokalu,
    palautaArviointityokalu
  } from '@/api/tekninen-paakayttaja'
  import AsiakirjatContent from '@/components/asiakirjat/asiakirjat-content.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ArviointityokaluLomakeKysymysForm from '@/forms/arviointityokalu-lomake-kysymys-form.vue'
  import { Arviointityokalu, Asiakirja } from '@/types'
  import { confirmExit } from '@/utils/confirm'
  import { ArviointityokaluTila } from '@/utils/constants'
  import { mapFile } from '@/utils/fileMapper'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ArviointityokaluLomakeKysymysForm,
      AsiakirjatContent,
      ElsaButton,
      ElsaConfirmationModal,
      ElsaFormGroup,
      ElsaFormMultiselect
    },
    validations: {
      form: {
        nimi: {
          required
        }
      }
    }
  })
  export default class ArviointityokaluView extends Mixins(validationMixin) {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('arviointityokalut'),
        active: false
      },
      {
        text: this.$t('arviointityokalu'),
        active: true
      }
    ]

    form: Arviointityokalu = {
      id: -1,
      nimi: null,
      ohjeteksti: null,
      kategoria: null,
      kysymykset: [],
      liite: null,
      tila: ArviointityokaluTila.LUONNOS,
      kaytossa: true
    }

    asiakirjat: Asiakirja[] = []
    skipRouteExitConfirm = true
    deleting = false
    loading = false

    async mounted() {
      this.fetchArviointityokalu()
    }

    async fetchArviointityokalu() {
      try {
        this.form = (
          await getArviointityokalu(Number(this.$route?.params?.arviointityokaluId))
        ).data
        if (this.form.liite) {
          const data = Uint8Array.from(atob(this.form.liite.data), (c) => c.charCodeAt(0))
          this.asiakirjat.push(
            mapFile(
              new File([data], this.form.liitetiedostonNimi || '', {
                type: this.form.liitetiedostonTyyppi || ''
              })
            )
          )
        }
      } catch (err) {
        console.warn(err) // eslint-disable-line no-console
        toastFail(this, this.$t('arviointityokalun-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'arviointityokalut' })
        this.loading = false
      }
    }

    async onCancel() {
      if (this.skipRouteExitConfirm || (await confirmExit(this))) {
        this.$v.form.$reset()
        this.skipRouteExitConfirm = true
        this.$emit('skipRouteExitConfirm', true)
      }
    }

    showDeleteConfirm() {
      this.$bvModal.show('confirm-dialog')
    }

    onCancelConfirm() {
      this.$emit('skipRouteExitConfirm', true)
    }

    async onArviointityokaluDelete() {
      try {
        if (this.form && this.form.id) {
          await deleteArviointityokalu(this.form.id)
          toastSuccess(this, this.$t('arviointityokalu-poistettu'))
          this.skipRouteExitConfirm = true
          this.$router.replace({ name: 'arviointityokalut' })
        }
      } catch {
        this.$router.replace({ name: 'arviointityokalut' })
      }
    }

    async palautaLuonnostilaan() {
      try {
        if (this.form && this.form.id) {
          const result = await palautaArviointityokalu(this.form.id)
          if (result.status === 200) {
            toastSuccess(this, this.$t('arviointityokalu-palautettu'))
          } else {
            toastFail(this, this.$t('arviointityokalu-palautus-epaonnistui'))
          }
          this.fetchArviointityokalu()
        }
      } catch {
        this.$router.replace({ name: 'arviointityokalut' })
      }
    }
  }
</script>
<style lang="scss" scoped>
  .arviointityokalu {
    max-width: 1024px;
  }

  .kayttajahallinta-link::before {
    content: '<';
    position: absolute;
    left: 1rem;
  }
</style>
