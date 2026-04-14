<template>
  <div>
    <b-row lg>
      <b-col>
        <b-form v-if="editing" @submit.stop.prevent="onSubmit">
          <elsa-form-group
            v-if="kurssikoodi.id != null && kurssikoodi.tyyppi != null"
            :label="$t('tyyppi')"
            class="col-12 pr-md-3 pl-0"
          >
            <template #default="{}">
              {{ $t('kurssikoodi-tyyppi-' + kurssikoodi.tyyppi.nimi) }}
            </template>
          </elsa-form-group>
          <elsa-form-group v-else :label="$t('tyyppi')" class="col-12 pr-md-3 pl-0">
            <template #default="{ uid }">
              <b-form-radio
                v-for="(tyyppi, index) in tyypit"
                :key="index"
                v-model="kurssikoodi.tyyppi"
                :state="validateState('tyyppi')"
                :name="tyyppi.nimi"
                :value="tyyppi"
                @input="$emit('skipRouteExitConfirm', false)"
              >
                <span>
                  {{ $t('kurssikoodi-tyyppi-' + tyyppi.nimi) }}
                </span>
              </b-form-radio>
              <b-form-invalid-feedback :id="`${uid}-feedback`" :state="validateState('tyyppi')">
                {{ $t('pakollinen-tieto') }}
              </b-form-invalid-feedback>
            </template>
          </elsa-form-group>
          <elsa-form-group
            :label="$t('tunniste')"
            :required="true"
            class="col-md-12 col-lg-4 pr-md-3 pl-0"
          >
            <template #label-help>
              <elsa-popover style="white-space: pre-line">
                <p class="mb-2">{{ $t('kurssikoodi-tunniste-ohje') }}</p>
                <p class="mb-2">{{ $t('kurssikoodi-tunniste-ohje2') }}</p>
                <p class="mb-1">{{ $t('kurssikoodi-tunniste-ohje3') }}</p>
                <ul>
                  <li>{{ $t('kurssikoodi-tunniste-ohje4') }}</li>
                  <li>{{ $t('kurssikoodi-tunniste-ohje5') }}</li>
                </ul>
                <p class="mb-2">{{ $t('kurssikoodi-tunniste-ohje6') }}</p>
                <p class="mb-1">{{ $t('kurssikoodi-tunniste-ohje3') }}</p>
                <ul>
                  <li>{{ $t('kurssikoodi-tunniste-ohje7') }}</li>
                </ul>
              </elsa-popover>
            </template>
            <template #default="{ uid }">
              <b-form-input
                :id="uid"
                v-model="kurssikoodi.tunniste"
                :state="validateState('tunniste')"
                @input="$emit('skipRouteExitConfirm', false)"
              ></b-form-input>
              <b-form-invalid-feedback :id="`${uid}-feedback`">
                {{ $t('pakollinen-tieto') }}
              </b-form-invalid-feedback>
            </template>
          </elsa-form-group>
          <div class="text-right">
            <elsa-button
              v-if="kurssikoodi == null"
              variant="back"
              :to="{
                name: 'kurssikoodit'
              }"
            >
              {{ $t('peruuta') }}
            </elsa-button>
            <elsa-button
              v-else
              variant="back"
              :to="{
                name: 'kurssikoodit',
                kurssikoodiId: kurssikoodi.id
              }"
            >
              {{ $t('peruuta') }}
            </elsa-button>
            <elsa-button type="submit" variant="primary" class="ml-2">
              {{ $t('tallenna') }}
            </elsa-button>
          </div>
          <div class="row">
            <elsa-form-error :active="$v.$anyError" />
          </div>
        </b-form>
        <div v-else>
          <elsa-form-group
            v-if="kurssikoodi.tyyppi"
            :label="$t('tyyppi')"
            class="col-12 pr-md-3 pl-0"
          >
            <template #default="{}">
              {{ $t('kurssikoodi-tyyppi-' + kurssikoodi.tyyppi.nimi) }}
            </template>
          </elsa-form-group>
          <elsa-form-group :label="$t('tunniste')" class="col-12 pr-md-3 pl-0">
            <template #default="{}">
              {{ kurssikoodi.tunniste }}
            </template>
          </elsa-form-group>
        </div>
      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
  import { Component, Prop, Vue } from 'vue-property-decorator'
  import { Validation } from 'vuelidate'
  import { required } from 'vuelidate/lib/validators'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import { Kurssikoodi, OpintosuoritusTyyppi } from '@/types'

  @Component({
    components: {
      ElsaButton,
      ElsaFormGroup,
      ElsaFormError,
      ElsaPopover
    }
  })
  export default class KurssikoodiForm extends Vue {
    @Prop({ required: false, default: false })
    editing!: boolean

    @Prop({ required: true, type: Object })
    kurssikoodi!: Kurssikoodi

    @Prop({ required: false, default: () => [] })
    tyypit?: OpintosuoritusTyyppi[]

    params = {
      saving: false
    }

    validations() {
      return {
        kurssikoodi: {
          tunniste: {
            required
          },
          tyyppi: {
            required
          }
        }
      }
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.kurssikoodi[name] as Validation
      return $dirty ? ($error ? false : null) : null
    }

    validateForm(): boolean {
      this.$v.kurssikoodi.$touch()
      return !this.$v.$anyError
    }

    onSubmit() {
      if (!this.validateForm()) {
        return
      }
      this.$emit(
        'submit',
        {
          ...this.kurssikoodi
        },
        this.params
      )
    }
  }
</script>
