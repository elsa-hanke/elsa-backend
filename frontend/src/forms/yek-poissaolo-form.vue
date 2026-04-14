<template>
  <b-form @submit.stop.prevent="onSubmit">
    <elsa-form-group :label="$t('poissaolon-syy')" :required="true">
      <template #label-help>
        <elsa-popover :title="$t('poissaolon-syy')">
          <elsa-poissaolon-syyt />
        </elsa-popover>
      </template>
      <template #default="{ uid }">
        <elsa-form-multiselect
          :id="uid"
          v-model="form.poissaolonSyy"
          :options="poissaolonSyytSorted"
          :state="validateState('poissaolonSyy')"
          label="nimi"
          track-by="id"
          @input="$emit('skipRouteExitConfirm', false)"
        />
        <b-form-invalid-feedback :id="`${uid}-feedback`">
          {{ $t('pakollinen-tieto') }}
        </b-form-invalid-feedback>
      </template>
    </elsa-form-group>
    <elsa-form-group
      :label="$t('tyoskentelyjakso')"
      :required="true"
      @submit="onTyoskentelyjaksoSubmit"
    >
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
    <b-form-row>
      <elsa-form-group
        :label="$t('alkamispaiva')"
        class="col-xs-12 col-sm-6 pr-sm-3"
        :required="true"
      >
        <template #default="{ uid }">
          <elsa-form-datepicker
            v-if="childDataReceived"
            :id="uid"
            ref="alkamispaiva"
            :value.sync="form.alkamispaiva"
            :initial-date="alkamispaivaInitialDate"
            :min="minAlkamispaiva"
            :min-error-text="
              $t('poissaolon-alkamispaiva-ei-voi-olla-ennen-tyoskentelyjakson-alkamista')
            "
            :max="maxAlkamispaiva"
            :max-error-text="$t('poissaolon-taytyy-alkaa-ennen-paattymispaivaa')"
            @input="$emit('skipRouteExitConfirm', false)"
          ></elsa-form-datepicker>
        </template>
      </elsa-form-group>
      <elsa-form-group
        :label="$t('paattymispaiva')"
        class="col-xs-12 col-sm-6 pl-sm-3"
        :required="true"
      >
        <template #default="{ uid }">
          <elsa-form-datepicker
            v-if="childDataReceived"
            :id="uid"
            ref="paattymispaiva"
            :value.sync="form.paattymispaiva"
            :initial-date="paattymispaivaInitialDate"
            :min="minPaattymispaiva"
            :min-error-text="$t('poissaolon-paattymispaivan-taytyy-olla-alkamispaivan-jalkeen')"
            :max="maxPaattymispaiva"
            :max-error-text="
              $t('poissaolon-paattymispaivan-taytyy-olla-ennen-tyoskentelyjakson-paattymista')
            "
            class="datepicker-range"
            @input="$emit('skipRouteExitConfirm', false)"
          ></elsa-form-datepicker>
          <b-form-invalid-feedback :id="`${uid}-feedback`">
            {{ $t('pakollinen-tieto') }}
          </b-form-invalid-feedback>
        </template>
      </elsa-form-group>
    </b-form-row>
    <b-form-checkbox
      v-model="form.kokoTyoajanPoissaolo"
      :class="{ 'mb-3': !form.kokoTyoajanPoissaolo }"
      @change="$emit('skipRouteExitConfirm', false)"
    >
      {{ $t('koko-tyoajan-poissaolo') }}
    </b-form-checkbox>
    <b-form-row>
      <elsa-form-group
        v-if="!form.kokoTyoajanPoissaolo"
        :label="`${$t('poissaolo-nykyisesta-taydesta-tyoajasta')} (0-100 %)`"
        :required="true"
        class="col-sm-5"
      >
        <template #label-help>
          <elsa-popover>
            {{ $t('poissaoloprosentti-tooltip') }}
          </elsa-popover>
        </template>
        <template #default="{ uid }">
          <div class="d-flex align-items-center">
            <b-form-input
              :id="uid"
              v-model.number="form.poissaoloprosentti"
              :state="validateState('poissaoloprosentti')"
              type="number"
              step="any"
              @input="$emit('skipRouteExitConfirm', false)"
            />
            <span class="mx-3">%</span>
          </div>
          <b-form-invalid-feedback
            :id="`${uid}-feedback`"
            :style="{
              display: validateState('poissaoloprosentti') === false ? 'block' : 'none'
            }"
          >
            {{ `${$t('poissaoloprosentti-validointivirhe')} 0-100 %` }}
          </b-form-invalid-feedback>
        </template>
      </elsa-form-group>
    </b-form-row>
    <hr />
    <div class="text-right">
      <elsa-button variant="back" :to="backButtonRoute">
        {{ $t('peruuta') }}
      </elsa-button>
      <elsa-button
        v-if="poissaolo.id && !account.impersonated"
        :loading="params.deleting"
        variant="outline-danger"
        @click="onDelete"
      >
        {{ $t('poista-poissaolo') }}
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
  import { validationMixin } from 'vuelidate'
  import { required, requiredIf, integer, between } from 'vuelidate/lib/validators'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import ElsaPoissaolonSyyt from '@/components/poissaolon-syyt/poissaolon-syyt.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import TyoskentelyjaksoForm from '@/forms/tyoskentelyjakso-form.vue'
  import TyoskentelyjaksoMixin from '@/mixins/tyoskentelyjakso'
  import store from '@/store'
  import { Poissaolo, PoissaolonSyy, Tyoskentelyjakso } from '@/types'
  import { dateBetween } from '@/utils/date'
  import { sortByAsc } from '@/utils/sort'
  import { tyoskentelyjaksoLabel } from '@/utils/tyoskentelyjakso'

  @Component({
    components: {
      TyoskentelyjaksoForm,
      ElsaFormGroup,
      ElsaFormError,
      ElsaFormMultiselect,
      ElsaPopover,
      ElsaFormDatepicker,
      ElsaButton,
      ElsaPoissaolonSyyt
    },
    validations: {
      form: {
        poissaolonSyy: {
          required
        },
        tyoskentelyjakso: {
          required
        },
        kokoTyoajanPoissaolo: {
          required
        },
        poissaoloprosentti: {
          required: requiredIf((poissaolo) => {
            return poissaolo.kokoTyoajanPoissaolo === false
          }),
          between: between(0, 100),
          integer
        }
      }
    }
  })
  export default class YekPoissaoloForm extends Mixins<TyoskentelyjaksoMixin>(
    validationMixin,
    TyoskentelyjaksoMixin
  ) {
    $refs!: {
      alkamispaiva: ElsaFormDatepicker
      paattymispaiva: ElsaFormDatepicker
    }

    @Prop({ required: true, default: () => [] })
    poissaolonSyyt!: PoissaolonSyy[]

    @Prop({ required: false, type: Number })
    tyoskentelyjaksoId?: number

    @Prop({
      required: false,
      default: () => ({
        poissaolonSyy: null,
        tyoskentelyjakso: null,
        alkamispaiva: null,
        paattymispaiva: null,
        kokoTyoajanPoissaolo: true,
        poissaoloprosentti: null
      })
    })
    poissaolo!: Poissaolo

    declare form: Poissaolo
    params = {
      saving: false,
      deleting: false
    }
    childDataReceived = false

    mounted() {
      this.form = this.poissaolo

      const selectedTyoskentelyjakso =
        this.tyoskentelyjaksoId &&
        this.tyoskentelyjaksotFormatted.find((t) => t.id === this.tyoskentelyjaksoId)

      if (selectedTyoskentelyjakso) {
        this.form.tyoskentelyjakso = selectedTyoskentelyjakso
      }

      this.childDataReceived = true
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    validateForm(): boolean {
      this.$v.form.$touch()
      return !this.$v.$anyError
    }

    onSubmit() {
      const validations = [
        this.validateForm(),
        this.$refs.alkamispaiva.validateForm(),
        this.$refs.paattymispaiva.validateForm()
      ]
      if (validations.includes(false)) {
        return
      }
      this.$emit(
        'submit',
        {
          ...this.form,
          poissaolonSyyId: this.form.poissaolonSyy?.id,
          tyoskentelyjaksoId: this.form.tyoskentelyjakso?.id,
          poissaoloprosentti: this.form.kokoTyoajanPoissaolo ? 100 : this.form.poissaoloprosentti
        },
        this.params
      )
    }

    onDelete() {
      this.$emit('delete', this.params)
    }

    onTyoskentelyjaksoSelect(tyoskentelyjakso: Tyoskentelyjakso) {
      if (!tyoskentelyjakso.alkamispaiva) {
        return
      }

      if (
        this.form?.alkamispaiva &&
        !dateBetween(
          this.form.alkamispaiva,
          tyoskentelyjakso.alkamispaiva,
          tyoskentelyjakso.paattymispaiva ?? undefined
        )
      ) {
        this.form.alkamispaiva = undefined
      }
      if (
        this.form?.paattymispaiva &&
        !dateBetween(
          this.form.paattymispaiva,
          tyoskentelyjakso.alkamispaiva,
          tyoskentelyjakso.paattymispaiva ?? undefined
        )
      ) {
        this.form.paattymispaiva = undefined
      }
    }

    get minAlkamispaiva() {
      return this.form.tyoskentelyjakso?.alkamispaiva
    }

    get maxAlkamispaiva() {
      return this.form.paattymispaiva || this.form.tyoskentelyjakso?.paattymispaiva
    }

    get minPaattymispaiva() {
      return this.form.alkamispaiva || this.form.tyoskentelyjakso?.alkamispaiva
    }

    get maxPaattymispaiva() {
      return this.form.tyoskentelyjakso?.paattymispaiva
    }

    get poissaolonSyytSorted() {
      return [...this.poissaolonSyyt.sort((a, b) => sortByAsc(a.nimi, b.nimi))]
    }

    get backButtonRoute() {
      return this.tyoskentelyjaksoId
        ? { name: 'yektyoskentelyjakso', id: this.tyoskentelyjaksoId }
        : { name: 'yektyoskentelyjaksot' }
    }

    get alkamispaivaInitialDate() {
      return this.form.tyoskentelyjakso?.alkamispaiva
    }

    get paattymispaivaInitialDate() {
      return this.form.alkamispaiva
    }

    get account() {
      return store.getters['auth/account']
    }

    get tyoskentelyjaksotFormatted() {
      return this.tyoskentelyjaksot.map((tj) => ({
        ...tj,
        label: tyoskentelyjaksoLabel(this, tj)
      }))
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .datepicker-range::before {
    content: '–';
    position: absolute;
    left: -1.5rem;
    padding: 0.375rem 0.75rem;
    @include media-breakpoint-down(xs) {
      display: none;
    }
  }
</style>
