<template>
  <div>
    <b-row lg>
      <b-col>
        <b-form v-if="editing" @submit.stop.prevent="onSubmit">
          <elsa-form-group
            :label="$t('opintooppaan-nimi')"
            :required="true"
            class="col-12 pr-md-3 pl-0"
          >
            <template #default="{ uid }">
              <b-input-group class="mb-2">
                <b-input-group-prepend>
                  <b-input-group-text class="input-group-fi">{{ 'FI' }}</b-input-group-text>
                </b-input-group-prepend>
                <b-form-input
                  :id="uid"
                  v-model="opas.nimi"
                  :state="validateState('nimi')"
                ></b-form-input>
                <b-form-invalid-feedback :id="`${uid}-feedback`" :state="validateState('nimi')">
                  {{ $t('pakollinen-tieto') }}
                </b-form-invalid-feedback>
              </b-input-group>
              <b-input-group prepend="SV">
                <b-form-input :id="uid" v-model="opas.nimiSv"></b-form-input>
              </b-input-group>
            </template>
          </elsa-form-group>
          <b-form-row>
            <elsa-form-group
              :label="$t('voimassaolon-alkupaiva')"
              class="col-xs-12 col-sm-6 col-md-4 pr-sm-3"
              :required="true"
            >
              <template #default="{ uid }">
                <elsa-form-datepicker
                  :id="uid"
                  ref="voimassaoloAlkaa"
                  v-model="opas.voimassaoloAlkaa"
                  :state="validateState('voimassaoloAlkaa')"
                  @input="$emit('skipRouteExitConfirm', false)"
                ></elsa-form-datepicker>
              </template>
            </elsa-form-group>
            <elsa-form-group
              :label="$t('voimassaolon-paattymispaiva')"
              class="col-xs-12 col-sm-6 col-md-4 pl-sm-3"
            >
              <template #default="{ uid }">
                <elsa-form-datepicker
                  :id="uid"
                  ref="voimassaoloPaattyy"
                  v-model="opas.voimassaoloPaattyy"
                  :required="false"
                  class="datepicker-range"
                  @input="$emit('skipRouteExitConfirm', false)"
                ></elsa-form-datepicker>
              </template>
            </elsa-form-group>
          </b-form-row>
          <hr />
          <h2>{{ $t('erikoisalan-vaatimukset') }}</h2>
          <elsa-button
            v-if="opas.id == null"
            variant="primary"
            class="mt-2 mb-4"
            @click="tuoOppaanTiedot()"
          >
            {{ $t('tuo-edellisen-oppaan-tiedot') }}
          </elsa-button>
          <elsa-form-group :label="$t('kaytannon-koulutuksen-vahimmaispituus')" :required="true">
            <template #default="{ uid }">
              <b-row>
                <b-col cols="2">
                  <b-row>
                    <b-col class="pr-1">
                      <b-form-input
                        :id="uid"
                        v-model="opas.kaytannonKoulutuksenVahimmaispituusVuodet"
                        :state="validateState('kaytannonKoulutuksenVahimmaispituusVuodet')"
                      ></b-form-input>
                      <b-form-invalid-feedback
                        :id="`${uid}-feedback`"
                        :state="validateState('kaytannonKoulutuksenVahimmaispituusVuodet')"
                      >
                        {{
                          opas.kaytannonKoulutuksenVahimmaispituusVuodet
                            ? $t('virheellinen-arvo')
                            : $t('pakollinen-tieto')
                        }}
                      </b-form-invalid-feedback>
                    </b-col>
                    <b-col cols="1" class="pl-1 mt-2">
                      <span>{{ 'v' }}</span>
                    </b-col>
                  </b-row>
                </b-col>
                <b-col cols="2">
                  <b-row>
                    <b-col class="pr-1">
                      <b-form-input
                        :id="uid"
                        v-model="opas.kaytannonKoulutuksenVahimmaispituusKuukaudet"
                        :state="validateState('kaytannonKoulutuksenVahimmaispituusKuukaudet')"
                      ></b-form-input>
                      <b-form-invalid-feedback
                        :id="`${uid}-feedback`"
                        :state="validateState('kaytannonKoulutuksenVahimmaispituusKuukaudet')"
                      >
                        {{
                          opas.kaytannonKoulutuksenVahimmaispituusKuukaudet
                            ? $t('virheellinen-arvo')
                            : $t('pakollinen-tieto')
                        }}
                      </b-form-invalid-feedback>
                    </b-col>
                    <b-col cols="1" class="pl-1 mt-2">
                      <span>{{ 'kk' }}</span>
                    </b-col>
                  </b-row>
                </b-col>
              </b-row>
            </template>
          </elsa-form-group>
          <b-form-row>
            <elsa-form-group
              :label="$t('yek.tk-jakson-vahimmaispituus')"
              class="col-xs-12 col-sm-6 col-md-6 pr-sm-3"
              :required="true"
            >
              <template #default="{ uid }">
                <b-row>
                  <b-col cols="4">
                    <b-row>
                      <b-col class="pr-1">
                        <b-form-input
                          :id="uid"
                          v-model="opas.terveyskeskuskoulutusjaksonVahimmaispituusVuodet"
                          :state="validateState('terveyskeskuskoulutusjaksonVahimmaispituusVuodet')"
                        ></b-form-input>
                        <b-form-invalid-feedback
                          :id="`${uid}-feedback`"
                          :state="validateState('terveyskeskuskoulutusjaksonVahimmaispituusVuodet')"
                        >
                          {{
                            opas.terveyskeskuskoulutusjaksonVahimmaispituusVuodet
                              ? $t('virheellinen-arvo')
                              : $t('pakollinen-tieto')
                          }}
                        </b-form-invalid-feedback>
                      </b-col>
                      <b-col cols="1" class="pl-1 mt-2">
                        <span>{{ 'v' }}</span>
                      </b-col>
                    </b-row>
                  </b-col>
                  <b-col cols="4">
                    <b-row>
                      <b-col class="pr-1">
                        <b-form-input
                          :id="uid"
                          v-model="opas.terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet"
                          :state="
                            validateState('terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet')
                          "
                        ></b-form-input>
                        <b-form-invalid-feedback
                          :id="`${uid}-feedback`"
                          :state="
                            validateState('terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet')
                          "
                        >
                          {{
                            opas.terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet
                              ? $t('virheellinen-arvo')
                              : $t('pakollinen-tieto')
                          }}
                        </b-form-invalid-feedback>
                      </b-col>
                      <b-col cols="1" class="pl-1 mt-2">
                        <span>{{ 'kk' }}</span>
                      </b-col>
                    </b-row>
                  </b-col>
                </b-row>
              </template>
            </elsa-form-group>
          </b-form-row>
          <elsa-form-group :label="$t('yek.sairaalajakson-vahimmaispituus')" :required="true">
            <template #default="{ uid }">
              <b-row>
                <b-col cols="2">
                  <b-row>
                    <b-col class="pr-1">
                      <b-form-input
                        :id="uid"
                        v-model="opas.yliopistosairaalajaksonVahimmaispituusVuodet"
                        :state="validateState('yliopistosairaalajaksonVahimmaispituusVuodet')"
                      ></b-form-input>
                      <b-form-invalid-feedback
                        :id="`${uid}-feedback`"
                        :state="validateState('yliopistosairaalajaksonVahimmaispituusVuodet')"
                      >
                        {{
                          opas.yliopistosairaalajaksonVahimmaispituusVuodet
                            ? $t('virheellinen-arvo')
                            : $t('pakollinen-tieto')
                        }}
                      </b-form-invalid-feedback>
                    </b-col>
                    <b-col cols="1" class="pl-1 mt-2">
                      <span>{{ 'v' }}</span>
                    </b-col>
                  </b-row>
                </b-col>
                <b-col cols="2">
                  <b-row>
                    <b-col class="pr-1">
                      <b-form-input
                        :id="uid"
                        v-model="opas.yliopistosairaalajaksonVahimmaispituusKuukaudet"
                        :state="validateState('yliopistosairaalajaksonVahimmaispituusKuukaudet')"
                      ></b-form-input>
                      <b-form-invalid-feedback
                        :id="`${uid}-feedback`"
                        :state="validateState('yliopistosairaalajaksonVahimmaispituusKuukaudet')"
                      >
                        {{
                          opas.yliopistosairaalajaksonVahimmaispituusKuukaudet
                            ? $t('virheellinen-arvo')
                            : $t('pakollinen-tieto')
                        }}
                      </b-form-invalid-feedback>
                    </b-col>
                    <b-col cols="1" class="pl-1 mt-2">
                      <span>{{ 'kk' }}</span>
                    </b-col>
                  </b-row>
                </b-col>
              </b-row>
            </template>
          </elsa-form-group>
          <b-form-row>
            <elsa-form-group
              :label="$t('yek.muun-koulutuksen-vahimmaispituus')"
              class="col-xs-12 col-sm-6 col-md-6 pr-sm-3"
              :required="true"
            >
              <template #default="{ uid }">
                <b-row>
                  <b-col cols="4">
                    <b-row>
                      <b-col class="pr-1">
                        <b-form-input
                          :id="uid"
                          v-model="
                            opas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet
                          "
                          :state="
                            validateState(
                              'yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet'
                            )
                          "
                        ></b-form-input>
                        <b-form-invalid-feedback
                          :id="`${uid}-feedback`"
                          :state="
                            validateState(
                              'yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet'
                            )
                          "
                        >
                          {{
                            opas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet
                              ? $t('virheellinen-arvo')
                              : $t('pakollinen-tieto')
                          }}
                        </b-form-invalid-feedback>
                      </b-col>
                      <b-col cols="1" class="pl-1 mt-2">
                        <span>{{ 'v' }}</span>
                      </b-col>
                    </b-row>
                  </b-col>
                  <b-col cols="4">
                    <b-row>
                      <b-col class="pr-1">
                        <b-form-input
                          :id="uid"
                          v-model="
                            opas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet
                          "
                          :state="
                            validateState(
                              'yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet'
                            )
                          "
                        ></b-form-input>
                        <b-form-invalid-feedback
                          :id="`${uid}-feedback`"
                          :state="
                            validateState(
                              'yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet'
                            )
                          "
                        >
                          {{
                            opas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet
                              ? $t('virheellinen-arvo')
                              : $t('pakollinen-tieto')
                          }}
                        </b-form-invalid-feedback>
                      </b-col>
                      <b-col cols="1" class="pl-1 mt-2">
                        <span>{{ 'kk' }}</span>
                      </b-col>
                    </b-row>
                  </b-col>
                  <b-col></b-col>
                  <b-col class="mt-2">
                    <span>{{ '-' }}</span>
                  </b-col>
                </b-row>
              </template>
            </elsa-form-group>
            <elsa-form-group
              :label="$t('yek.muun-koulutuksen-maksimipituus')"
              class="col-xs-12 col-sm-6 col-md-6 pr-sm-3"
            >
              <template #default="{ uid }">
                <b-row>
                  <b-col cols="4">
                    <b-row>
                      <b-col class="pr-1">
                        <b-form-input
                          :id="uid"
                          v-model="
                            opas.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusVuodet
                          "
                          :state="
                            validateState(
                              'yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusVuodet'
                            )
                          "
                        ></b-form-input>
                        <b-form-invalid-feedback
                          :id="`${uid}-feedback`"
                          :state="
                            validateState(
                              'yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusVuodet'
                            )
                          "
                        >
                          {{ $t('virheellinen-arvo') }}
                        </b-form-invalid-feedback>
                      </b-col>
                      <b-col cols="1" class="pl-1 mt-2">
                        <span>{{ 'v' }}</span>
                      </b-col>
                    </b-row>
                  </b-col>
                  <b-col cols="4">
                    <b-row>
                      <b-col class="pr-1">
                        <b-form-input
                          :id="uid"
                          v-model="
                            opas.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusKuukaudet
                          "
                          :state="
                            validateState(
                              'yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusKuukaudet'
                            )
                          "
                        ></b-form-input>
                        <b-form-invalid-feedback
                          :id="`${uid}-feedback`"
                          :state="
                            validateState(
                              'yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusKuukaudet'
                            )
                          "
                        >
                          {{ $t('virheellinen-arvo') }}
                        </b-form-invalid-feedback>
                      </b-col>
                      <b-col cols="1" class="pl-1 mt-2">
                        <span>{{ 'kk' }}</span>
                      </b-col>
                    </b-row>
                  </b-col>
                </b-row>
              </template>
            </elsa-form-group>
          </b-form-row>
          <elsa-form-group :label="$t('teoriakoulutusten-vahimmaismaara')" :required="true">
            <template #default="{ uid }">
              <b-row>
                <b-col cols="2">
                  <b-row align-v="center">
                    <b-col class="pr-1">
                      <b-form-input
                        :id="uid"
                        v-model="opas.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara"
                        :state="validateState('erikoisalanVaatimaTeoriakoulutustenVahimmaismaara')"
                      ></b-form-input>
                      <b-form-invalid-feedback
                        :id="`${uid}-feedback`"
                        :state="validateState('erikoisalanVaatimaTeoriakoulutustenVahimmaismaara')"
                      >
                        {{
                          opas.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara
                            ? $t('virheellinen-arvo')
                            : $t('pakollinen-tieto')
                        }}
                      </b-form-invalid-feedback>
                    </b-col>
                    <b-col cols="1" class="pl-1">
                      <span>{{ 'tuntia' }}</span>
                    </b-col>
                  </b-row>
                </b-col>
              </b-row>
            </template>
          </elsa-form-group>
          <div class="text-right">
            <elsa-button
              variant="back"
              :to="{
                name: opas.id == null ? 'erikoisala' : 'opintoopas'
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
          <h5>{{ $t('opintooppaan-nimi') }}</h5>
          <p>{{ opas.nimi }}</p>
          <h5>{{ $t('voimassaolo') }}</h5>
          <p>
            {{ $date(opas.voimassaoloAlkaa) }} -
            {{ opas.voimassaoloPaattyy != null ? $date(opas.voimassaoloPaattyy) : '' }}
          </p>
          <hr />
          <h5>{{ $t('kaytannon-koulutuksen-vahimmaispituus') }}</h5>
          <p>
            <span v-if="opas.kaytannonKoulutuksenVahimmaispituusVuodet > 0">
              {{ opas.kaytannonKoulutuksenVahimmaispituusVuodet }} {{ 'v' }}
            </span>
            <span
              v-if="
                opas.kaytannonKoulutuksenVahimmaispituusKuukaudet > 0 ||
                opas.kaytannonKoulutuksenVahimmaispituusVuodet === 0
              "
            >
              {{ opas.kaytannonKoulutuksenVahimmaispituusKuukaudet }} {{ 'kk' }}
            </span>
          </p>
          <h5>{{ $t('yek.tk-jakson-vahimmaispituus') }}</h5>
          <p>
            <span v-if="opas.terveyskeskuskoulutusjaksonVahimmaispituusVuodet > 0">
              {{ opas.terveyskeskuskoulutusjaksonVahimmaispituusVuodet }} {{ 'v' }}
            </span>
            <span
              v-if="
                opas.terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet > 0 ||
                opas.terveyskeskuskoulutusjaksonVahimmaispituusVuodet === 0
              "
            >
              {{ opas.terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet }} {{ 'kk' }}
            </span>
          </p>
          <h5>{{ $t('yek.sairaalajakson-vahimmaispituus') }}</h5>
          <p>
            <span v-if="opas.yliopistosairaalajaksonVahimmaispituusVuodet > 0">
              {{ opas.yliopistosairaalajaksonVahimmaispituusVuodet }} {{ 'v' }}
            </span>
            <span
              v-if="
                opas.yliopistosairaalajaksonVahimmaispituusKuukaudet > 0 ||
                opas.yliopistosairaalajaksonVahimmaispituusVuodet === 0
              "
            >
              {{ opas.yliopistosairaalajaksonVahimmaispituusKuukaudet }} {{ 'kk' }}
            </span>
          </p>
          <h5>{{ $t('yek.muun-koulutuksen-pituus') }}</h5>
          <p>
            <span v-if="opas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet > 0">
              {{ opas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet }} {{ 'v' }}
            </span>
            <span
              v-if="
                opas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet > 0 ||
                opas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet === 0
              "
            >
              {{ opas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet }}
              {{ 'kk' }}
            </span>
            <span v-if="showMuunKoulutuksenMaksimipituus">
              <span>{{ '-' }}</span>
              <span
                v-if="
                  opas.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusVuodet &&
                  opas.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusVuodet > 0
                "
              >
                {{ opas.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusVuodet }} {{ 'v' }}
              </span>
              <span
                v-if="
                  (opas.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusKuukaudet &&
                    opas.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusKuukaudet > 0) ||
                  opas.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusVuodet === 0
                "
              >
                {{ opas.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusKuukaudet }}
                {{ 'kk' }}
              </span>
            </span>
          </p>
          <h5>{{ $t('teoriakoulutusten-vahimmaismaara') }}</h5>
          <p>{{ opas.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara }} {{ $t('tuntia') }}</p>
        </div>
      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
  import { Component, Mixins, Prop, Vue } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { maxValue, minValue, required, requiredIf } from 'vuelidate/lib/validators'

  import { getUusinOpas } from '@/api/tekninen-paakayttaja'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import { Opintoopas } from '@/types'

  @Component({
    components: {
      ElsaButton,
      ElsaFormGroup,
      ElsaFormError,
      ElsaFormDatepicker
    }
  })
  export default class YekOpintoopasForm extends Mixins(validationMixin) {
    $refs!: {
      voimassaoloAlkaa: ElsaFormDatepicker
      voimassaoloPaattyy: ElsaFormDatepicker
    }

    @Prop({ required: false, default: false })
    editing!: boolean

    @Prop({ required: true, type: Object })
    opas!: Opintoopas

    @Prop({ required: true })
    erikoisalaId!: number

    params = {
      saving: false
    }

    validations() {
      return {
        opas: {
          nimi: {
            required
          },
          voimassaoloAlkaa: {
            required
          },
          kaytannonKoulutuksenVahimmaispituusVuodet: {
            required: requiredIf(() => {
              return !this.opas.kaytannonKoulutuksenVahimmaispituusKuukaudet
            }),
            minValue: minValue(0)
          },
          kaytannonKoulutuksenVahimmaispituusKuukaudet: {
            required: requiredIf(() => {
              return !this.opas.kaytannonKoulutuksenVahimmaispituusVuodet
            }),
            minValue: minValue(0),
            maxValue: maxValue(12)
          },
          terveyskeskuskoulutusjaksonVahimmaispituusVuodet: {
            required: requiredIf(() => {
              return !this.opas.terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet
            }),
            minValue: minValue(0)
          },
          terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet: {
            required: requiredIf(() => {
              return !this.opas.terveyskeskuskoulutusjaksonVahimmaispituusVuodet
            }),
            minValue: minValue(0),
            maxValue: maxValue(12)
          },
          yliopistosairaalajaksonVahimmaispituusVuodet: {
            required: requiredIf(() => {
              return !this.opas.yliopistosairaalajaksonVahimmaispituusKuukaudet
            }),
            minValue: minValue(0)
          },
          yliopistosairaalajaksonVahimmaispituusKuukaudet: {
            required: requiredIf(() => {
              return !this.opas.yliopistosairaalajaksonVahimmaispituusVuodet
            }),
            minValue: minValue(0),
            maxValue: maxValue(12)
          },
          yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet: {
            required: requiredIf(() => {
              return !this.opas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet
            }),
            minValue: minValue(0)
          },
          yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet: {
            required: requiredIf(() => {
              return !this.opas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet
            }),
            minValue: minValue(0),
            maxValue: maxValue(12)
          },
          yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusVuodet: {
            minValue: minValue(0)
          },
          yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusKuukaudet: {
            minValue: minValue(0),
            maxValue: maxValue(12)
          },
          erikoisalanVaatimaTeoriakoulutustenVahimmaismaara: {
            required,
            minValue: minValue(0)
          }
        }
      }
    }

    get showMuunKoulutuksenMaksimipituus() {
      return (
        this.opas?.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet !=
          this.opas?.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusVuodet ||
        this.opas?.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet !=
          this.opas?.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusKuukaudet
      )
    }

    async tuoOppaanTiedot() {
      const uusinOpas = (await getUusinOpas(this.$route.params.erikoisalaId)).data
      this.opas.kaytannonKoulutuksenVahimmaispituusVuodet =
        uusinOpas.kaytannonKoulutuksenVahimmaispituusVuodet
      this.opas.kaytannonKoulutuksenVahimmaispituusKuukaudet =
        uusinOpas.kaytannonKoulutuksenVahimmaispituusKuukaudet
      this.opas.terveyskeskuskoulutusjaksonVahimmaispituusVuodet =
        uusinOpas.terveyskeskuskoulutusjaksonVahimmaispituusVuodet
      this.opas.terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet =
        uusinOpas.terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet
      this.opas.yliopistosairaalajaksonVahimmaispituusVuodet =
        uusinOpas.yliopistosairaalajaksonVahimmaispituusVuodet
      this.opas.yliopistosairaalajaksonVahimmaispituusKuukaudet =
        uusinOpas.yliopistosairaalajaksonVahimmaispituusKuukaudet
      this.opas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet =
        uusinOpas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet
      this.opas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet =
        uusinOpas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet
      this.opas.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara =
        uusinOpas.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara
      Vue.set(
        this.opas,
        'yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusVuodet',
        uusinOpas.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusVuodet
      )
      Vue.set(
        this.opas,
        'yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusKuukaudet',
        uusinOpas.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusKuukaudet
      )
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.opas[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    validateForm(): boolean {
      this.$v.opas.$touch()
      return !this.$v.$anyError
    }

    onSubmit() {
      const validations = [
        this.validateForm(),
        this.$refs.voimassaoloAlkaa.validateForm(),
        this.$refs.voimassaoloPaattyy.validateForm()
      ]

      if (validations.includes(false)) {
        return
      }
      this.$emit(
        'submit',
        {
          ...this.opas
        },
        this.params
      )
    }
  }
</script>

<style lang="scss" scoped>
  .input-group-fi {
    padding-left: 0.95rem;
    padding-right: 0.95rem;
  }
</style>
