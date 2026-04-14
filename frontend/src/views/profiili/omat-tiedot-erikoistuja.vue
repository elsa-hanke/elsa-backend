<template>
  <div class="omat-tiedot mb-4">
    <div v-if="!editing">
      <div class="d-block d-lg-none d-xl-none">
        <avatar
          :src="avatarSrc"
          :username="displayName"
          background-color="gray"
          color="white"
          :size="160"
        />
      </div>
      <div class="d-flex">
        <div class="d-none d-lg-block d-xl-block mr-3">
          <avatar
            :src="avatarSrc"
            :username="displayName"
            background-color="gray"
            color="white"
            :size="160"
          />
        </div>
        <div class="flex-fill">
          <elsa-form-group
            :label="$t('erikoistuva-laakari')"
            label-cols-md="4"
            label-cols-xl="4"
            label-cols="12"
            class="align-items-center mb-md-0"
          >
            {{ displayName }}
          </elsa-form-group>
          <elsa-form-group
            :label="$t('syntymaaika')"
            label-cols-md="4"
            label-cols-xl="4"
            label-cols="12"
            class="align-items-center mb-md-0"
          >
            {{ syntymaaika }}
          </elsa-form-group>
          <elsa-form-group
            :label="$t('laillistamispaiva')"
            label-cols-md="4"
            label-cols-xl="4"
            label-cols="12"
            class="align-items-center mb-md-0"
          >
            <div v-if="form.laillistamispaiva">
              {{ $date(form.laillistamispaiva) }} -
              <elsa-button variant="link" class="pl-0" @click="onDownloadLaillistamistodistus">
                {{ laillistamistodistusNimi }}
              </elsa-button>
            </div>
          </elsa-form-group>
          <elsa-form-group
            v-if="$isYekKoulutettava() && form.laakarikoulutusSuoritettuSuomiTaiBelgia"
            :label="$t('yek.aiempi-laakarikoulutus')"
            label-cols-md="4"
            label-cols-xl="4"
            label-cols="12"
            class="align-items-center mb-md-0"
          >
            {{ $t('yek.aiempi-laakarikoulutus-olen-suorittanut') }}
          </elsa-form-group>
          <elsa-form-group
            v-if="account.email"
            :label="$t('sahkopostiosoite')"
            label-cols-md="4"
            label-cols-xl="4"
            label-cols="12"
            class="align-items-center mb-md-0"
          >
            {{ account.email }}
          </elsa-form-group>
          <elsa-form-group
            v-if="account.phoneNumber"
            :label="$t('puhelinnumero')"
            label-cols-md="4"
            label-cols-xl="4"
            label-cols="12"
            class="align-items-center mb-md-0"
          >
            {{ account.phoneNumber }}
          </elsa-form-group>
          <div class="mt-2">
            <elsa-button variant="primary" @click="() => $emit('change', !editing)">
              {{ $t('muokkaa-tietoja') }}
            </elsa-button>
          </div>
        </div>
      </div>
      <h3 class="mt-6">{{ $t('opinto-oikeudet') }}</h3>
      <i18n path="opinto-oikeudet-help" tag="p" class="mb-2">
        <a
          href="https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/lomakkeet"
          target="_blank"
          rel="noopener noreferrer"
        >
          {{ $t('luopumisilmoituksen') }}
        </a>
      </i18n>
      <b-row v-for="(opintooikeus, index) in opintooikeudet" :key="index" lg>
        <b-col>
          <div class="d-flex justify-content-center border rounded pt-1 pb-1 mb-4">
            <div class="container-fluid">
              <h4 class="mt-2 mb-3">
                {{ $t(`yliopisto-nimi.${opintooikeus.yliopistoNimi}`) }},
                {{ opintooikeus.erikoisalaNimi }}
              </h4>
              <div v-if="opintooikeus.opiskelijatunnus">
                <h5 class="mb-0">{{ $t('opiskelijanumero') }}</h5>
                <div class="mb-3">{{ opintooikeus.opiskelijatunnus }}</div>
              </div>
              <h5 class="mb-0">{{ $t('opinto-opas') }}</h5>
              <div class="mb-3">{{ opintooikeus.opintoopasNimi }}</div>
              <h5 class="mb-0">{{ $t('opinto-oikeus') }}</h5>
              <div class="mb-3">
                {{ $date(opintooikeus.opintooikeudenMyontamispaiva) }} -
                {{ $date(opintooikeus.opintooikeudenPaattymispaiva) }}
              </div>
              <h5 class="mb-0">{{ $t('asetus') }}</h5>
              <div class="mb-3">{{ opintooikeus.asetus.nimi }}</div>
              <h5 class="mb-0">{{ $t('osaamisen-arvioinnin-oppaan-paivamaara') }}</h5>
              <div class="mb-3">
                {{ $date(opintooikeus.osaamisenArvioinninOppaanPvm) }}
              </div>
            </div>
          </div>
        </b-col>
      </b-row>
    </div>
    <div v-else>
      <b-form @submit.stop.prevent="onSubmit">
        <elsa-form-group
          :label="$t('erikoistuva-laakari')"
          label-cols-md="4"
          label-cols-xl="4"
          label-cols="12"
          class="align-items-center mb-md-0"
        >
          {{ displayName }}
        </elsa-form-group>
        <elsa-form-group
          :label="$t('syntymaaika')"
          label-cols-md="4"
          label-cols-xl="4"
          label-cols="12"
          class="align-items-center mb-md-0"
        >
          {{ syntymaaika }}
        </elsa-form-group>
        <hr />
        <elsa-form-group class="col-xs-12 col-sm-3 pl-0" :label="$t('laillistamispaiva')">
          <template #default="{ uid }">
            <elsa-form-datepicker
              :id="uid"
              ref="laillistamispaiva"
              :value.sync="form.laillistamispaiva"
              @input="$emit('skipRouteExitConfirm', false)"
            ></elsa-form-datepicker>
          </template>
        </elsa-form-group>
        <elsa-form-group :label="$t('laillistamispaivan-liitetiedosto')" :required="true">
          <span>
            {{ $t('lisaa-liite-joka-todistaa-laillistamispaivan') }}
          </span>
          <asiakirjat-upload
            class="mt-3"
            :is-primary-button="false"
            :allow-multiples-files="false"
            :button-text="$t('lisaa-liitetiedosto')"
            :disabled="laillistamispaivaAsiakirjat.length > 0"
            @selectedFiles="onLaillistamispaivaFilesAdded"
          />
          <div v-if="laillistamispaivaAsiakirjat.length > 0">
            <asiakirjat-content
              :asiakirjat="laillistamispaivaAsiakirjat"
              :sorting-enabled="false"
              :pagination-enabled="false"
              :enable-search="false"
              :enable-delete="true"
              :enable-lisatty="false"
              :no-results-info-text="$t('ei-liitetiedostoja')"
              @deleteAsiakirja="onDeleteLaillistamispaivanLiite"
            />
          </div>
          <div v-else>
            <b-alert variant="dark" class="mt-3" show>
              <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
              <span>
                {{ $t('ei-asiakirjoja') }}
              </span>
            </b-alert>
          </div>
        </elsa-form-group>
        <elsa-form-group
          v-if="$isYekKoulutettava()"
          :label="$t('yek.aiempi-laakarikoulutus')"
          :required="true"
        >
          <template #default="{ uid }">
            <div>
              <b-form-radio
                key="laakarikoulutus-suomi-belgia"
                v-model="form.laakarikoulutusSuoritettuSuomiTaiBelgia"
                name="laakarikoulutus"
                :value="true"
                @input="$emit('skipRouteExitConfirm', false)"
                @change="aiempiLaakarikoulutusChange('suomibelgia')"
              >
                <span>
                  {{ $t('yek.aiempi-laakarikoulutus-olen-suorittanut-suomi-belgia') }}
                </span>
              </b-form-radio>
              <b-form-radio
                key="laakarikoulutus-muu"
                v-model="form.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia"
                name="laakarikoulutus"
                :value="true"
                @input="$emit('skipRouteExitConfirm', false)"
                @change="aiempiLaakarikoulutusChange('muu')"
              >
                <span>
                  {{ $t('yek.aiempi-laakarikoulutus-olen-suorittanut-muu-kuin-suomi-belgia') }}
                </span>
              </b-form-radio>
              <b-form-invalid-feedback
                :id="`${uid}-feedback`"
                :style="{
                  display:
                    !form.laakarikoulutusSuoritettuSuomiTaiBelgia &&
                    !form.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia
                      ? 'block'
                      : 'none'
                }"
              >
                {{ $t('pakollinen-tieto') }}
              </b-form-invalid-feedback>
            </div>
            <hr />
          </template>
        </elsa-form-group>

        <elsa-form-group :label="$t('sahkopostiosoite')" :required="true">
          <template #default="{ uid }">
            <b-form-input
              :id="uid"
              v-model="form.email"
              :state="validateState('email')"
              :aria-describedby="`${uid}-feedback`"
              @input="needEmailConfirm = true"
              @copy.prevent
            ></b-form-input>
            <b-form-invalid-feedback :id="`${uid}-feedback`">
              {{ $t('tarkista-sahkoposti') }}
            </b-form-invalid-feedback>
          </template>
        </elsa-form-group>
        <elsa-form-group v-if="needEmailConfirm" :label="$t('vahvista-sahkopostiosoite')">
          <template #default="{ uid }">
            <b-form-input
              :id="uid"
              v-model="confirmEmailValue"
              :state="validateState('emailConfirm')"
              :aria-describedby="`${uid}-feedback`"
            ></b-form-input>
            <b-form-invalid-feedback :id="`${uid}-feedback`">
              {{ $t('sahkopostiosoitteet-eivat-tasmaa') }}
            </b-form-invalid-feedback>
          </template>
        </elsa-form-group>
        <elsa-form-group :label="$t('puhelinnumero')">
          <template #default="{ uid }">
            <b-form-input
              :id="uid"
              v-model="form.phoneNumber"
              :state="validateState('phoneNumber')"
            ></b-form-input>
            <small class="form-text text-muted">
              {{ $t('syota-puhelinnumero-muodossa') }}
            </small>
            <b-form-invalid-feedback :id="`${uid}-feedback`">
              {{ $t('tarkista-puhelinnumeron-muoto') }}
            </b-form-invalid-feedback>
          </template>
        </elsa-form-group>
        <elsa-form-group :label="$t('profiilikuva')">
          <avatar
            :src="previewSrc"
            src-content-type="image/jpeg"
            :username="displayName"
            background-color="gray"
            color="white"
            :size="160"
          />
          <input
            id="avatar-file-input"
            ref="avatarFileInput"
            type="file"
            accept="image/jpeg,image/png"
            hidden
            @change="avatarChange"
          />
          <div class="d-flex flex-wrap">
            <div class="mt-2">
              <elsa-button variant="primary" class="mr-2" @click="selectAvatar">
                {{ $t('valitse-profiilikuva') }}
              </elsa-button>
            </div>
            <div class="mt-2">
              <elsa-button v-if="form.avatar" variant="outline-danger" @click="removeAvatar">
                <font-awesome-icon :icon="['far', 'trash-alt']" fixed-width size="lg" />
                {{ $t('poista-kuva') }}
              </elsa-button>
            </div>
          </div>
        </elsa-form-group>
        <hr />
        <div class="text-right">
          <elsa-button variant="back" @click="onCancel">
            {{ $t('peruuta') }}
          </elsa-button>
          <elsa-button :loading="params.saving" type="submit" variant="primary" class="ml-2">
            {{ $t('tallenna') }}
          </elsa-button>
        </div>
        <div class="row">
          <elsa-form-error :active="$v.$anyError" />
        </div>
      </b-form>
    </div>
  </div>
</template>

<script lang="ts">
  import axios, { AxiosError } from 'axios'
  import Avatar from 'vue-avatar'
  import { Component, Vue, Prop } from 'vue-property-decorator'
  import { email, required } from 'vuelidate/lib/validators'

  import AsiakirjatContent from '@/components/asiakirjat/asiakirjat-content.vue'
  import AsiakirjatUpload from '@/components/asiakirjat/asiakirjat-upload.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import store from '@/store'
  import {
    OmatTiedotLomakeErikoistuja,
    ElsaError,
    Laillistamistiedot,
    Asiakirja,
    Opintooikeus
  } from '@/types'
  import { saveBlob } from '@/utils/blobs'
  import { confirmExit } from '@/utils/confirm'
  import { phoneNumber } from '@/utils/constants'
  import { mapFile, mapFiles } from '@/utils/fileMapper'
  import { getTitleFromAuthorities } from '@/utils/functions'
  import { sortByDesc } from '@/utils/sort'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      Avatar,
      AsiakirjatContent,
      AsiakirjatUpload,
      ElsaFormDatepicker,
      ElsaButton,
      ElsaFormError,
      ElsaFormGroup
    }
  })
  export default class OmatTiedotErikoistuja extends Vue {
    @Prop({ required: false, default: false })
    editing!: boolean
    needEmailConfirm = false
    confirmEmailValue = ''

    validations() {
      return {
        form: {
          email: {
            required,
            email
          },
          emailConfirm: {
            emailConfirmed: () => {
              return this.needEmailConfirm ? this.form.email === this.confirmEmailValue : true
            }
          },
          phoneNumber: {
            phoneNumber
          }
        }
      }
    }

    $refs!: {
      laillistamispaiva: ElsaFormDatepicker
      avatarFileInput: HTMLInputElement
    }

    form: OmatTiedotLomakeErikoistuja = {
      email: null,
      phoneNumber: null,
      avatar: null,
      avatarUpdated: false,
      laillistamispaiva: null,
      laillistamispaivanLiite: null,
      laakarikoulutusSuoritettuSuomiTaiBelgia: false,
      laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia: false
    }

    laillistamispaivaAsiakirjat: Asiakirja[] = []

    params = {
      saving: false
    }

    async mounted() {
      try {
        const role = this.$isErikoistuva() ? 'erikoistuva-laakari' : 'yek-koulutettava'
        const laillistamistiedot: Laillistamistiedot = (
          await axios.get(`/${role}/laillistamispaiva`)
        ).data
        this.form.laillistamispaiva = laillistamistiedot.laillistamispaiva
        if (laillistamistiedot.laakarikoulutusSuoritettuSuomiTaiBelgia) {
          this.form.laakarikoulutusSuoritettuSuomiTaiBelgia =
            laillistamistiedot.laakarikoulutusSuoritettuSuomiTaiBelgia
        }
        if (laillistamistiedot.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia) {
          this.form.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia =
            laillistamistiedot.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia
        }

        if (laillistamistiedot.laillistamistodistus) {
          const data = Uint8Array.from(atob(laillistamistiedot.laillistamistodistus), (c) =>
            c.charCodeAt(0)
          )
          this.laillistamispaivaAsiakirjat.push(
            mapFile(
              new File([data], laillistamistiedot.laillistamistodistusNimi || '', {
                type: laillistamistiedot.laillistamistodistusTyyppi || ''
              })
            )
          )
        }
      } catch {
        toastFail(this, this.$t('laillistamispaivan-hakeminen-epaonnistui'))
      }
      this.form = this.initForm()
    }

    selectAvatar() {
      this.$refs.avatarFileInput.click()
    }

    removeAvatar() {
      this.form.avatar = null
      this.form.avatarUpdated = true
      this.$refs.avatarFileInput.value = ''
    }

    avatarChange(e: Event) {
      const inputElement = e.target as HTMLInputElement
      if (inputElement.files && inputElement.files?.length > 0) {
        const file = inputElement.files[0]
        this.form.avatar = file
        this.form.avatarUpdated = true
      }
    }

    async onDownloadLaillistamistodistus() {
      if (this.laillistamispaivaAsiakirjat.length > 0) {
        const file = this.laillistamispaivaAsiakirjat[0]
        const data = await file.data
        if (data) {
          saveBlob(file.nimi, data, file.contentType || '')
        }
      }
    }

    onLaillistamispaivaFilesAdded(files: File[]) {
      this.form.laillistamispaivanLiite = files[0]
      this.laillistamispaivaAsiakirjat.push(...mapFiles(files))
    }

    async onDeleteLaillistamispaivanLiite() {
      this.form.laillistamispaivanLiite = null
      this.laillistamispaivaAsiakirjat = []
    }

    initForm(): OmatTiedotLomakeErikoistuja {
      return {
        email: this.account?.email || null,
        phoneNumber: this.account?.phoneNumber || null,
        avatar: this.account?.avatar || null,
        avatarUpdated: false,
        laillistamispaiva: this.form.laillistamispaiva,
        laakarikoulutusSuoritettuSuomiTaiBelgia: this.form.laakarikoulutusSuoritettuSuomiTaiBelgia,
        laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia:
          this.form.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia
      }
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    validateForm(): boolean {
      this.$v.form.$touch()
      return !this.$v.$anyError
    }

    async onSubmit() {
      const validations = [
        this.validateForm(),
        this.$refs.laillistamispaiva ? this.$refs.laillistamispaiva.validateForm() : true
      ]

      if (validations.includes(false)) {
        return
      }

      try {
        this.params.saving = true
        if (
          this.form.laillistamispaivanLiite == null &&
          this.laillistamispaivaAsiakirjat.length > 0
        ) {
          const file = this.laillistamispaivaAsiakirjat[0]
          const data = await file.data
          if (data) {
            this.form.laillistamispaivanLiite = new File([data], file.nimi || '', {
              type: file.contentType || ''
            })
          }
        }

        await store.dispatch(
          'auth/putErikoistuvaLaakari',
          // Ohitetaan olemassa olevan avatarin lähettäminen
          !this.form.avatar?.name
            ? {
                ...this.form,
                avatar: null
              }
            : this.form
        )
        toastSuccess(this, this.$t('omat-tiedot-paivitetty'))
        this.$v.form.$reset()
        this.form = this.initForm()
        this.$emit('change', false)
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('omien-tietojen-paivittaminen-epaonnistui')}: ${this.$t(message)}`
            : this.$t('omien-tietojen-paivittaminen-epaonnistui')
        )
      } finally {
        this.params.saving = false
        this.needEmailConfirm = false
      }
    }

    async onCancel() {
      if (await confirmExit(this)) {
        this.form = this.initForm()
        this.$v.form.$reset()
        this.$emit('change', false)
      }
    }

    get previewSrc() {
      if (this.form.avatar && this.form.avatar.name) {
        return URL.createObjectURL(this.form.avatar)
      } else if (this.form.avatar) {
        return `data:image/jpeg;base64,${this.account.avatar}`
      }
      return undefined
    }

    get account() {
      return store.getters['auth/account']
    }

    get displayName() {
      if (this.account) {
        return `${this.account.firstName} ${this.account.lastName}`
      }
      return ''
    }

    get syntymaaika() {
      if (this.account) {
        return this.$date(this.account.erikoistuvaLaakari.syntymaaika)
      }
      return ''
    }

    get avatarSrc() {
      if (this.account) {
        return `data:image/jpeg;base64,${this.account.avatar}`
      }
      return undefined
    }

    get activeAuthority() {
      if (this.account) {
        return this.account.activeAuthority
      }
      return ''
    }

    get title() {
      return getTitleFromAuthorities(this, this.activeAuthority)
    }

    get avatar() {
      if (this.account) {
        return this.account.avatar
      }
      return null
    }

    get laillistamistodistusNimi() {
      if (this.laillistamispaivaAsiakirjat.length > 0) {
        return this.laillistamispaivaAsiakirjat[0].nimi
      }
      return null
    }

    get opintooikeudet() {
      return this.account.erikoistuvaLaakari.opintooikeudet.sort(
        (a: Opintooikeus, b: Opintooikeus) =>
          sortByDesc(a.opintooikeudenMyontamispaiva, b.opintooikeudenMyontamispaiva)
      )
    }

    aiempiLaakarikoulutusChange(option: string) {
      if (option === 'suomibelgia') {
        this.form.laakarikoulutusSuoritettuSuomiTaiBelgia = true
        this.form.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia = false
      } else {
        this.form.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia = true
        this.form.laakarikoulutusSuoritettuSuomiTaiBelgia = false
      }
    }
  }
</script>
