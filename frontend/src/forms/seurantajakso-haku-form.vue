<template>
  <b-form @submit.stop.prevent="onSubmit">
    <elsa-form-group
      :label="$t('koulutusjakso')"
      :add-new-enabled="true"
      :add-new-label="$t('lisaa-koulutusjakso')"
      @submit="onKoulutusjaksoSubmit"
    >
      <template #modal-content="{ submit, cancel }">
        <koulutusjakso-form
          :tyoskentelyjaksot="tyoskentelyjaksot"
          :kunnat="kunnat"
          :arvioitavan-kokonaisuuden-kategoriat="arvioitavanKokonaisuudenKategoriat"
          @submit="submit"
          @cancel="cancel"
        />
      </template>
      <template #default="{ uid }">
        <div v-for="(_, index) in form.koulutusjaksot" :id="uid" :key="index" class="mb-1">
          <elsa-form-multiselect
            :id="uid"
            v-model="form.koulutusjaksot[index]"
            :value="form.koulutusjaksot"
            :options="koulutusjaksotFiltered"
            :custom-label="koulutusjaksoLabel"
            label="label"
            track-by="id"
          />
          <elsa-button
            v-if="index !== 0"
            variant="link"
            size="sm"
            class="text-decoration-none shadow-none p-0"
            @click="deleteKoulutusjakso(index)"
          >
            <font-awesome-icon :icon="['far', 'trash-alt']" fixed-width size="sm" />
            {{ $t('poista-koulutusjakso') }}
          </elsa-button>
        </div>
        <small class="form-text text-muted">
          {{ $t('seurantajakso-koulutusjakso-help') }}
        </small>
        <elsa-button
          variant="link"
          size="sm"
          class="text-decoration-none shadow-none p-0"
          @click="addKoulutusjakso"
        >
          <font-awesome-icon icon="plus" fixed-width size="sm" class="text-lowercase" />
          {{ $t('useampi-koulutusjakso') }}
        </elsa-button>
        <b-form-invalid-feedback :id="`${uid}-feedback`">
          {{ $t('pakollinen-tieto') }}
        </b-form-invalid-feedback>
      </template>
    </elsa-form-group>
    <b-form-row>
      <elsa-form-group
        :label="$t('seurantajakso-alkaa')"
        class="col-xs-12 col-sm-6 pr-sm-3 mb-0"
        :required="true"
      >
        <template #default="{ uid }">
          <elsa-form-datepicker
            v-if="childDataReceived"
            :id="uid"
            ref="alkamispaiva"
            :value.sync="form.alkamispaiva"
            :max="maxAlkamispaiva"
            :max-error-text="$t('alkamispaiva-ei-voi-olla-paattymispaivan-jalkeen')"
          ></elsa-form-datepicker>
        </template>
      </elsa-form-group>
      <elsa-form-group
        :label="$t('seurantajakso-paattyy')"
        class="col-xs-12 col-sm-6 pl-sm-3 mb-0"
        :required="true"
      >
        <template #default="{ uid }">
          <elsa-form-datepicker
            v-if="childDataReceived"
            :id="uid"
            ref="paattymispaiva"
            :value.sync="form.paattymispaiva"
            :min="minPaattymispaiva"
            :min-error-text="$t('paattymispaiva-ei-voi-olla-ennen-alkamispaivaa')"
            :aria-describedby="`${uid}-help`"
            class="datepicker-range"
          />
        </template>
      </elsa-form-group>
      <small class="form-text text-muted pl-1">
        {{ $t('seurantajakso-aikajakso-help') }}
      </small>
    </b-form-row>
    <div class="d-flex flex-row-reverse flex-wrap mt-4">
      <elsa-button :loading="params.saving" type="submit" variant="primary" class="ml-2 mb-2">
        {{ $t('hae-tiedot') }}
      </elsa-button>
      <elsa-button variant="back" class="mb-2" @click.stop.prevent="onCancel">
        {{ $t('peruuta') }}
      </elsa-button>
    </div>
  </b-form>
</template>

<script lang="ts">
  import { BModal } from 'bootstrap-vue'
  import Component from 'vue-class-component'
  import { Mixins, Prop, Vue } from 'vue-property-decorator'

  import { postKoulutusjakso } from '@/api/erikoistuva'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormDatepicker from '@/components/datepicker/datepicker.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import KoulutusjaksoForm from '@/forms/koulutusjakso-form.vue'
  import TyoskentelyjaksoMixin from '@/mixins/tyoskentelyjakso'
  import {
    ArvioitavanKokonaisuudenKategoria,
    Erikoisala,
    Koulutusjakso,
    Kunta,
    Seurantajakso,
    SeurantajaksoHakuForm
  } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      ElsaFormDatepicker,
      ElsaFormGroup,
      ElsaFormMultiselect,
      KoulutusjaksoForm
    }
  })
  export default class SeurantajaksoHakuFormClass extends Mixins(TyoskentelyjaksoMixin) {
    $refs!: {
      alkamispaiva: ElsaFormDatepicker
      paattymispaiva: ElsaFormDatepicker
    }

    @Prop({ required: false, default: () => [] })
    arvioitavanKokonaisuudenKategoriat!: ArvioitavanKokonaisuudenKategoria[]

    @Prop({ required: false, default: () => [] })
    kunnat!: Kunta[]

    @Prop({ required: false, default: () => [] })
    erikoisalat!: Erikoisala[]

    @Prop({ required: false, default: () => [] })
    koulutusjaksot!: Koulutusjakso[]

    @Prop({
      required: false,
      default: () => null
    })
    seurantajakso!: Seurantajakso | null

    form: SeurantajaksoHakuForm = {
      alkamispaiva: null,
      paattymispaiva: null,
      koulutusjaksot: [{}]
    }

    params = {
      saving: false
    }
    childDataReceived = false

    mounted() {
      if (this.seurantajakso) {
        this.form = {
          ...this.seurantajakso,
          koulutusjaksot:
            this.seurantajakso.koulutusjaksot.length > 0 ? this.seurantajakso.koulutusjaksot : [{}]
        }
      }
      this.childDataReceived = true
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    get maxAlkamispaiva() {
      return this.form.paattymispaiva
    }

    get minPaattymispaiva() {
      return this.form.alkamispaiva
    }

    get koulutusjaksotFiltered() {
      return this.koulutusjaksot?.filter(
        (koulutusjakso) => !this.form.koulutusjaksot?.find((t) => t?.id === koulutusjakso.id)
      )
    }

    koulutusjaksoLabel(koulutusjakso: Koulutusjakso): string {
      return koulutusjakso.nimi || ''
    }

    addKoulutusjakso() {
      this.form.koulutusjaksot?.push({})
    }

    deleteKoulutusjakso(index: number) {
      if (this.form.koulutusjaksot) {
        Vue.delete(this.form.koulutusjaksot, index)
      }
    }

    async onKoulutusjaksoSubmit(data: Koulutusjakso, params: { saving: boolean }, modal: BModal) {
      params.saving = true
      try {
        const koulutusjakso = (await postKoulutusjakso(data)).data
        this.koulutusjaksot.push(koulutusjakso)
        this.form.koulutusjaksot[this.form.koulutusjaksot.length - 1] = koulutusjakso
        modal.hide('confirm')
        toastSuccess(this, this.$t('koulutusjakso-lisatty'))
      } catch (err) {
        toastFail(this, this.$t('uuden-koulutusjakson-lisaaminen-epaonnistui'))
      }
      params.saving = false
    }

    onSubmit() {
      const validations = [
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
          koulutusjaksot: this.form.koulutusjaksot?.filter((koulutusjakso) => koulutusjakso?.id)
        },
        this.params
      )
    }

    onCancel() {
      this.$emit('cancel')
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
