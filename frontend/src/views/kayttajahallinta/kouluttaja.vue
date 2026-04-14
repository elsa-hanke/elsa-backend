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
              <elsa-form-group
                :label="$t('etunimi')"
                :required="editing"
                class="col-sm-12 col-md-6 pr-md-3"
              >
                <template #default="{ uid }">
                  <div v-if="editing">
                    <b-form-input
                      :id="uid"
                      v-model="form.etunimi"
                      :state="validateState('etunimi')"
                      @input="skipRouteExitConfirm = false"
                    ></b-form-input>
                    <b-form-invalid-feedback :id="`${uid}-feedback`">
                      {{ $t('pakollinen-tieto') }}
                    </b-form-invalid-feedback>
                  </div>
                  <span v-else :id="uid">{{ etunimi }}</span>
                </template>
              </elsa-form-group>
              <elsa-form-group
                :label="$t('sukunimi')"
                :required="editing"
                class="col-sm-12 col-md-6 pl-md-3"
              >
                <template #default="{ uid }">
                  <div v-if="editing">
                    <b-form-input
                      :id="uid"
                      v-model="form.sukunimi"
                      :state="validateState('sukunimi')"
                      @input="skipRouteExitConfirm = false"
                    ></b-form-input>
                    <b-form-invalid-feedback :id="`${uid}-feedback`">
                      {{ $t('pakollinen-tieto') }}
                    </b-form-invalid-feedback>
                  </div>
                  <span v-else :id="uid">{{ sukunimi }}</span>
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
                    {{ sahkoposti }}
                  </span>
                </template>
              </elsa-form-group>
            </div>
            <div>
              <elsa-form-group :label="$t('puhelinnumero')">
                <template #default="{ uid }">
                  <span :id="uid">
                    {{ puhelin ? puhelin : '-' }}
                  </span>
                </template>
              </elsa-form-group>
            </div>
            <div>
              <elsa-form-group :label="$t('yliopisto-ja-erikoisala')">
                <div
                  v-for="yliopistoAndErikoisala in yliopistotAndErikoisalat"
                  :key="yliopistoAndErikoisala.id"
                >
                  <span>
                    {{
                      `${$t(`yliopisto-nimi.${yliopistoAndErikoisala.yliopisto.nimi}`)}: ${
                        yliopistoAndErikoisala.erikoisala.nimi
                      }`
                    }}
                  </span>
                </div>
              </elsa-form-group>
            </div>
            <hr />
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
                v-if="isKutsuttu && !editing"
                variant="primary"
                class="mb-3 ml-3"
                @click="onInvitationResend"
              >
                {{ $t('laheta-kutsu-uudelleen') }}
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
                v-else-if="isKutsuttu || isAktiivinen"
                variant="outline-danger"
                :loading="updatingTila"
                :disabled="updatingKayttaja"
                class="mb-3"
                @click="showDeleteConfirm"
              >
                {{ isAktiivinen ? $t('passivoi-kayttaja') : $t('poista-kayttaja') }}
              </elsa-button>
              <elsa-button
                v-if="editing"
                variant="back"
                :disabled="updatingKayttaja"
                class="mb-3"
                @click.stop.prevent="onCancel"
              >
                {{ $t('peruuta') }}
              </elsa-button>
              <elsa-button
                v-if="!editing"
                :disabled="updatingTila"
                :to="{ name: 'kayttajahallinta', hash: '#kouluttajat' }"
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

      <elsa-confirmation-modal
        id="confirm-dialog"
        :title="isAktiivinen ? $t('passivoi-kayttaja') : $t('poista-kayttaja')"
        :submit-text="isAktiivinen ? $t('passivoi-kayttaja') : $t('poista-kayttaja')"
        submit-variant="outline-danger"
        :hide-on-submit="false"
        @submit="onPassivateDeleteKayttaja"
        @cancel="onCancelConfirm"
      >
        <template #modal-content>
          <div v-if="kayttajaWrapper && !kayttajaWrapper.avoimiaTehtavia" class="d-block">
            {{ isAktiivinen ? $t('passivoi-kayttaja-varmistus') : $t('poista-kayttaja-varmistus') }}
          </div>
          <div v-else>
            <p class="mb-3">
              {{
                isAktiivinen
                  ? $t('passivoi-kayttaja-varmistus-avoimia-tehtavia')
                  : $t('poista-kayttaja-varmistus-avoimia-tehtavia')
              }}
            </p>
            <elsa-form-group :label="$t('kouluttaja')" :required="true" class="col-md-12 pl-0 mb-2">
              <template #default="{ uid }">
                <elsa-form-multiselect
                  :id="uid"
                  v-model="reassignedKouluttaja"
                  :options="formattedKouluttajat"
                  :state="validateConfirm()"
                  track-by="id"
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
    deleteKayttaja,
    getKayttaja,
    getKorvaavatKouluttajat,
    passivateKayttaja,
    patchKouluttaja,
    putKouluttajaInvitation
  } from '@/api/kayttajahallinta'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import KayttajahallintaKayttajaMixin from '@/mixins/kayttajahallinta-kayttaja'
  import { ElsaError, Kayttaja, KayttajahallintaUpdateKayttaja } from '@/types'
  import { confirmExit } from '@/utils/confirm'
  import { KayttajatiliTila } from '@/utils/constants'
  import { formatList } from '@/utils/kouluttajaAndVastuuhenkiloListFormatter'
  import { sortByAsc } from '@/utils/sort'
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
        sahkoposti: {
          required,
          email
        },
        sahkopostiUudelleen: {
          required,
          email,
          sameAsSahkoposti: sameAs('sahkoposti')
        },
        etunimi: {
          required
        },
        sukunimi: {
          required
        }
      },
      reassignedKouluttaja: {
        required
      }
    }
  })
  export default class KouluttajaView extends Mixins(KayttajahallintaKayttajaMixin) {
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
      etunimi: null,
      sukunimi: null,
      sahkoposti: null,
      sahkopostiUudelleen: null
    }

    deleting = false
    reassignedKouluttaja: Kayttaja | null = null
    kouluttajat: Kayttaja[] = []

    async mounted() {
      await this.fetchKayttaja()
      await this.fetchKouluttajat()
    }

    async fetchKayttaja() {
      try {
        this.kayttajaWrapper = (await getKayttaja(this.$route?.params?.kayttajaId)).data
        this.initForm()
      } catch (err) {
        toastFail(this, this.$t('kayttajan-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'kayttajahallinta' })

        this.loading = false
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
      this.form.etunimi = this.etunimi
      this.form.sukunimi = this.sukunimi
    }

    async onSave() {
      if (!this.validateForm() || !this.kayttajaWrapper?.kayttaja?.id) {
        return
      }
      this.updatingKayttaja = true

      try {
        await patchKouluttaja(this.kayttajaWrapper.kayttaja.id, {
          etunimi: this.form.etunimi,
          sukunimi: this.form.sukunimi,
          sahkoposti: this.form.sahkoposti
        })
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

      await this.fetchKayttaja()
      this.editing = false
      this.updatingKayttaja = false
      this.skipRouteExitConfirm = true
      this.$emit('skipRouteExitConfirm', true)
    }

    async onCancel() {
      if (this.skipRouteExitConfirm || (await confirmExit(this))) {
        this.initForm()
        this.$v.form.$reset()
        this.skipRouteExitConfirm = true
        this.editing = false
        this.$emit('skipRouteExitConfirm', true)
      }
    }

    async onInvitationResend() {
      if (
        this.kayttajaWrapper?.kayttaja?.id &&
        (await this.$bvModal.msgBoxConfirm(this.$t('laheta-kutsu-viesti-kouluttaja') as string, {
          title: this.$t('laheta-kutsu-uudelleen') as string,
          okVariant: 'primary',
          okTitle: this.$t('laheta-kutsu') as string,
          cancelTitle: this.$t('peruuta') as string,
          cancelVariant: 'back',
          hideHeaderClose: false,
          centered: true
        }))
      ) {
        try {
          await putKouluttajaInvitation(this.kayttajaWrapper?.kayttaja?.id)
          toastSuccess(this, this.$t('kutsulinkki-lahetetty-uudestaan'))
        } catch (err) {
          toastFail(this, this.$t('kutsulinkin-lahettaminen-epaonnistui'))
        }
      }
    }

    showDeleteConfirm() {
      this.$bvModal.show('confirm-dialog')
    }

    async onPassivateDeleteKayttaja() {
      this.$v.reassignedKouluttaja.$touch()
      if (
        (this.kayttajaWrapper?.avoimiaTehtavia && this.reassignedKouluttaja == null) ||
        !this.kayttajaWrapper?.kayttaja?.id
      ) {
        return
      }
      this.$bvModal.hide('confirm-dialog')
      this.updatingTila = true
      if (this.isAktiivinen) {
        try {
          await passivateKayttaja(this.kayttajaWrapper.kayttaja.id, this.reassignedKouluttaja?.id)
          this.kayttajaWrapper.kayttaja.tila = KayttajatiliTila.PASSIIVINEN
          this.kayttajaWrapper.avoimiaTehtavia = false
          toastSuccess(this, this.$t('kayttajan-passivointi-onnistui'))
        } catch (err) {
          toastFail(this, this.$t('kayttajan-passivointi-epaonnistui'))
        }
      } else {
        try {
          await deleteKayttaja(this.kayttajaWrapper.kayttaja.id, this.reassignedKouluttaja?.id)
          toastSuccess(this, this.$t('kayttajan-poisto-onnistui'))
          this.$emit('skipRouteExitConfirm', true)
          this.$router.replace({ name: 'kayttajahallinta' })
        } catch (err) {
          toastFail(this, this.$t('kayttajan-poisto-epaonnistui'))
        }
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
