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
            :label="title ? title : $t('nimi')"
            label-cols-md="4"
            label-cols-xl="4"
            label-cols="12"
            class="align-items-center mb-md-0"
          >
            {{ displayName }}
          </elsa-form-group>
          <div v-if="$isKouluttaja() || $isVastuuhenkilo()">
            <elsa-form-group
              :label="$t('nimike')"
              label-cols-md="4"
              label-cols-xl="4"
              label-cols="12"
              class="align-items-center mb-md-0"
            >
              {{ form.nimike }}
            </elsa-form-group>
            <elsa-form-group
              :label="$t('yliopisto-ja-erikoisalat')"
              label-cols-md="4"
              label-cols-xl="4"
              label-cols="12"
              class="align-items-center mb-md-0"
            >
              <div
                v-for="yliopistoErikoisalat in form.kayttajanYliopistotJaErikoisalat"
                :key="yliopistoErikoisalat.yliopisto.id"
              >
                <div v-for="erikoisala in yliopistoErikoisalat.erikoisalat" :key="erikoisala.id">
                  {{ $t(`yliopisto-nimi.${yliopistoErikoisalat.yliopisto.nimi}`) }}:
                  {{ erikoisala.nimi }}
                </div>
              </div>
            </elsa-form-group>
          </div>
          <div v-if="$isVirkailija()">
            <elsa-form-group
              :label="$t('yliopisto')"
              label-cols-md="4"
              label-cols-xl="4"
              label-cols="12"
              class="align-items-center mb-md-0"
            >
              <div
                v-for="yliopisto in kayttajaTiedot ? kayttajaTiedot.kayttajanYliopistot : []"
                :key="yliopisto.id"
              >
                {{ $t(`yliopisto-nimi.${yliopisto.nimi}`) }}
              </div>
            </elsa-form-group>
          </div>
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
        </div>
      </div>
      <div class="text-right">
        <elsa-button variant="primary" @click="() => $emit('change', !editing)">
          {{ $t('muokkaa-tietoja') }}
        </elsa-button>
      </div>
    </div>
    <div v-else>
      <b-form @submit.stop.prevent="onSubmit">
        <elsa-form-group
          :label="title ? title : $t('nimi')"
          label-cols-md="4"
          label-cols-xl="4"
          label-cols="12"
          class="align-items-center mb-md-2"
        >
          {{ displayName }}
        </elsa-form-group>
        <div v-if="$isVastuuhenkilo()">
          <elsa-form-group
            :label="$t('yliopisto-ja-erikoisalat')"
            label-cols-md="4"
            label-cols-xl="4"
            label-cols="12"
            class="align-items-center mb-md-0"
          >
            <div
              v-for="yliopistoErikoisalat in form.kayttajanYliopistotJaErikoisalat"
              :key="yliopistoErikoisalat.yliopisto.id"
            >
              <div v-for="erikoisala in yliopistoErikoisalat.erikoisalat" :key="erikoisala.id">
                {{ $t(`yliopisto-nimi.${yliopistoErikoisalat.yliopisto.nimi}`) }}:
                {{ erikoisala.nimi }}
              </div>
            </div>
          </elsa-form-group>
          <hr />
        </div>
        <div v-if="$isVirkailija()">
          <elsa-form-group
            :label="$t('yliopisto')"
            label-cols-md="4"
            label-cols-xl="4"
            label-cols="12"
            class="align-items-center mb-md-0"
          >
            <div
              v-for="yliopisto in kayttajaTiedot ? kayttajaTiedot.kayttajanYliopistot : []"
              :key="yliopisto.id"
            >
              {{ $t(`yliopisto-nimi.${yliopisto.nimi}`) }}
            </div>
          </elsa-form-group>
          <hr />
        </div>
        <elsa-form-group v-if="$isKouluttaja() || $isVastuuhenkilo()" :label="$t('nimike')">
          <template #default="{ uid }">
            <b-form-input :id="uid" v-model="form.nimike"></b-form-input>
          </template>
        </elsa-form-group>
        <div v-if="$isKouluttaja()">
          <hr />
          <div
            v-for="(yliopistoErikoisalat, index) in form.kayttajanYliopistotJaErikoisalat"
            :key="yliopistoErikoisalat.yliopisto.id"
          >
            <hr v-if="index > 0" />
            <elsa-form-group :label="$t('yliopisto')">
              <template #default="{ uid }">
                <elsa-form-multiselect
                  :id="uid"
                  v-model="form.kayttajanYliopistotJaErikoisalat[index].yliopisto"
                  :options="
                    yliopistotOptions(form.kayttajanYliopistotJaErikoisalat[index].yliopisto)
                  "
                  :state="validateKayttajanYliopistoState(index)"
                  :custom-label="yliopistoLabel"
                ></elsa-form-multiselect>
                <b-form-invalid-feedback :state="validateKayttajanYliopistoState(index)">
                  {{ $t('pakollinen-tieto') }}
                </b-form-invalid-feedback>
              </template>
            </elsa-form-group>
            <elsa-form-group :label="$t('erikoisala')">
              <template #default="{ uid }">
                <elsa-form-multiselect
                  :id="uid"
                  v-model="form.kayttajanYliopistotJaErikoisalat[index].erikoisalat"
                  :options="kayttajaTiedot ? kayttajaTiedot.erikoisalat : []"
                  :multiple="true"
                  :state="validateKayttajanYliopistoErikoisalaState(index)"
                  label="nimi"
                  track-by="id"
                ></elsa-form-multiselect>
                <b-form-invalid-feedback :state="validateKayttajanYliopistoErikoisalaState(index)">
                  {{ $t('pakollinen-tieto') }}
                </b-form-invalid-feedback>
              </template>
            </elsa-form-group>
            <elsa-button
              v-if="index !== 0"
              variant="link"
              class="text-decoration-none shadow-none p-0"
              @click="deleteYliopistoErikoisala(index)"
            >
              <font-awesome-icon :icon="['far', 'trash-alt']" fixed-width size="sm" />
              {{ $t('poista-yliopisto-ja-erikoisala') }}
            </elsa-button>
          </div>
          <elsa-button
            variant="link"
            class="text-decoration-none shadow-none p-0"
            @click="addYliopistoErikoisala"
          >
            <font-awesome-icon icon="plus" fixed-width size="sm" />
            {{ $t('lisaa-yliopisto-ja-erikoisala') }}
          </elsa-button>
          <hr />
        </div>
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
            ref="avatar-file-input"
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
  import { TranslateResult } from 'vue-i18n'
  import { Component, Vue, Prop, Mixins } from 'vue-property-decorator'
  import { Validation, validationMixin } from 'vuelidate'
  import { minLength, required, email } from 'vuelidate/lib/validators'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import store from '@/store'
  import {
    ElsaError,
    OmatTiedotLomake,
    Yliopisto,
    Kayttajatiedot,
    KayttajaYliopistoErikoisalat
  } from '@/types'
  import { confirmExit } from '@/utils/confirm'
  import { phoneNumber } from '@/utils/constants'
  import { getTitleFromAuthorities } from '@/utils/functions'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      Avatar,
      ElsaButton,
      ElsaFormError,
      ElsaFormGroup,
      ElsaFormMultiselect
    }
  })
  export default class OmatTiedot extends Mixins(validationMixin) {
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
          },
          kayttajanYliopistotJaErikoisalat: {
            $each: {
              yliopisto: {
                id: {
                  required
                }
              },
              erikoisalat: {
                required,
                minLength: minLength(1)
              }
            }
          }
        }
      }
    }

    form: OmatTiedotLomake = {
      nimike: null,
      email: null,
      phoneNumber: null,
      avatar: null,
      avatarUpdated: false,
      kayttajanYliopistotJaErikoisalat: []
    }
    params = {
      saving: false
    }

    kayttajaTiedot: Kayttajatiedot | null = null
    valittuYliopisto: Yliopisto | null = null

    async mounted() {
      this.kayttajaTiedot = (await axios.get('/kayttaja-lisatiedot')).data
      this.form = this.initForm()
    }

    selectAvatar() {
      const inputEl = this.$refs['avatar-file-input'] as HTMLInputElement
      inputEl.click()
    }

    removeAvatar() {
      this.form.avatar = null
      this.form.avatarUpdated = true
      const inputEl = this.$refs['avatar-file-input'] as HTMLInputElement
      inputEl.value = ''
    }

    avatarChange(e: Event) {
      const inputElement = e.target as HTMLInputElement
      if (inputElement.files && inputElement.files?.length > 0) {
        const file = inputElement.files[0]
        this.form.avatar = file
        this.form.avatarUpdated = true
      }
    }

    initForm(): OmatTiedotLomake {
      return {
        email: this.account?.email || null,
        phoneNumber: this.account?.phoneNumber || null,
        avatar: this.account?.avatar || null,
        avatarUpdated: false,
        nimike: this.kayttajaTiedot?.nimike || null,
        kayttajanYliopistotJaErikoisalat:
          this.kayttajaTiedot?.kayttajanYliopistotJaErikoisalat || []
      }
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    validateKayttajanYliopistoState(index: number) {
      const { $dirty, $error } = this.$v.form?.kayttajanYliopistotJaErikoisalat?.$each[index]
        ?.yliopisto.id as Validation
      return $dirty ? ($error ? false : null) : null
    }

    validateKayttajanYliopistoErikoisalaState(index: number) {
      const { $dirty, $error } = this.$v.form?.kayttajanYliopistotJaErikoisalat?.$each[index]
        ?.erikoisalat as Validation
      return $dirty ? ($error ? false : null) : null
    }

    addYliopistoErikoisala() {
      this.form.kayttajanYliopistotJaErikoisalat.push({
        yliopisto: { nimi: '', erikoisalat: [] },
        erikoisalat: []
      })
    }

    deleteYliopistoErikoisala(index: number) {
      if (this.form.kayttajanYliopistotJaErikoisalat) {
        Vue.delete(this.form.kayttajanYliopistotJaErikoisalat, index)
      }
    }

    yliopistoLabel(yliopisto: Yliopisto): TranslateResult {
      return yliopisto.nimi === '' ? '' : this.$t(`yliopisto-nimi.${yliopisto.nimi}`)
    }

    async onSubmit() {
      this.$v.form.$touch()
      if (this.$v.form.$anyError) {
        return
      }

      try {
        this.params.saving = true
        await store.dispatch(
          'auth/putUser',
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
        this.kayttajaTiedot = (await axios.get('/kayttaja-lisatiedot')).data
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

    yliopistotOptions(yliopisto: Yliopisto) {
      return this.kayttajaTiedot?.yliopistot?.map((y: Yliopisto) => {
        if (
          this.form.kayttajanYliopistotJaErikoisalat
            .map((y2: KayttajaYliopistoErikoisalat) => y2.yliopisto.id)
            .includes(y.id)
        ) {
          return {
            ...y,
            $isDisabled: y.id !== yliopisto.id
          }
        }
        return y
      })
    }
  }
</script>
