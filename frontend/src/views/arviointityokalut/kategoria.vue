<template>
  <div class="kategoria">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('kategoria') }}</h1>
          <hr />
          <div v-if="form">
            <b-form-row>
              <elsa-form-group
                :label="$t('kategorian-nimi')"
                :required="editing"
                class="col-sm-12 col-md-6 pr-md-3"
              >
                <template #default="{ uid }">
                  <div v-if="editing">
                    <b-form-input
                      :id="uid"
                      v-model="form.nimi"
                      :state="validateState('nimi')"
                      @input="skipRouteExitConfirm = false"
                    ></b-form-input>
                    <b-form-invalid-feedback :id="`${uid}-feedback`">
                      {{ $t('pakollinen-tieto') }}
                    </b-form-invalid-feedback>
                  </div>
                  <span v-else :id="uid">{{ form.nimi }}</span>
                </template>
              </elsa-form-group>
            </b-form-row>
            <hr />
            <div class="d-flex flex-row-reverse flex-wrap">
              <elsa-button
                v-if="editing"
                variant="primary"
                :loading="updatingKategoria"
                class="mb-3 ml-3"
                @click="onSave"
              >
                {{ $t('tallenna') }}
              </elsa-button>
              <elsa-button
                v-else
                variant="primary"
                :disabled="updatingKategoria"
                class="mb-3 ml-3"
                @click="onEditKategoria"
              >
                {{ $t('muokkaa-kategoriaa') }}
              </elsa-button>
              <elsa-button
                v-if="editing"
                variant="back"
                :disabled="updatingKategoria"
                class="mb-3"
                @click.stop.prevent="onCancel"
              >
                {{ $t('peruuta') }}
              </elsa-button>
              <elsa-button
                v-if="form.id"
                variant="outline-danger"
                class="mb-3 ml-3"
                @click="showDeleteConfirm()"
              >
                {{ $t('arviointityokalut-kategoria-poista-kategoria') }}
              </elsa-button>
              <elsa-button
                v-if="!editing"
                :disabled="updatingKategoria"
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
        :title="$t('arviointityokalut-kategoria-poista-kategoria')"
        :submit-text="$t('arviointityokalut-kategoria-poista-kategoria')"
        :text="$t('arviointityokalut-kategoria-poista-kategoria-selite')"
        submit-variant="outline-danger"
        :hide-on-submit="false"
        @submit="onKategoriaDelete"
        @cancel="onCancelConfirm"
      >
        <template #modal-content></template>
      </elsa-confirmation-modal>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { AxiosError } from 'axios'
  import { Component, Mixins } from 'vue-property-decorator'
  import { Validation, validationMixin } from 'vuelidate'
  import { required } from 'vuelidate/lib/validators'

  import {
    deleteArviointityokalutKategoria,
    getArviointityokalutKategoria,
    patchArviointityokalutKategoria
  } from '@/api/tekninen-paakayttaja'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import { ArviointityokaluKategoria, ElsaError } from '@/types'
  import { confirmExit } from '@/utils/confirm'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
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
  export default class KategoriaView extends Mixins(validationMixin) {
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
        text: this.$t('kategoria'),
        active: true
      }
    ]

    form: ArviointityokaluKategoria = {
      id: -1,
      nimi: null
    }

    skipRouteExitConfirm = true
    updatingKategoria = false
    editing = false
    deleting = false
    loading = false

    async mounted() {
      this.fetchKategoria()
    }

    async fetchKategoria() {
      try {
        this.form = (await getArviointityokalutKategoria(this.$route?.params?.kategoriaId)).data
      } catch (err) {
        console.warn(err) // eslint-disable-line no-console
        toastFail(this, this.$t('arviointityokalut-kategorian-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'arviointityokalut' })
        this.loading = false
      }
    }

    async onSave() {
      if (!this.validateForm() || !this.form?.id) {
        return
      }
      this.updatingKategoria = true
      try {
        await patchArviointityokalutKategoria(this.form)
        toastSuccess(this, this.$t('arviointityokalut-kategoria-paivitetty'))
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('tietojen-tallennus-epaonnistui')}: ${this.$t(message)}`
            : this.$t('tietojen-tallennus-epaonnistui')
        )
      }
      await this.fetchKategoria()
      this.editing = false
      this.updatingKategoria = false
      this.skipRouteExitConfirm = true
      this.$emit('skipRouteExitConfirm', true)
    }

    validateForm(): boolean {
      this.$v.form.$touch()
      return !this.$v.$anyError
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    async onCancel() {
      if (this.skipRouteExitConfirm || (await confirmExit(this))) {
        // this.initForm()
        this.$v.form.$reset()
        this.skipRouteExitConfirm = true
        this.editing = false
        this.$emit('skipRouteExitConfirm', true)
      }
    }

    showDeleteConfirm() {
      this.$bvModal.show('confirm-dialog')
    }

    onCancelConfirm() {
      this.$emit('skipRouteExitConfirm', true)
    }

    async onKategoriaDelete() {
      try {
        if (this.form && this.form.id) {
          await deleteArviointityokalutKategoria(this.form.id)
          toastSuccess(this, this.$t('arviointityokalut-kategoria-poistettu'))
          this.skipRouteExitConfirm = true
          this.$router.replace({ name: 'arviointityokalut' })
        }
      } catch {
        // todo error
        this.$router.replace({ name: 'arviointityokalut' })
      }
    }

    validateConfirm() {
      const { $dirty, $error } = this.$v.reassignedKouluttaja as Validation
      return $dirty ? ($error ? false : null) : null
    }

    onEditKategoria() {
      this.editing = true
    }
  }
</script>
<style lang="scss" scoped>
  .kategoria {
    max-width: 1024px;
  }

  .kayttajahallinta-link::before {
    content: '<';
    position: absolute;
    left: 1rem;
  }
</style>
