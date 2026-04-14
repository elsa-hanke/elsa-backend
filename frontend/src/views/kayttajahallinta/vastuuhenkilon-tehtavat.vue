<template>
  <div v-if="!loading">
    <div
      v-for="(yliopistoErikoisala, yliopistoErikoisalaIndex) in form.yliopistotAndErikoisalat"
      :key="yliopistoErikoisala.id"
      class="border rounded p-3 mb-4"
    >
      <elsa-form-group
        :label="$t('erikoisala')"
        :required="allowEditing && !yliopistoErikoisala.id"
      >
        <template #default="{ uid }">
          <div :class="[allowEditing && !yliopistoErikoisala.id ? 'd-flex' : 'd-inline-flex']">
            <div v-if="allowEditing && !yliopistoErikoisala.id" class="mr-2 w-100">
              <elsa-form-multiselect
                :id="uid"
                v-model="yliopistoErikoisala.erikoisala"
                :options="erikoisalatOptions"
                label="nimi"
                :state="validateErikoisala(yliopistoErikoisalaIndex)"
                track-by="id"
                @select="onErikoisalaSelect($event, yliopistoErikoisalaIndex, yliopistoErikoisala)"
                @input="$emit('skipRouteExitConfirm', false)"
              ></elsa-form-multiselect>
              <b-form-invalid-feedback :state="validateErikoisala(yliopistoErikoisalaIndex)">
                {{ $t('pakollinen-tieto') }}
              </b-form-invalid-feedback>
            </div>
            <div v-else-if="yliopistoErikoisala.erikoisala">
              <span :id="uid" class="mr-1">{{ yliopistoErikoisala.erikoisala.nimi }}</span>
            </div>
            <elsa-button
              v-if="
                allowEditing &&
                form.yliopistotAndErikoisalat.length > 1 &&
                yliopistoErikoisala.erikoisala
              "
              variant="outline-primary"
              class="border-0 p-0"
              :disabled="!allowErikoisalaDelete(yliopistoErikoisala.erikoisala)"
              @click="
                onDeleteErikoisala(
                  yliopistoErikoisalaIndex,
                  yliopistoErikoisala.erikoisala,
                  yliopistoErikoisala.vastuuhenkilonTehtavat || []
                )
              "
            >
              <font-awesome-icon :icon="['far', 'trash-alt']" fixed-width size="lg" />
            </elsa-button>
          </div>
          <div class="mb-3 mt-2">
            <span
              v-if="
                allowEditing &&
                yliopistoErikoisala.erikoisala &&
                erikoisalatWhichShouldBeAddedIntoAnotherVastuuhenkilo.includes(
                  yliopistoErikoisala.erikoisala.id
                )
              "
              class="text-danger text-size-sm"
            >
              {{ $t('kay-ensin-lisaamassa-erikoisala-toiselle-vastuuhenkilolle') }}
            </span>
            <span
              v-else-if="
                allowEditing &&
                yliopistoErikoisala.erikoisala &&
                erikoisalatWithTehtavatWhichShouldBeReassigned.includes(
                  yliopistoErikoisala.erikoisala.id
                )
              "
              class="text-danger text-size-sm"
            >
              {{ $t('lisaa-tehtavat-ensin-toiselle-vastuuhenkilolle') }}
            </span>
          </div>
        </template>
      </elsa-form-group>
      <elsa-form-group
        v-if="yliopistoErikoisala && yliopistoErikoisala.erikoisala"
        :label="$t('vastuualueet-elsassa')"
      >
        <template
          v-if="
            allowEditing &&
            getVastuuhenkilotByErikoisala(yliopistoErikoisala.erikoisala.id).length === 0
          "
          #label-help
        >
          <elsa-popover class="align-top">
            <p class="mb-0">{{ $t('taman-erikoisalan-tehtavia-ei-voi-muokata') }}</p>
          </elsa-popover>
        </template>
        <template #default="{ uid }">
          <div
            v-if="
              yliopistoErikoisala.vastuuhenkilonTehtavat &&
              (!allowEditing ||
                getVastuuhenkilotByErikoisala(yliopistoErikoisala.erikoisala.id).length === 0)
            "
          >
            <div v-if="!hasAnyTehtava(yliopistoErikoisala.vastuuhenkilonTehtavat)">
              {{ $t('ei-vastuualueita') }}
            </div>
            <div
              v-for="(tehtava, index) in yliopistoErikoisala.vastuuhenkilonTehtavat"
              v-else
              :id="uid"
              :key="index"
              class="mb-1"
            >
              <div v-if="tehtava">
                {{ $t(`vastuualue.${tehtava.nimi}`) }}
              </div>
            </div>
          </div>
          <div
            v-else-if="yliopistoErikoisala.erikoisala && yliopistoErikoisala.vastuuhenkilonTehtavat"
          >
            <div
              v-for="(tehtava, tehtavaIndex) in getVastuuhenkilonTehtavatyypit(
                yliopistoErikoisala.erikoisala.id
              )"
              :key="tehtava.id"
            >
              <b-form-checkbox
                v-model="yliopistoErikoisala.vastuuhenkilonTehtavat[tehtavaIndex]"
                :value="tehtava"
                class="mb-1"
                :disabled="disabled"
                @input="$emit('skipRouteExitConfirm', false)"
                @change="
                  onTehtavaChanged(
                    $event,
                    yliopistoErikoisala,
                    tehtava.id,
                    yliopistoErikoisalaIndex,
                    tehtavaIndex
                  )
                "
              >
                {{ $t(`vastuualue.${tehtava.nimi}`) }}
              </b-form-checkbox>
              <div
                v-if="
                  !tehtavatContains(yliopistoErikoisala, tehtava.id) &&
                  existingTehtavaRemoved(yliopistoErikoisala, tehtava.id)
                "
                class="pl-4"
              >
                <div class="font-weight-500 mt-2">
                  {{ $t('valitse-kenelle-haluat-siirtaa-vastuualueen') }}
                </div>
                <elsa-form-group class="mb-3 mt-2" :required="true" :label="$t('vastuuhenkilo')">
                  <elsa-form-multiselect
                    v-model="
                      form.erikoisalatForTehtavat[yliopistoErikoisalaIndex].reassignedTehtavat[
                        tehtavaIndex
                      ]
                    "
                    :options="
                      vastuuhenkilotOptionsByErikoisalaId(
                        yliopistoErikoisala.erikoisala.id,
                        tehtava.id
                      )
                    "
                    :state="validateTehtavat(yliopistoErikoisalaIndex, tehtavaIndex)"
                    :disabled="disabled"
                    label="label"
                    :track-by="tehtava.id"
                    @input="$emit('skipRouteExitConfirm', false)"
                  />
                  <b-form-invalid-feedback
                    :state="validateTehtavat(yliopistoErikoisalaIndex, tehtavaIndex)"
                  >
                    {{ $t('lisaa-vastuualue-ensin-toiselle-kayttajalle') }}
                  </b-form-invalid-feedback>
                </elsa-form-group>
              </div>
              <div
                v-if="showTehtavaRemovedFromVastuuhenkiloText(yliopistoErikoisala, tehtava.id)"
                class="pl-4 mb-2"
              >
                <span>
                  {{
                    `${$t(
                      'valinta-poistaa-vastuualueen-aiemmalta-vastuuhenkilolta'
                    )}: ${getNimiForVastuuhenkiloWithRemovedTehtava(
                      yliopistoErikoisala.erikoisala.id,
                      tehtava.id
                    )}`
                  }}
                </span>
              </div>
            </div>
          </div>
        </template>
      </elsa-form-group>
    </div>
    <elsa-button
      v-if="allowEditing"
      variant="link"
      size="md"
      class="text-decoration-none shadow-none p-0"
      @click="addErikoisala(false)"
    >
      <font-awesome-icon icon="plus" fixed-width size="sm" />
      {{ $t('useampi-erikoisala') }}
    </elsa-button>
  </div>
  <div v-else class="text-center mb-4">
    <b-spinner variant="primary" :label="$t('ladataan')" />
  </div>
</template>

<script lang="ts">
  import { Component, Prop, Mixins, Watch } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { required } from 'vuelidate/lib/validators'

  import { getVastuuhenkilonTehtavatForm } from '@/api/kayttajahallinta'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import {
    KayttajaYliopistoErikoisala,
    KayttajahallintaVastuuhenkilonTehtavatLomake,
    VastuuhenkilonTehtavatLomake,
    ErikoisalaForVastuuhenkilonTehtavat,
    ReassignedVastuuhenkilonTehtava,
    VastuuhenkilonTehtava,
    Yliopisto,
    Erikoisala
  } from '@/types'
  import { ReassignedVastuuhenkilonTehtavaTyyppi } from '@/utils/constants'
  import { sortByAsc } from '@/utils/sort'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaFormGroup,
      ElsaFormMultiselect,
      ElsaButton,
      ElsaPopover
    }
  })
  export default class VastuuhenkilonTehtavat extends Mixins(validationMixin) {
    validations() {
      return {
        form: {
          yliopistotAndErikoisalat: {
            $each: {
              erikoisala: { required }
            }
          },
          erikoisalatForTehtavat: {
            $each: {
              reassignedTehtavat: {
                $each: { isNotNull: (obj: any) => this.isNotNull(obj) }
              }
            }
          }
        }
      }
    }

    @Prop({ required: false, type: Number })
    kayttajaId?: number

    @Prop({ required: true, type: Object })
    yliopisto!: Yliopisto

    @Prop({ required: false, default: () => [] })
    yliopistotAndErikoisalat?: KayttajaYliopistoErikoisala[]

    @Prop({ required: false, type: Boolean, default: false })
    editing?: boolean

    @Prop({ required: false, type: Boolean, default: false })
    newVastuuhenkilo?: boolean

    @Prop({ required: false, type: Boolean, default: false })
    disabled?: boolean

    form: VastuuhenkilonTehtavatLomake = {
      yliopistotAndErikoisalat: [],
      erikoisalatForTehtavat: []
    }

    formData: KayttajahallintaVastuuhenkilonTehtavatLomake | null = null
    loading = true
    erikoisalatWhichShouldBeAddedIntoAnotherVastuuhenkilo: number[] = []
    erikoisalatWithTehtavatWhichShouldBeReassigned: number[] = []

    async mounted() {
      await this.fetch(this.yliopisto.id)
    }

    async fetch(yliopistoId?: number) {
      this.loading = true
      try {
        if (yliopistoId) {
          this.formData = (await getVastuuhenkilonTehtavatForm(yliopistoId)).data
          this.initForm()
          if (this.newVastuuhenkilo) {
            this.addErikoisala(true)
          }
        }
      } catch {
        toastFail(this, this.$t('lomakkeen-tietojen-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'kayttajahallinta' })
      }
      this.loading = false
    }

    @Watch('yliopisto')
    async onPropertyChanged(yliopisto: Yliopisto) {
      await this.fetch(yliopisto.id)
    }

    initForm() {
      this.$v.form.$reset()
      this.form.yliopistotAndErikoisalat = []
      this.form.erikoisalatForTehtavat = []

      this.yliopistotAndErikoisalat?.forEach((ye) => {
        if (!ye.erikoisala?.id) return
        const newYliopistoErikoisala = Object.assign({}, ye)
        newYliopistoErikoisala.vastuuhenkilonTehtavat = []
        const vastuuhenkilonTehtavatyypit = this.getVastuuhenkilonTehtavatyypit(ye.erikoisala.id)
        vastuuhenkilonTehtavatyypit?.forEach((vt, index) => {
          if (vt.id && this.tehtavatContains(ye, vt.id)) {
            newYliopistoErikoisala.vastuuhenkilonTehtavat[index] = Object.assign({}, vt)
          }
        })
        this.form.yliopistotAndErikoisalat.push(newYliopistoErikoisala)
        this.form.erikoisalatForTehtavat.push({
          erikoisalaId: ye.erikoisala?.id,
          reassignedTehtavat: []
        } as ErikoisalaForVastuuhenkilonTehtavat)
        this.erikoisalatWhichShouldBeAddedIntoAnotherVastuuhenkilo = []
        this.erikoisalatWithTehtavatWhichShouldBeReassigned = []
      })
    }

    isNotNull(obj: any) {
      return obj !== null
    }

    onTehtavaChanged(
      vastuuhenkilonTehtava: VastuuhenkilonTehtava,
      yliopistoErikoisala: Partial<KayttajaYliopistoErikoisala>,
      tehtavaId: number,
      yliopistoErikoisalaIndex: number,
      tehtavaIndex: number
    ) {
      if (vastuuhenkilonTehtava) {
        if (
          (yliopistoErikoisala.erikoisala?.id &&
            this.isNewErikoisala(yliopistoErikoisala.erikoisala)) ||
          this.newTehtavaAdded(yliopistoErikoisala, tehtavaId)
        ) {
          this.setRemovedTehtava(
            yliopistoErikoisala,
            tehtavaId,
            yliopistoErikoisalaIndex,
            tehtavaIndex
          )
        } else {
          this.clearReassignedTehtava(yliopistoErikoisalaIndex, tehtavaIndex)
        }
      } else if (this.existingTehtavaRemoved(yliopistoErikoisala, tehtavaId)) {
        this.setTehtavaWhichShouldBeReassigned(yliopistoErikoisalaIndex, tehtavaIndex)
      } else {
        this.clearReassignedTehtava(yliopistoErikoisalaIndex, tehtavaIndex)
      }
    }

    setRemovedTehtava(
      yliopistoErikoisala: Partial<KayttajaYliopistoErikoisala>,
      tehtavaId: number,
      yliopistoErikoisalaIndex: number,
      tehtavaIndex: number
    ) {
      const kayttajaYliopistoErikoisalaForRemovedTehtava =
        this.yliopistotAndErikoisalatForVastuuhenkilot?.find(
          (ye) =>
            ye.erikoisala &&
            ye.erikoisala.id === yliopistoErikoisala.erikoisala?.id &&
            ye.vastuuhenkilonTehtavat.find((vt) => vt.id === tehtavaId)
        )
      this.$set(
        this.form.erikoisalatForTehtavat[yliopistoErikoisalaIndex].reassignedTehtavat,
        tehtavaIndex,
        {
          kayttajaYliopistoErikoisala: kayttajaYliopistoErikoisalaForRemovedTehtava,
          tehtavaId: tehtavaId,
          tyyppi: ReassignedVastuuhenkilonTehtavaTyyppi.REMOVE
        } as ReassignedVastuuhenkilonTehtava
      )
    }

    setTehtavaWhichShouldBeReassigned(yliopistoErikoisalaIndex: number, tehtavaIndex: number) {
      this.$set(
        this.form.erikoisalatForTehtavat[yliopistoErikoisalaIndex].reassignedTehtavat,
        tehtavaIndex,
        null as unknown as ReassignedVastuuhenkilonTehtava
      )
    }

    clearReassignedTehtava(yliopistoErikoisalaIndex: number, tehtavaIndex: number) {
      this.$set(
        this.form.erikoisalatForTehtavat[yliopistoErikoisalaIndex].reassignedTehtavat,
        tehtavaIndex,
        undefined as unknown as ReassignedVastuuhenkilonTehtava
      )
    }

    addErikoisala(skipRouteExitConfirm: boolean) {
      this.$emit('skipRouteExitConfirm', skipRouteExitConfirm)
      this.form.yliopistotAndErikoisalat.push({
        yliopisto: this.yliopisto,
        vastuuhenkilonTehtavat: []
      } as Partial<KayttajaYliopistoErikoisala>)
    }

    onErikoisalaSelect(
      erikoisala: Erikoisala,
      yliopistoErikoisalaIndex: number,
      yliopistoErikoisala: Partial<KayttajaYliopistoErikoisala>
    ) {
      if (erikoisala.id) {
        yliopistoErikoisala.vastuuhenkilonTehtavat = []

        if (this.getVastuuhenkilotByErikoisala(erikoisala.id)?.length === 0) {
          erikoisala.vastuuhenkilonTehtavatyypit.forEach((vt) =>
            yliopistoErikoisala.vastuuhenkilonTehtavat?.push(Object.assign({}, vt))
          )
        } else {
          this.form.erikoisalatForTehtavat[yliopistoErikoisalaIndex] = {
            erikoisalaId: erikoisala.id,
            reassignedTehtavat: []
          } as ErikoisalaForVastuuhenkilonTehtavat
        }
      }
    }

    onDeleteErikoisala(
      yliopistoErikoisalaIndex: number,
      erikoisala: Erikoisala | undefined,
      tehtavat: VastuuhenkilonTehtava[]
    ) {
      if (
        erikoisala &&
        erikoisala.id &&
        !this.isNewErikoisala(erikoisala) &&
        !this.anotherVastuuhenkiloHasErikoisala(erikoisala.id)
      ) {
        this.erikoisalatWhichShouldBeAddedIntoAnotherVastuuhenkilo.push(erikoisala.id)
        return
      }

      if (
        erikoisala &&
        erikoisala.id &&
        !this.isNewErikoisala(erikoisala) &&
        tehtavat.length !== 0
      ) {
        this.erikoisalatWithTehtavatWhichShouldBeReassigned.push(erikoisala.id)
        return
      }

      this.$emit('skipRouteExitConfirm', false)
      this.form.yliopistotAndErikoisalat.splice(yliopistoErikoisalaIndex, 1)
      this.form.erikoisalatForTehtavat.splice(yliopistoErikoisalaIndex, 1)
    }

    isNewErikoisala(erikoisala: Erikoisala | undefined) {
      return (
        this.yliopistotAndErikoisalat?.find((ye) => ye.erikoisala?.id === erikoisala?.id) ===
        undefined
      )
    }

    anotherVastuuhenkiloHasErikoisala(erikoisalaId: number) {
      return (
        erikoisalaId &&
        this.yliopistotAndErikoisalatForVastuuhenkilot?.find(
          (ye) => ye.erikoisala?.id === erikoisalaId
        ) !== undefined
      )
    }

    existingTehtavaRemoved(
      yliopistoErikoisala: Partial<KayttajaYliopistoErikoisala>,
      tehtavaId: number
    ) {
      const originalYliopistoErikoisala = this.getOriginalYliopistoErikoisala(
        yliopistoErikoisala.id
      )
      return (
        originalYliopistoErikoisala && this.tehtavatContains(originalYliopistoErikoisala, tehtavaId)
      )
    }

    newTehtavaAdded(yliopistoErikoisala: Partial<KayttajaYliopistoErikoisala>, tehtavaId: number) {
      const originalYliopistoErikoisala = this.getOriginalYliopistoErikoisala(
        yliopistoErikoisala.id
      )
      return (
        originalYliopistoErikoisala &&
        !this.tehtavatContains(originalYliopistoErikoisala, tehtavaId)
      )
    }

    showTehtavaRemovedFromVastuuhenkiloText(
      yliopistoErikoisala: Partial<KayttajaYliopistoErikoisala>,
      tehtavaId: number
    ) {
      return (
        this.tehtavatContains(yliopistoErikoisala, tehtavaId) &&
        (this.newVastuuhenkilo ||
          this.isNewErikoisala(yliopistoErikoisala.erikoisala) ||
          this.newTehtavaAdded(yliopistoErikoisala, tehtavaId))
      )
    }

    tehtavatContains(yliopistoErikoisala: Partial<KayttajaYliopistoErikoisala>, tehtavaId: number) {
      return (
        yliopistoErikoisala.vastuuhenkilonTehtavat?.find((vt) => vt && vt.id === tehtavaId) !==
        undefined
      )
    }

    getOriginalYliopistoErikoisala(yliopistoErikoisalaId: number | undefined) {
      return this.yliopistotAndErikoisalat?.find(
        (data: KayttajaYliopistoErikoisala) =>
          yliopistoErikoisalaId && data.id === yliopistoErikoisalaId
      )
    }

    getVastuuhenkilonTehtavatyypit(erikoisalaId: number) {
      return this.formData?.erikoisalat.find((e) => e.id === erikoisalaId)
        ?.vastuuhenkilonTehtavatyypit
    }

    getNimiForVastuuhenkiloWithRemovedTehtava(erikoisalaId: number, tehtavaId: number) {
      const vastuuhenkiloWithRemovedTehtava = this.vastuuhenkilot?.find((v) =>
        v.yliopistotAndErikoisalat
          .find((ye) => ye.erikoisala?.id === erikoisalaId)
          ?.vastuuhenkilonTehtavat.find((vt) => vt.id === tehtavaId)
      )
      return `${vastuuhenkiloWithRemovedTehtava?.etunimi} ${vastuuhenkiloWithRemovedTehtava?.sukunimi}`
    }

    getVastuuhenkilotByErikoisala(erikoisalaId: number) {
      return (
        this.vastuuhenkilot?.filter((v) =>
          v.yliopistotAndErikoisalat.find((ye) => ye.erikoisala?.id === erikoisalaId)
        ) || []
      )
    }

    vastuuhenkilotOptionsByErikoisalaId(erikoisalaId: number, tehtavaId: number) {
      return this.getVastuuhenkilotByErikoisala(erikoisalaId)?.map(
        (v) =>
          ({
            label: `${v.etunimi} ${v.sukunimi}`,
            kayttajaYliopistoErikoisala: v.yliopistotAndErikoisalat.find(
              (ye) => ye.erikoisala?.id === erikoisalaId
            ),
            tehtavaId: tehtavaId,
            tyyppi: ReassignedVastuuhenkilonTehtavaTyyppi.ADD
          } as ReassignedVastuuhenkilonTehtava)
      )
    }

    hasAnyTehtava(tehtavat: VastuuhenkilonTehtava[]) {
      return tehtavat.filter((t: VastuuhenkilonTehtava | boolean) => t !== false).length > 0
    }

    allowErikoisalaDelete(erikoisala: Erikoisala) {
      return (
        this.newVastuuhenkilo ||
        !erikoisala ||
        !erikoisala.id ||
        (!this.disabled &&
          !this.erikoisalatWhichShouldBeAddedIntoAnotherVastuuhenkilo.includes(erikoisala.id) &&
          !this.erikoisalatWithTehtavatWhichShouldBeReassigned.includes(erikoisala.id))
      )
    }

    validateState(v: any, name?: string) {
      const { $dirty, $error } = name ? v[name] : v ?? true
      return $dirty ? ($error ? false : null) : null
    }

    validateErikoisala(index: number) {
      const { $dirty, $error } = this.$v.form.yliopistotAndErikoisalat?.$each[index]
        ?.erikoisala as any
      return $dirty ? !$error : null
    }

    validateTehtavat(yliopistoErikoisalaIndex: number, tehtavaIndex: number) {
      const { $dirty, $error } = this.$v.form.erikoisalatForTehtavat?.$each[
        yliopistoErikoisalaIndex
      ]?.reassignedTehtavat.$each[tehtavaIndex] as any
      return $dirty ? !$error : null
    }

    getFormIfValid() {
      this.$v.form.$touch()
      return !this.$v.form.$invalid ? this.form : null
    }

    get vastuuhenkilot() {
      return this.kayttajaId
        ? this.formData?.vastuuhenkilot.filter((v) => v.id !== this.kayttajaId)
        : this.formData?.vastuuhenkilot
    }

    get yliopistotAndErikoisalatForVastuuhenkilot() {
      return this.vastuuhenkilot?.map((v) => v.yliopistotAndErikoisalat).flat()
    }

    get allowEditing() {
      return this.editing || this.newVastuuhenkilo
    }

    get erikoisalatOptions() {
      const excludedErikoisalaIds = this.form.yliopistotAndErikoisalat.map(
        (ye) => ye.erikoisala?.id
      )
      return this.formData?.erikoisalat
        .filter((e) => !excludedErikoisalaIds.includes(e.id))
        .sort((a, b) => sortByAsc(a.nimi, b.nimi))
    }
  }
</script>
