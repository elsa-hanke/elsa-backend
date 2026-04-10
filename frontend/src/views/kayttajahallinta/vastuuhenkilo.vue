<template>
  <div class="kayttaja">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('kayttaja') }}</h1>
          <hr />
          <div v-if="kayttajaWrapper">
            <elsa-form-group :label="$t('tilin-tila')">
              <template #default="{ uid }">
                <span :id="uid" :class="tilaColor">{{ tilinTilaText }}</span>
              </template>
            </elsa-form-group>
            <elsa-form-group v-if="rooli" :label="$t('rooli')">
              <template #default="{ uid }">
                <span :id="uid">{{ rooli }}</span>
              </template>
            </elsa-form-group>
            <b-form-row>
              <elsa-form-group :label="$t('etunimi')" class="col-sm-12 col-md-6 pr-md-3">
                <template #default="{ uid }">
                  <span :id="uid">{{ etunimi }}</span>
                </template>
              </elsa-form-group>
              <elsa-form-group :label="$t('sukunimi')" class="col-sm-12 col-md-6 pl-md-3">
                <template #default="{ uid }">
                  <span :id="uid">{{ sukunimi }}</span>
                </template>
              </elsa-form-group>
            </b-form-row>
            <div v-if="editing">
              <elsa-form-group :label="$t('sahkopostiosoite')" :required="true">
                <template #default="{ uid }">
                  <b-form-input
                    :id="uid"
                    v-model="form.sahkoposti"
                    :state="validateState('sahkoposti')"
                    @input="skipRouteExitConfirm = false"
                  ></b-form-input>
                  <b-form-invalid-feedback
                    v-if="$v.form.sahkoposti && !$v.form.sahkoposti.required"
                    :id="`${uid}-feedback`"
                  >
                    {{ $t('pakollinen-tieto') }}
                  </b-form-invalid-feedback>
                  <b-form-invalid-feedback
                    v-if="$v.form.sahkoposti && !$v.form.sahkoposti.email"
                    :id="`${uid}-feedback`"
                  >
                    {{ $t('sahkopostiosoite-ei-kelvollinen') }}
                  </b-form-invalid-feedback>
                </template>
              </elsa-form-group>
              <elsa-form-group :label="$t('sahkopostiosoite-uudelleen')" :required="true">
                <template #default="{ uid }">
                  <b-form-input
                    :id="uid"
                    v-model="form.sahkopostiUudelleen"
                    :state="validateState('sahkopostiUudelleen')"
                    @input="skipRouteExitConfirm = false"
                  ></b-form-input>
                  <b-form-invalid-feedback
                    v-if="$v.form.sahkopostiUudelleen && !$v.form.sahkopostiUudelleen.required"
                    :id="`${uid}-feedback`"
                  >
                    {{ $t('pakollinen-tieto') }}
                  </b-form-invalid-feedback>
                  <b-form-invalid-feedback
                    v-if="$v.form.sahkopostiUudelleen && !$v.form.sahkopostiUudelleen.email"
                    :id="`${uid}-feedback`"
                  >
                    {{ $t('sahkopostiosoite-ei-kelvollinen') }}
                  </b-form-invalid-feedback>
                  <b-form-invalid-feedback
                    v-if="
                      $v.form.sahkopostiUudelleen &&
                      $v.form.sahkopostiUudelleen.required &&
                      $v.form.sahkopostiUudelleen.email &&
                      !$v.form.sahkopostiUudelleen.sameAsSahkoposti
                    "
                    :id="`${uid}-feedback`"
                  >
                    {{ $t('sahkopostiosoitteet-eivat-tasmaa') }}
                  </b-form-invalid-feedback>
                </template>
              </elsa-form-group>
            </div>
            <div v-else>
              <elsa-form-group :label="$t('sahkopostiosoite')">
                <template #default="{ uid }">
                  <span :id="uid">
                    {{ form.sahkoposti }}
                  </span>
                </template>
              </elsa-form-group>
            </div>
            <hr />
            <h2 class="mb-3">{{ $t('yliopisto-ja-erikoisalat') }}</h2>
            <elsa-form-group v-if="yliopisto" :label="$t('yliopisto')">
              <template #default="{ uid }">
                <span :id="uid">{{ $t(`yliopisto-nimi.${yliopisto.nimi}`) }}</span>
              </template>
            </elsa-form-group>
            <elsa-form-group
              :required="editing && isKutsuttu"
              :label="$t('yliopiston-kayttajatunnus')"
            >
              <template #default="{ uid }">
                <div v-if="editing && isKutsuttu">
                  <b-form-input
                    :id="uid"
                    v-model="form.eppn"
                    class="col-sm-12 col-md-6 pr-md-3"
                    :state="validateState('eppn')"
                    @input="skipRouteExitConfirm = false"
                  ></b-form-input>
                  <b-form-invalid-feedback :id="`${uid}-feedback`">
                    {{ $t('pakollinen-tieto') }}
                  </b-form-invalid-feedback>
                </div>
                <div v-else>
                  <span :id="uid">{{ form.eppn }}</span>
                </div>
              </template>
            </elsa-form-group>
            <vastuuhenkilon-tehtavat
              ref="vastuuhenkilonTehtavat"
              :yliopisto="yliopisto"
              :kayttaja-id="kayttajaId"
              :yliopistot-and-erikoisalat="yliopistotAndErikoisalat"
              :editing="editing"
              :disabled="updatingKayttaja"
              @skipRouteExitConfirm="onSkipRouteExitConfirm"
            />
            <div class="d-flex flex-row-reverse flex-wrap">
              <elsa-button
                v-if="editing"
                variant="primary"
                :loading="updatingKayttaja"
                class="mb-3 ml-3"
                @click="onSave"
              >
                {{ $t('tallenna') }}
              </elsa-button>
              <elsa-button
                v-else
                variant="primary"
                :disabled="updatingTila"
                class="mb-3 ml-3"
                @click="onEditUser"
              >
                {{ $t('muokkaa-kayttajaa') }}
              </elsa-button>
              <elsa-button
                v-if="isPassiivinen"
                variant="outline-success"
                :loading="updatingTila"
                :disabled="updatingKayttaja"
                class="mb-3"
                @click="onActivateKayttaja"
              >
                {{ $t('aktivoi-kayttaja') }}
              </elsa-button>
              <elsa-button
                v-else-if="isAktiivinen || isKutsuttu"
                variant="outline-danger"
                :loading="updatingTila"
                :disabled="updatingKayttaja || hasVastuualueita"
                class="mb-3"
                @click="showPassivateConfirm"
              >
                {{ $t('passivoi-kayttaja') }}
              </elsa-button>
              <elsa-button
                v-if="editing"
                variant="back"
                :disabled="updatingKayttaja"
                class="mb-3 mr-3"
                @click.stop.prevent="onCancel"
              >
                {{ $t('peruuta') }}
              </elsa-button>
              <elsa-button
                v-if="!editing"
                :disabled="updatingTila"
                :to="{ name: 'kayttajahallinta', hash: '#vastuuhenkilot' }"
                variant="link"
                class="mb-3 mr-auto font-weight-500 kayttajahallinta-link"
              >
                {{ $t('palaa-kayttajahallintaan') }}
              </elsa-button>
            </div>
          </div>
          <div v-else class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
        </b-col>
      </b-row>
      <b-row>
        <elsa-form-error :active="$v.form.$anyError" />
      </b-row>

      <elsa-confirmation-modal
        id="confirm-dialog"
        :title="$t('passivoi-kayttaja')"
        :submit-text="$t('passivoi-kayttaja')"
        submit-variant="outline-danger"
        :hide-on-submit="false"
        @submit="onPassivateKayttaja"
        @cancel="onCancelConfirm"
      >
        <template #modal-content>
          <div v-if="kayttajaWrapper && !kayttajaWrapper.avoimiaTehtavia" class="d-block">
            {{ $t('passivoi-kayttaja-varmistus') }}
          </div>
          <div v-else>
            <p class="mb-3">
              {{ $t('passivoi-kayttaja-varmistus-avoimia-tehtavia') }}
            </p>
            <elsa-form-group
              :label="$t('vastuuhenkilo')"
              :required="true"
              class="col-md-12 pl-0 mb-2"
            >
              <template #default="{ uid }">
                <elsa-form-multiselect
                  :id="uid"
                  v-model="reassignedKouluttaja"
                  :options="formattedKouluttajat"
                  :state="validateConfirm()"
                  track-by="kayttajaId"
                  label="nimi"
                  @input="$emit('skipRouteExitConfirm', false)"
                >
                  <template #option="{ option }">
                    <div v-if="option.nimi != null">{{ option.nimi }}</div>
                  </template>
                </elsa-form-multiselect>
                <b-form-invalid-feedback :id="`${uid}-feedback`" :state="validateConfirm()">
                  {{ $t('pakollinen-tieto') }}
                </b-form-invalid-feedback>
              </template>
            </elsa-form-group>
          </div>
        </template>
      </elsa-confirmation-modal>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { AxiosError } from 'axios'
  import { Component, Mixins } from 'vue-property-decorator'
  import { Validation } from 'vuelidate'
  import { required, email, sameAs } from 'vuelidate/lib/validators'

  import {
    getKayttaja,
    getKorvaavatKouluttajat,
    passivateKayttaja,
    putVastuuhenkilo
  } from '@/api/kayttajahallinta'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import KayttajahallintaKayttajaMixin from '@/mixins/kayttajahallinta-kayttaja'
  import {
    KayttajahallintaUpdateKayttaja,
    ErikoisalaForVastuuhenkilonTehtavat,
    ElsaError,
    KayttajaYliopistoErikoisala,
    VastuuhenkilonTehtava,
    ReassignedVastuuhenkilonTehtava,
    Kayttaja
  } from '@/types'
  import { confirmExit } from '@/utils/confirm'
  import { KayttajatiliTila } from '@/utils/constants'
  import { formatList } from '@/utils/kouluttajaAndVastuuhenkiloListFormatter'
  import { sortByAsc } from '@/utils/sort'
  import { toastFail, toastSuccess } from '@/utils/toast'
  import VastuuhenkilonTehtavat from '@/views/kayttajahallinta/vastuuhenkilon-tehtavat.vue'

  @Component({
    components: {
      ElsaButton,
      ElsaConfirmationModal,
      ElsaFormError,
      ElsaFormGroup,
      ElsaFormMultiselect,
      VastuuhenkilonTehtavat
    },
    validations: {
      form: {
        sahkoposti: {
          required,
          email
        },
        sahkopostiUudelleen: {
          required,
          email,
          sameAsSahkoposti: sameAs('sahkoposti')
        },
        eppn: {
          required
        }
      },
      reassignedKouluttaja: {
        required
      }
    }
  })
  export default class VastuuhenkiloView extends Mixins(KayttajahallintaKayttajaMixin) {
    $refs!: {
      vastuuhenkilonTehtavat: any
    }

    items = [
      {
        text: this.$t('kayttajahallinta'),
        to: { name: 'kayttajahallinta' }
      },
      {
        text: this.$t('kayttaja'),
        active: true
      }
    ]

    form: KayttajahallintaUpdateKayttaja = {
      sahkoposti: null,
      sahkopostiUudelleen: null,
      eppn: null,
      yliopistotAndErikoisalat: [],
      reassignedTehtavat: []
    }

    reassignedKouluttaja: Kayttaja | null = null
    kouluttajat: Kayttaja[] = []

    async mounted() {
      await this.fetchKayttaja()
      await this.fetchKouluttajat()
      this.loading = false
    }

    async fetchKayttaja() {
      try {
        this.kayttajaWrapper = (await getKayttaja(this.$route?.params?.kayttajaId)).data
        this.initForm()
      } catch (err) {
        toastFail(this, this.$t('kayttajan-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'kayttajahallinta' })
      }
    }

    async fetchKouluttajat() {
      try {
        this.kouluttajat = (await getKorvaavatKouluttajat(this.$route?.params?.kayttajaId)).data
          .filter((k) => k.id !== this.kayttajaWrapper?.kayttaja?.id)
          .sort((a, b) => sortByAsc(a.sukunimi, b.sukunimi))
      } catch (err) {
        toastFail(this, this.$t('kayttajan-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'kayttajahallinta' })

        this.loading = false
      }
    }

    initForm() {
      const sahkoposti = this.sahkoposti
      this.form.sahkoposti = sahkoposti
      this.form.sahkopostiUudelleen = sahkoposti
      this.form.eppn = this.eppn
      this.form.yliopistotAndErikoisalat = []
      this.form.reassignedTehtavat = []
    }

    onEditUser() {
      this.editing = true
      this.$refs.vastuuhenkilonTehtavat.initForm()
    }

    async onCancel() {
      if (this.skipRouteExitConfirm || (await confirmExit(this))) {
        this.initForm()
        this.$v.form.$reset()
        this.$refs.vastuuhenkilonTehtavat.initForm()
        this.skipRouteExitConfirm = true
        this.editing = false
        this.$emit('skipRouteExitConfirm', true)
      }
    }

    async onSave() {
      const vastuuhenkilonTehtavatForm = this.$refs.vastuuhenkilonTehtavat.getFormIfValid()
      if (
        !this.kayttajaWrapper?.kayttaja?.userId ||
        !this.validateForm() ||
        !vastuuhenkilonTehtavatForm
      ) {
        return
      }
      this.updatingKayttaja = true
      this.form.yliopistotAndErikoisalat = vastuuhenkilonTehtavatForm.yliopistotAndErikoisalat.map(
        (ye: KayttajaYliopistoErikoisala) => {
          return {
            ...ye,
            vastuuhenkilonTehtavat: ye.vastuuhenkilonTehtavat.filter(
              (vt: VastuuhenkilonTehtava | boolean) => vt !== false
            )
          }
        }
      )
      this.form.reassignedTehtavat = vastuuhenkilonTehtavatForm.erikoisalatForTehtavat
        .map((e: ErikoisalaForVastuuhenkilonTehtavat) => e.reassignedTehtavat)
        .flat()
        .filter((r: ReassignedVastuuhenkilonTehtava) => r !== undefined)

      try {
        if (!this.kayttajaWrapper.kayttaja?.id) return
        this.kayttajaWrapper = (
          await putVastuuhenkilo(this.kayttajaWrapper.kayttaja?.id, {
            ...this.form,
            yliopistotAndErikoisalat: this.form.yliopistotAndErikoisalat,
            reassignedTehtavat: this.form.reassignedTehtavat
          })
        ).data

        toastSuccess(this, this.$t('kayttajan-tiedot-paivitetty'))
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

      this.editing = false
      this.updatingKayttaja = false
      this.skipRouteExitConfirm = true
      this.$emit('skipRouteExitConfirm', true)
    }

    showPassivateConfirm() {
      this.$bvModal.show('confirm-dialog')
    }

    async onPassivateKayttaja() {
      this.$v.reassignedKouluttaja.$touch()
      if (
        (this.kayttajaWrapper?.avoimiaTehtavia && this.reassignedKouluttaja == null) ||
        !this.kayttajaWrapper?.kayttaja?.id
      ) {
        return
      }
      this.$bvModal.hide('confirm-dialog')
      this.updatingTila = true
      try {
        await passivateKayttaja(this.kayttajaWrapper.kayttaja.id, this.reassignedKouluttaja?.id)
        this.kayttajaWrapper.kayttaja.tila = KayttajatiliTila.PASSIIVINEN
        this.kayttajaWrapper.avoimiaTehtavia = false
        toastSuccess(this, this.$t('kayttajan-passivointi-onnistui'))
      } catch (err) {
        toastFail(this, this.$t('kayttajan-passivointi-epaonnistui'))
      }
      this.updatingTila = false
      this.$emit('skipRouteExitConfirm', true)
    }

    onCancelConfirm() {
      this.$emit('skipRouteExitConfirm', true)
    }

    get formattedKouluttajat() {
      return formatList(this, this.kouluttajat)
    }

    validateConfirm() {
      const { $dirty, $error } = this.$v.reassignedKouluttaja as Validation
      return $dirty ? ($error ? false : null) : null
    }

    onSkipRouteExitConfirm(value: boolean) {
      this.skipRouteExitConfirm = value
    }

    get hasVastuualueita() {
      return (
        (
          this.kayttajaWrapper?.kayttaja?.yliopistotAndErikoisalat.filter(
            (ye) => ye.vastuuhenkilonTehtavat.length > 0
          ) || []
        ).length > 0
      )
    }
  }
</script>
<style lang="scss" scoped>
  .kayttaja {
    max-width: 768px;
  }

  .kayttajahallinta-link::before {
    content: '<';
    position: absolute;
    left: 1rem;
  }
</style>
