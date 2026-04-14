<template>
  <b-form @submit.stop.prevent="onSubmit">
    <elsa-form-group
      :label="$t('tyoskentelyjakso')"
      :add-new-enabled="true"
      :add-new-label="$t('lisaa-tyoskentelyjakso')"
      :required="true"
      @submit="onTyoskentelyjaksoSubmit"
    >
      <template #modal-content="{ submit, cancel }">
        <tyoskentelyjakso-form
          :kunnat="kunnat"
          :erikoisalat="erikoisalat"
          @submit="submit"
          @cancel="cancel"
        />
      </template>
      <template #default="{ uid }">
        <elsa-form-multiselect
          :id="uid"
          v-model="form.tyoskentelyjakso"
          :options="tyoskentelyjaksotFormatted"
          :state="validateState('tyoskentelyjakso')"
          label="label"
          track-by="id"
          @input="$emit('skipRouteExitConfirm', false)"
          @select="onTyoskentelyjaksoSelect"
        />
        <b-form-invalid-feedback :id="`${uid}-feedback`">
          {{ $t('pakollinen-tieto') }}
        </b-form-invalid-feedback>
      </template>
    </elsa-form-group>
    <hr />
    <div v-if="value.id == null">
      <div v-for="(uusiSuorite, index) in uudetSuoritteet" :key="index">
        <elsa-form-group :label="$t('suorite')" :required="true">
          <template #default="{ uid }">
            <elsa-form-multiselect
              :id="uid"
              v-model="uusiSuorite.suorite"
              :options="suoritteenKategoriat"
              :state="validateSuoriteState(index)"
              group-values="suoritteet"
              group-label="nimi"
              :group-select="false"
              label="nimi"
              track-by="id"
              @input="$emit('skipRouteExitConfirm', false)"
            >
              <template #option="props">
                <span v-if="props.option.$isLabel">{{ props.option.$groupLabel }}</span>
                <span v-else class="d-inline-block ml-3">{{ props.option.nimi }}</span>
              </template>
            </elsa-form-multiselect>
            <b-form-invalid-feedback :id="`${uid}-feedback`">
              {{ $t('pakollinen-tieto') }}
            </b-form-invalid-feedback>
          </template>
        </elsa-form-group>
        <b-form-row>
          <elsa-form-group :label="arviointiAsteikonNimi" class="col-md-6 mb-2">
            <template #label-help>
              <elsa-popover :title="arviointiAsteikonNimi">
                <elsa-arviointiasteikon-taso-tooltip-content
                  :arviointiasteikon-tasot="arviointiasteikko.tasot"
                />
              </elsa-popover>
            </template>
            <template #default="{ uid }">
              <elsa-form-multiselect
                :id="uid"
                v-model="uusiSuorite.arviointiasteikonTaso"
                :options="arviointiasteikko.tasot"
                :custom-label="arviointiasteikonTasoLabel"
                track-by="taso"
                @input="$emit('skipRouteExitConfirm', false)"
              >
                <template #singleLabel="props">
                  <span class="font-weight-700">{{ props.option.taso }}</span>
                  {{ $t('arviointiasteikon-taso-' + props.option.nimi) }}
                </template>
                <template #option="props">
                  <span class="font-weight-700">{{ props.option.taso }}</span>
                  {{ $t('arviointiasteikon-taso-' + props.option.nimi) }}
                </template>
              </elsa-form-multiselect>
              <b-form-invalid-feedback :id="`${uid}-feedback`">
                {{ $t('pakollinen-tieto') }}
              </b-form-invalid-feedback>
            </template>
          </elsa-form-group>
          <elsa-form-group :label="$t('vaativuustaso')" class="col-md-6 mb-2">
            <template #label-help>
              <elsa-popover :title="$t('vaativuustaso')">
                <elsa-vaativuustaso-tooltip-content />
              </elsa-popover>
            </template>
            <template #default="{ uid }">
              <elsa-form-multiselect
                :id="uid"
                v-model="uusiSuorite.vaativuustaso"
                :options="vaativuustasot"
                :custom-label="vaativuustasoLabel"
                track-by="arvo"
                @input="$emit('skipRouteExitConfirm', false)"
              >
                <template #singleLabel="props">
                  <span class="font-weight-700">{{ props.option.arvo }}</span>
                  {{ $t(props.option.nimi) }}
                </template>
                <template #option="props">
                  <span class="font-weight-700">{{ props.option.arvo }}</span>
                  {{ $t(props.option.nimi) }}
                </template>
              </elsa-form-multiselect>
              <b-form-invalid-feedback :id="`${uid}-feedback`">
                {{ $t('pakollinen-tieto') }}
              </b-form-invalid-feedback>
            </template>
          </elsa-form-group>
        </b-form-row>
        <elsa-button
          v-if="index !== 0"
          variant="link"
          class="text-decoration-none shadow-none p-0"
          @click="deleteSuorite(index)"
        >
          <font-awesome-icon :icon="['far', 'trash-alt']" fixed-width size="sm" />
          {{ $t('poista-suorite') }}
        </elsa-button>
        <hr v-if="uudetSuoritteet.length > 1" />
      </div>
      <elsa-button
        variant="outline-primary"
        :class="uudetSuoritteet.length > 1 ? '' : 'mt-3'"
        class="lisaa-suorite"
        @click.stop.prevent="onLisaaSuorite"
      >
        {{ $t('lisaa-suorite') }}
      </elsa-button>
    </div>
    <div v-else>
      <elsa-form-group :label="$t('suorite')" :required="true">
        <template #default="{ uid }">
          <elsa-form-multiselect
            :id="uid"
            v-model="form.suorite"
            :options="suoritteenKategoriat"
            :state="validateState('suorite')"
            group-values="suoritteet"
            group-label="nimi"
            :group-select="false"
            label="nimi"
            track-by="id"
            @input="$emit('skipRouteExitConfirm', false)"
          >
            <template #option="props">
              <span v-if="props.option.$isLabel">{{ props.option.$groupLabel }}</span>
              <span v-else class="d-inline-block ml-3">{{ props.option.nimi }}</span>
            </template>
          </elsa-form-multiselect>
          <b-form-invalid-feedback :id="`${uid}-feedback`">
            {{ $t('pakollinen-tieto') }}
          </b-form-invalid-feedback>
        </template>
      </elsa-form-group>
      <b-form-row>
        <elsa-form-group :label="arviointiAsteikonNimi" class="col-md-6">
          <template #label-help>
            <elsa-popover :title="arviointiAsteikonNimi">
              <elsa-arviointiasteikon-taso-tooltip-content
                :arviointiasteikon-tasot="arviointiasteikko.tasot"
              />
            </elsa-popover>
          </template>
          <template #default="{ uid }">
            <elsa-form-multiselect
              :id="uid"
              v-model="form.arviointiasteikonTaso"
              :options="arviointiasteikko.tasot"
              :custom-label="arviointiasteikonTasoLabel"
              track-by="taso"
              @input="$emit('skipRouteExitConfirm', false)"
            >
              <template #singleLabel="props">
                <span class="font-weight-700">{{ props.option.taso }}</span>
                {{ $t('arviointiasteikon-taso-' + props.option.nimi) }}
              </template>
              <template #option="props">
                <span class="font-weight-700">{{ props.option.taso }}</span>
                {{ $t('arviointiasteikon-taso-' + props.option.nimi) }}
              </template>
            </elsa-form-multiselect>
            <b-form-invalid-feedback :id="`${uid}-feedback`">
              {{ $t('pakollinen-tieto') }}
            </b-form-invalid-feedback>
          </template>
        </elsa-form-group>
        <elsa-form-group :label="$t('vaativuustaso')" class="col-md-6">
          <template #label-help>
            <elsa-popover :title="$t('vaativuustaso')">
              <elsa-vaativuustaso-tooltip-content />
            </elsa-popover>
          </template>
          <template #default="{ uid }">
            <elsa-form-multiselect
              :id="uid"
              v-model="form.vaativuustaso"
              :options="vaativuustasot"
              :custom-label="vaativuustasoLabel"
              track-by="arvo"
              @input="$emit('skipRouteExitConfirm', false)"
            >
              <template #singleLabel="props">
                <span class="font-weight-700">{{ props.option.arvo }}</span>
                {{ $t(props.option.nimi) }}
              </template>
              <template #option="props">
                <span class="font-weight-700">{{ props.option.arvo }}</span>
                {{ $t(props.option.nimi) }}
              </template>
            </elsa-form-multiselect>
            <b-form-invalid-feedback :id="`${uid}-feedback`">
              {{ $t('pakollinen-tieto') }}
            </b-form-invalid-feedback>
          </template>
        </elsa-form-group>
      </b-form-row>
    </div>
    <hr />
    <elsa-form-group :label="$t('suorituspaiva')" :required="true">
      <template #default="{ uid }">
        <elsa-form-datepicker
          v-if="childDataReceived"
          :id="uid"
          ref="suorituspaiva"
          :value.sync="form.suorituspaiva"
          :min="tyoskentelyjaksonAlkamispaiva"
          :min-error-text="$t('suorituspaiva-ei-voi-olla-ennen-tyoskentelyjakson-alkamista')"
          :max="tyoskentelyjaksonPaattymispaiva"
          :max-error-text="$t('suorituspaiva-ei-voi-olla-tyoskentelyjakson-paattymisen-jalkeen')"
          class="col-md-4 pl-0"
          @input="$emit('skipRouteExitConfirm', false)"
        ></elsa-form-datepicker>
      </template>
    </elsa-form-group>
    <elsa-form-group :label="$t('lisatiedot')">
      <template #default="{ uid }">
        <b-form-textarea
          :id="uid"
          v-model="form.lisatiedot"
          rows="5"
          @input="$emit('skipRouteExitConfirm', false)"
        ></b-form-textarea>
      </template>
    </elsa-form-group>
    <div class="text-right">
      <elsa-button variant="back" :to="{ name: 'suoritemerkinnat' }">
        {{ $t('peruuta') }}
      </elsa-button>
      <elsa-button
        v-if="value.id"
        :loading="params.deleting"
        variant="outline-danger"
        @click="onSuoritemerkintaDelete"
      >
        {{ $t('poista-merkinta') }}
      </elsa-button>
      <elsa-button :loading="params.saving" type="submit" variant="primary" class="ml-2">
        {{ $t('tallenna') }}
      </elsa-button>
    </div>
    <div class="row">
      <elsa-form-error :active="$v.$anyError" />
    </div>
  </b-form>
</template>

<script lang="ts">
  import Component from 'vue-class-component'
  import { Mixins, Prop } from 'vue-property-decorator'
  import { Validation } from 'vuelidate'
  import { required, requiredIf } from 'vuelidate/lib/validators'

  import ElsaArviointiasteikonTasoTooltipContent from '@/components/arviointiasteikon-taso/arviointiasteikon-taso-tooltip.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import ElsaVaativuustasoTooltipContent from '@/components/vaativuustaso/vaativuustaso-tooltip-content.vue'
  import TyoskentelyjaksoForm from '@/forms/tyoskentelyjakso-form.vue'
  import TyoskentelyjaksoMixin from '@/mixins/tyoskentelyjakso'
  import {
    Arviointiasteikko,
    ArviointiasteikonTaso,
    Erikoisala,
    Kunta,
    SuoritemerkinnanSuorite,
    Suoritemerkinta,
    SuoritteenKategoria,
    Vaativuustaso
  } from '@/types'
  import { vaativuustasot, ArviointiasteikkoTyyppi } from '@/utils/constants'
  import { dateBetween } from '@/utils/date'

  @Component({
    components: {
      TyoskentelyjaksoForm,
      ElsaFormGroup,
      ElsaFormError,
      ElsaFormMultiselect,
      ElsaPopover,
      ElsaFormDatepicker,
      ElsaButton,
      ElsaVaativuustasoTooltipContent,
      ElsaArviointiasteikonTasoTooltipContent
    }
  })
  export default class SuoritemerkintaForm extends Mixins(TyoskentelyjaksoMixin) {
    $refs!: {
      suorituspaiva: ElsaFormDatepicker
    }

    validations() {
      return {
        form: {
          tyoskentelyjakso: {
            required
          },
          suorite: {
            required: requiredIf(() => {
              return this.value.id
            })
          }
        },
        uudetSuoritteet: {
          $each: {
            suorite: {
              required: requiredIf(() => {
                return !this.value.id
              })
            }
          }
        }
      }
    }

    @Prop({ required: false, default: () => [] })
    suoritteenKategoriat!: SuoritteenKategoria[]

    @Prop({ required: true })
    arviointiasteikko!: Arviointiasteikko

    @Prop({ required: false })
    arviointiasteikonTaso?: ArviointiasteikonTaso

    @Prop({ required: false, default: () => [] })
    kunnat!: Kunta[]

    @Prop({ required: false, default: () => [] })
    erikoisalat!: Erikoisala[]

    @Prop({
      required: false,
      default: () => ({
        tyoskentelyjakso: null,
        suorite: null,
        vaativuustaso: null,
        arviointiasteikonTaso: null,
        suorituspaiva: null,
        lisatiedot: null
      })
    })
    value!: Suoritemerkinta

    form: Partial<Suoritemerkinta> = {}
    vaativuustasot = vaativuustasot
    params = {
      saving: false,
      deleting: false
    }
    childDataReceived = false
    uudetSuoritteet: Partial<SuoritemerkinnanSuorite>[] = [{}]

    mounted() {
      const currentTyoskentelyjaksot = this.tyoskentelyjaksotFormatted.filter((jakso) =>
        dateBetween(new Date(), jakso.alkamispaiva, jakso.paattymispaiva)
      )
      this.form = {
        tyoskentelyjakso: this.value.tyoskentelyjakso
          ? this.value.tyoskentelyjakso
          : currentTyoskentelyjaksot.length === 1
          ? currentTyoskentelyjaksot[0]
          : undefined,
        suorite: this.value.suorite,
        vaativuustaso: vaativuustasot.find((taso) => taso.arvo === this.value.vaativuustaso),
        arviointiasteikonTaso: this.arviointiasteikonTaso,
        suorituspaiva: this.value.suorituspaiva,
        lisatiedot: this.value.lisatiedot
      }
      this.childDataReceived = true
    }

    validateForm(): boolean {
      this.$v.form.$touch()
      this.$v.uudetSuoritteet.$touch()
      return !this.$v.$anyError
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    validateSuoriteState(index: number) {
      if (!this.$v.uudetSuoritteet?.$each) return
      const { $dirty, $error } = this.$v.uudetSuoritteet?.$each[index]?.suorite as Validation
      return $dirty ? ($error ? false : null) : null
    }

    get arviointiAsteikonNimi() {
      return this.arviointiasteikko?.nimi === ArviointiasteikkoTyyppi.EPA
        ? this.$t('luottamuksen-taso')
        : this.$t('etappi')
    }

    onSubmit() {
      const validations = [this.validateForm(), this.$refs.suorituspaiva.validateForm()]
      if (validations.includes(false)) {
        return
      }

      this.$emit(
        'submit',
        {
          tyoskentelyjaksoId: this.form?.tyoskentelyjakso?.id,
          suoriteId: this.form?.suorite?.id,
          vaativuustaso: (this.form?.vaativuustaso as Vaativuustaso)?.arvo,
          arviointiasteikonTaso: (this.form?.arviointiasteikonTaso as ArviointiasteikonTaso)?.taso,
          suorituspaiva: this.form?.suorituspaiva,
          lisatiedot: this.form?.lisatiedot
        },
        this.params,
        this.uudetSuoritteet
      )
    }

    onSuoritemerkintaDelete() {
      this.$emit('delete', this.params)
    }

    arviointiasteikonTasoLabel(value: ArviointiasteikonTaso) {
      return `${value.taso} ${value.nimi}`
    }

    vaativuustasoLabel(value: Vaativuustaso) {
      return `${value.arvo} ${value.nimi}`
    }

    onLisaaSuorite() {
      this.uudetSuoritteet.push({})
    }

    deleteSuorite(index: number) {
      this.uudetSuoritteet.splice(index, 1)
    }
  }
</script>

<style scoped>
  .lisaa-suorite:before {
    content: '+';
    margin-right: 6px;
  }
</style>
