<template>
  <div>
    <b-input-group class="date-input-group">
      <b-form-input
        :id="`date-input-${id}`"
        v-model="form.dateStr"
        class="date-input"
        :state="validateState('dateStr')"
        maxlength="10"
        v-bind="$attrs"
        type="text"
        @blur="onDateStrInput"
      ></b-form-input>
      <b-input-group-append>
        <b-form-datepicker
          :id="`datepicker-${id}`"
          v-model="form.selectedDate"
          v-bind="$attrs"
          start-weekday="1"
          :locale="currentLocale"
          :min="datepickerMinDate"
          :max="datepickerMaxDate"
          :date-format-options="{
            year: 'numeric',
            month: 'numeric',
            day: 'numeric'
          }"
          :label-no-date-selected="$t('datepicker-no-date-selected')"
          right
          @input="onDateInput"
          v-on="$listeners"
        >
          <template #button-content>
            <font-awesome-icon :icon="['far', 'calendar-alt']" class="text-primary" />
          </template>
        </b-form-datepicker>
      </b-input-group-append>
      <b-form-invalid-feedback
        v-if="$v.form.dateStr && !$v.form.dateStr.required && !form.dateStr"
        :id="`feedback-required-${id}`"
      >
        {{ $t('pakollinen-tieto') }}
      </b-form-invalid-feedback>
      <b-form-invalid-feedback
        v-else-if="$v.form.dateStr && !$v.form.dateStr.isValidLocalDate"
        :id="`feedback-invalid-date-${id}`"
      >
        {{ $t('anna-paivamaara-muodossa') }}
      </b-form-invalid-feedback>
      <b-form-invalid-feedback
        v-else-if="$v.form.dateStr && !$v.form.dateStr.minValue"
        :id="`feedback-min-date-${id}`"
      >
        {{ formattedMinErrorText }}
      </b-form-invalid-feedback>
      <b-form-invalid-feedback
        v-else-if="$v.form.dateStr && !$v.form.dateStr.maxValue"
        :id="`feedback-max-date-${id}`"
      >
        {{ formattedMaxErrorText }}
      </b-form-invalid-feedback>
    </b-input-group>
  </div>
</template>

<script lang="ts">
  import { parse, format, isValid, addYears, subYears } from 'date-fns'
  import { fi, sv, enUS } from 'date-fns/locale'
  import { Component, Prop, Mixins } from 'vue-property-decorator'
  import { validationMixin } from 'vuelidate'
  import { requiredIf } from 'vuelidate/lib/validators'

  const defaultDateFormat = 'yyyy-MM-dd'
  const defaultMinDate = format(subYears(new Date(), 40), defaultDateFormat)
  const defaultMaxDate = format(addYears(new Date(), 30), defaultDateFormat)

  interface DateForm {
    dateStr?: string
    selectedDate?: string | Date | null
  }

  @Component({
    inheritAttrs: false
  })
  export default class ElsaFormDatepicker extends Mixins(validationMixin) {
    validations() {
      return {
        form: {
          dateStr: {
            required: requiredIf(() => {
              return this.required
            }),
            isValidLocalDate: (value: string) => this.isValidLocalDate(value),
            minValue: (value: string) =>
              this.$v.form.dateStr?.isValidLocalDate && this.isValidMinDate(value),
            maxValue: (value: string) =>
              this.$v.form.dateStr?.isValidLocalDate && this.isValidMaxDate(value)
          }
        }
      }
    }
    @Prop({ required: true, type: String })
    id!: string

    @Prop({ required: true })
    value?: string | Date | null

    @Prop({
      required: false,
      type: [String, Date],
      default: () => defaultMinDate
    })
    min?: string | Date | null

    @Prop({ required: false, type: String })
    minErrorText?: string

    @Prop({
      required: false,
      type: [String, Date],
      default: () => defaultMaxDate
    })
    max?: string | Date | null

    @Prop({ required: false, type: String })
    maxErrorText?: string

    @Prop({ required: false, type: Boolean, default: true })
    required!: boolean

    form = {
      dateStr: '',
      selectedDate: null
    } as DateForm
    allowedDateFormats: string[] = []

    mounted() {
      this.form.selectedDate = this.value
      const isDateObj = this.value instanceof Date
      const dateStr = isDateObj ? format(this.value as Date, defaultDateFormat) : this.value
      this.form.dateStr = dateStr ? this.$date(dateStr as string) : ''

      document.querySelector(`button#datepicker-${this.id}`)?.setAttribute('tabindex', '-1')
    }

    isValidMinDate(value: string) {
      if (!value) {
        return true
      }
      const parsedDate = this.parseDate(value)
      const formattedDate = format(parsedDate, defaultDateFormat)
      const isValidMinDate = formattedDate >= this.datepickerMinDate
      if (!isValidMinDate) {
        this.form.selectedDate = null
      }
      return isValidMinDate
    }

    isValidMaxDate(value: string) {
      if (!value) {
        return true
      }
      const parsedDate = this.parseDate(value)
      const formattedDate = format(parsedDate, defaultDateFormat)
      const isValidMaxDate = formattedDate <= this.datepickerMaxDate
      if (!isValidMaxDate) {
        this.form.selectedDate = null
      }
      return isValidMaxDate
    }

    getLocaleObj() {
      switch (this.currentLocale) {
        case 'fi':
          return fi
        case 'sv':
          return sv
        case 'en':
          return enUS
      }
    }

    isValidLocalDate(value: string) {
      if (!value) {
        return true
      }
      const parsedDate = this.parseDate(value)
      return isValid(parsedDate)
    }

    onDateStrInput(event: FocusEvent) {
      let dateStr = (event.target as HTMLInputElement).value
      dateStr = this.parsePartialYear(dateStr)
      const isDate = dateStr && this.isValidLocalDate(dateStr)
      const parsedDate = isDate ? this.parseDate(dateStr) : null
      const formattedDate = parsedDate ? format(parsedDate, defaultDateFormat) : null
      const isValid = isDate && this.isValidMinDate(dateStr) && this.isValidMaxDate(dateStr)
      this.form.selectedDate = isValid ? formattedDate : null

      this.$emit('update:value', formattedDate)
      this.$emit('input', formattedDate)
    }

    parsePartialYear(dateStr: string): string {
      const dateParts = dateStr.split('.')
      if (dateParts.length === 3) {
        const day = dateParts[0]
        const month = dateParts[1]
        let year = dateParts[2]
        if (year.length === 2) {
          year = '20' + year
        }
        const formatted = `${day}.${month}.${year}`
        this.form.dateStr = formatted
        return formatted
      }
      return dateStr
    }

    parseDate(value: string): Date {
      return parse(value, 'P', new Date(), { locale: this.getLocaleObj() })
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    onDateInput(date: string) {
      this.form.dateStr = this.$date(date)
      this.$emit('update:value', date)
    }

    validateForm(): boolean {
      this.$v.form.$touch()
      return !this.$v.$anyError
    }

    resetValue() {
      this.form = {
        dateStr: '',
        selectedDate: null
      }
    }

    get formattedMinDate(): string {
      return this.min instanceof Date
        ? format(this.min, defaultDateFormat)
        : this.min ?? defaultMinDate
    }

    get formattedMaxDate(): string {
      return this.max instanceof Date
        ? format(this.max, defaultDateFormat)
        : this.max ?? defaultMaxDate
    }

    get formattedMinErrorText() {
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      const parsedDate = this.parseDate(this.form.dateStr!)
      const defaultErrorText = this.$t('paivamaara-ei-voi-olla-ennen')

      if (!isValid(parsedDate)) {
        return `${defaultErrorText} ${this.$date(defaultMinDate)}`
      }

      const formattedDate = format(parsedDate, defaultDateFormat)
      if (formattedDate < defaultMinDate) {
        return `${defaultErrorText} ${this.$date(defaultMinDate)}`
      }

      return this.minErrorText
        ? this.minErrorText
        : `${defaultErrorText} ${this.$date(this.formattedMinDate)}`
    }

    get formattedMaxErrorText() {
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      const parsedDate = this.parseDate(this.form.dateStr!)
      const defaultErrorText = this.$t('paivamaara-ei-voi-olla-jalkeen')

      if (!isValid(parsedDate)) {
        return `${defaultErrorText} ${this.$date(defaultMaxDate)}`
      }

      const formattedDate = format(parsedDate, defaultDateFormat)
      if (formattedDate > defaultMaxDate) {
        return `${defaultErrorText} ${this.$date(defaultMaxDate)}`
      }

      return this.maxErrorText
        ? this.maxErrorText
        : `${defaultErrorText} ${this.$date(this.formattedMaxDate)}`
    }

    get currentLocale() {
      if (this.$i18n) {
        return this.$i18n.locale
      } else {
        return 'fi'
      }
    }

    get datepickerMinDate() {
      const formattedMinDate = this.formattedMinDate
      return formattedMinDate >= defaultMinDate && formattedMinDate <= defaultMaxDate
        ? formattedMinDate
        : defaultMinDate
    }

    get datepickerMaxDate() {
      const formattedMaxDate = this.formattedMaxDate
      return formattedMaxDate <= defaultMaxDate && this.formattedMaxDate >= defaultMinDate
        ? formattedMaxDate
        : defaultMaxDate
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';

  .date-input {
    border-radius: $border-radius !important;
  }

  ::v-deep {
    label {
      display: none;
    }

    .b-form-btn-label-control {
      all: unset;
    }

    .show > .btn-secondary.dropdown-toggle {
      background-color: unset;
      border: none;
    }

    .b-form-btn-label-control.form-control > .btn {
      line-height: unset;
    }

    .b-calendar-grid {
      height: 17rem !important;
      border: none;
    }

    .b-calendar-grid-help {
      display: none;
    }
  }
</style>
