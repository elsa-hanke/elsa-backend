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
            <elsa-form-group :label="$t('yliopisto')">
              <template #default="{ uid }">
                <span :id="uid">
                  {{ yliopistoNimi }}
                </span>
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
                :disabled="updatingKayttaja"
                class="mb-3"
                @click="onPassivateKayttaja"
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
                :to="{ name: 'kayttajahallinta', hash: '#virkailijat' }"
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
        <elsa-form-error :active="$v.$anyError" />
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { AxiosError } from 'axios'
  import { Component, Mixins } from 'vue-property-decorator'
  import { required, email, sameAs } from 'vuelidate/lib/validators'

  import { getKayttaja, patchVirkailija } from '@/api/kayttajahallinta'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import KayttajahallintaKayttajaMixin from '@/mixins/kayttajahallinta-kayttaja'
  import { KayttajahallintaUpdateKayttaja, ElsaError } from '@/types'
  import { confirmExit } from '@/utils/confirm'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      ElsaFormGroup,
      ElsaFormError
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
      }
    }
  })
  export default class VirkailijaView extends Mixins(KayttajahallintaKayttajaMixin) {
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

    form: Partial<KayttajahallintaUpdateKayttaja> = {
      sahkoposti: null,
      sahkopostiUudelleen: null,
      eppn: null,
      yliopistotAndErikoisalat: []
    }

    async mounted() {
      await this.fetchKayttaja()
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

    initForm() {
      const sahkoposti = this.sahkoposti
      this.form.sahkoposti = sahkoposti
      this.form.sahkopostiUudelleen = sahkoposti
      this.form.eppn = this.eppn
    }

    onEditUser() {
      this.editing = true
    }

    async onCancel() {
      if (this.skipRouteExitConfirm || (await confirmExit(this))) {
        this.initForm()
        this.$v.form.$reset()
        this.skipRouteExitConfirm = true
        this.editing = false
      }
    }

    async onSave() {
      if (!this.kayttajaWrapper?.kayttaja?.id || !this.validateForm()) {
        return
      }
      this.updatingKayttaja = true

      try {
        await patchVirkailija(this.kayttajaWrapper.kayttaja.id, {
          sahkoposti: this.form.sahkoposti,
          eppn: this.form.eppn
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

      this.editing = false
      this.updatingKayttaja = false
      this.skipRouteExitConfirm = true
    }

    get yliopistoNimi() {
      return this.$t(`yliopisto-nimi.${this.kayttajaWrapper?.kayttaja?.yliopistot[0]?.nimi}`)
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
