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
            <elsa-form-group v-if="syntymaaika" :label="$t('syntymaaika')">
              <template #default="{ uid }">
                <span :id="uid">
                  {{ syntymaaika }}
                </span>
              </template>
            </elsa-form-group>
            <hr />
            <h2>{{ $t('opintooikeudet') }}</h2>
            <div
              v-for="(opintooikeus, index) in opintooikeudet"
              :key="opintooikeus.id"
              class="border rounded p-3 mb-4"
            >
              <h3 class="mb-3">
                {{
                  `${$t(`yliopisto-nimi.${opintooikeus.yliopistoNimi}`)}, ${
                    opintooikeus.erikoisalaNimi
                  }`
                }}
              </h3>
              <elsa-form-group v-if="opintooikeus.opiskelijatunnus" :label="$t('opiskelijatunnus')">
                <template #default="{ uid }">
                  <span :id="uid">{{ opintooikeus.opiskelijatunnus }}</span>
                </template>
              </elsa-form-group>
              <elsa-form-group :label="$t('opintooikeus')">
                <template #default="{ uid }">
                  <span :id="uid">
                    {{ `${$date(opintooikeus.opintooikeudenMyontamispaiva)} -` }}
                  </span>
                  <span
                    :class="{ 'text-danger': isInPast(opintooikeus.opintooikeudenPaattymispaiva) }"
                  >
                    {{ `${$date(opintooikeus.opintooikeudenPaattymispaiva)}` }}
                  </span>
                </template>
              </elsa-form-group>
              <elsa-form-group :label="$t('asetus')">
                <template #default="{ uid }">
                  <span :id="uid">{{ opintooikeus.asetus.nimi }}</span>
                </template>
              </elsa-form-group>
              <elsa-form-group
                v-if="editing && form.opintooikeudet"
                :label="$t('kaytossa-oleva-opintoopas')"
                :required="true"
              >
                <template #default="{ uid }">
                  <elsa-form-multiselect
                    :id="uid"
                    v-model="form.opintooikeudet[index].opintoopas"
                    :options="opintooppaatFilteredAndSorted(opintooikeus.erikoisalaId)"
                    :state="validateOpintoopas(index)"
                    label="nimi"
                    value-field="id"
                    track-by="id"
                    @input="$emit('skipRouteExitConfirm', false)"
                  />
                  <b-form-invalid-feedback :id="`${uid}-feedback`">
                    {{ $t('pakollinen-tieto') }}
                  </b-form-invalid-feedback>
                </template>
              </elsa-form-group>
              <elsa-form-group v-else :label="$t('kaytossa-oleva-opintoopas')">
                <template #default="{ uid }">
                  <span :id="uid">{{ opintooikeus.opintoopasNimi }}</span>
                </template>
              </elsa-form-group>
              <elsa-form-group
                v-if="editing && form.opintooikeudet"
                class="col-xs-12 col-sm-6 pl-0"
                :label="$t('osaamisen-arvioinnin-oppaan-paivamaara')"
                :required="true"
              >
                <template #default="{ uid }">
                  <elsa-form-datepicker
                    :id="uid"
                    :ref="`osaamisenArvioinninOppaanPvm${opintooikeus.id}`"
                    :value.sync="form.opintooikeudet[index].osaamisenArvioinninOppaanPvm"
                    @input="$emit('skipRouteExitConfirm', false)"
                  ></elsa-form-datepicker>
                </template>
              </elsa-form-group>
              <elsa-form-group v-else :label="$t('osaamisen-arvioinnin-oppaan-paivamaara')">
                <template #default="{ uid }">
                  <span :id="uid">
                    {{ $date(opintooikeus.osaamisenArvioinninOppaanPvm) }}
                  </span>
                </template>
              </elsa-form-group>
            </div>
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
                v-else-if="isAktiivinen"
                variant="outline-danger"
                :loading="updatingTila"
                :disabled="updatingKayttaja"
                class="mb-3"
                @click="onPassivateKayttaja"
              >
                {{ $t('passivoi-kayttaja') }}
              </elsa-button>
              <elsa-button
                v-else-if="isKutsuttu"
                variant="primary"
                class="mb-3"
                @click="onInvitationResend"
              >
                {{ $t('laheta-kutsu-uudelleen') }}
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
                :to="{ name: 'kayttajahallinta', hash: '#erikoistuvat-laakarit' }"
                variant="link"
                class="mb-3 mr-auto font-weight-500 kayttajahallinta-link"
              >
                {{ $t('palaa-kayttajahallintaan') }}
              </elsa-button>
            </div>
            <div class="row">
              <elsa-form-error :active="$v.$anyError" />
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
  import { AxiosError } from 'axios'
  import { Component, Mixins } from 'vue-property-decorator'
  import { Validation } from 'vuelidate'
  import { required, email, sameAs } from 'vuelidate/lib/validators'

  import {
    getErikoistuvaLaakari,
    putErikoistuvaLaakariInvitation,
    patchErikoistuvaLaakari,
    getOpintooppaat
  } from '@/api/kayttajahallinta'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import KayttajahallintaKayttajaMixin from '@/mixins/kayttajahallinta-kayttaja'
  import { KayttajahallintaUpdateKayttaja, ElsaError, OpintoopasSimple } from '@/types/index'
  import { confirmExit } from '@/utils/confirm'
  import { isInPast } from '@/utils/date'
  import { sortByAsc } from '@/utils/sort'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      ElsaFormDatepicker,
      ElsaFormError,
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
        opintooikeudet: {
          $each: {
            osaamisenArvioinninOppaanPvm: {
              required
            },
            opintoopas: {
              required
            }
          }
        }
      }
    }
  })
  export default class ErikoistuvaLaakariView extends Mixins(KayttajahallintaKayttajaMixin) {
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
    resending = false
    opintooppaat: OpintoopasSimple[] = []

    form: KayttajahallintaUpdateKayttaja = {
      sahkoposti: null,
      sahkopostiUudelleen: null,
      opintooikeudet: []
    }

    async mounted() {
      await this.fetchOpintooppaat()
      await this.fetchKayttaja()
      this.loading = false
    }

    async fetchKayttaja() {
      try {
        this.kayttajaWrapper = (await getErikoistuvaLaakari(this.$route?.params?.kayttajaId)).data
        this.initForm()
      } catch (err) {
        toastFail(this, this.$t('kayttajan-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'kayttajahallinta' })
      }
    }

    async fetchOpintooppaat() {
      try {
        this.opintooppaat = (await getOpintooppaat()).data
      } catch (err) {
        toastFail(this, this.$t('opintooppaiden-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'kayttajahallinta' })
      }
    }

    initForm() {
      const sahkoposti = this.sahkoposti
      this.form.sahkoposti = sahkoposti
      this.form.sahkopostiUudelleen = sahkoposti
      this.form.opintooikeudet = this.opintooikeudet.map((o) => ({
        id: o.id,
        opintoopas: this.opintooppaat.find((o2) => o2.id === o.opintoopasId),
        osaamisenArvioinninOppaanPvm: o.osaamisenArvioinninOppaanPvm
      }))
    }

    async onInvitationResend() {
      if (
        this.kayttajaWrapper?.erikoistuvaLaakari?.id &&
        (await this.$bvModal.msgBoxConfirm(this.$t('laheta-kutsu-viesti') as string, {
          title: this.$t('laheta-kutsu-uudelleen') as string,
          okVariant: 'primary',
          okTitle: this.$t('laheta-kutsu') as string,
          cancelTitle: this.$t('peruuta') as string,
          cancelVariant: 'back',
          hideHeaderClose: false,
          centered: true
        }))
      ) {
        this.resending = true
        try {
          await putErikoistuvaLaakariInvitation(this.kayttajaWrapper?.erikoistuvaLaakari?.id)
          toastSuccess(this, this.$t('kutsulinkki-lahetetty-uudestaan'))
        } catch (err) {
          toastFail(this, this.$t('kutsulinkin-lahettaminen-epaonnistui'))
        }
        this.resending = false
      }
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

    async onSave() {
      const pvmValidations = this.opintooikeudet.map((o) =>
        (
          this.$refs[`osaamisenArvioinninOppaanPvm${o.id}`] as ElsaFormDatepicker[]
        )[0].validateForm()
      )
      if (
        !this.validateForm() ||
        pvmValidations.includes(false) ||
        !this.kayttajaWrapper?.kayttaja
      ) {
        return
      }
      this.updatingKayttaja = true

      try {
        await patchErikoistuvaLaakari(this.kayttajaWrapper.kayttaja.userId, {
          sahkoposti: this.form.sahkoposti,
          opintooikeudet: this.form.opintooikeudet?.map((o) => ({
            id: o.id,
            opintoopas: (o.opintoopas as OpintoopasSimple)?.id,
            osaamisenArvioinninOppaanPvm: o.osaamisenArvioinninOppaanPvm
          }))
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

    isInPast(date: string) {
      return isInPast(date)
    }

    get syntymaaika() {
      return this.kayttajaWrapper?.erikoistuvaLaakari?.syntymaaika
        ? this.$date(this.kayttajaWrapper?.erikoistuvaLaakari?.syntymaaika)
        : ''
    }

    get opintooikeudet() {
      return this.kayttajaWrapper?.erikoistuvaLaakari?.opintooikeudet ?? []
    }

    opintooppaatFilteredAndSorted(erikoisalaId: number) {
      return this.opintooppaat
        .filter((o) => o.erikoisalaId === erikoisalaId)
        .sort((a, b) => sortByAsc(a.nimi, b.nimi))
    }

    validateOpintoopas(index: number) {
      const { $dirty, $error } = this.$v.form?.opintooikeudet?.$each[index]
        ?.opintoopas as Validation
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
