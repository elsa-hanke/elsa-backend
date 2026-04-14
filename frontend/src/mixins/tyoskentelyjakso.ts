import axios, { AxiosError } from 'axios'
import { BModal } from 'bootstrap-vue'
import { Component, Vue, Prop } from 'vue-property-decorator'

import { ElsaError, Tyoskentelyjakso } from '@/types'
import { dateBetween } from '@/utils/date'
import { toastSuccess, toastFail } from '@/utils/toast'
import { tyoskentelyjaksoLabel } from '@/utils/tyoskentelyjakso'

@Component({})
export default class TyoskentelyjaksoMixin extends Vue {
  @Prop({ required: false, default: () => [] })
  tyoskentelyjaksot!: Tyoskentelyjakso[]

  form = {
    tyoskentelyjakso: null,
    tapahtumanAjankohta: null
  } as any

  onTyoskentelyjaksoSelect(value: Tyoskentelyjakso) {
    if (!dateBetween(this.form.tapahtumanAjankohta, value.alkamispaiva, value.paattymispaiva)) {
      this.form.tapahtumanAjankohta = null
    }
  }

  async onTyoskentelyjaksoSubmit(value: any, params: any, modal: BModal) {
    params.saving = true
    try {
      const formData = new FormData()
      formData.append('tyoskentelyjaksoJson', JSON.stringify(value.tyoskentelyjakso))
      value.addedFiles.forEach((file: File) => formData.append('files', file, file.name))

      const tyoskentelyjakso = (
        await axios.post('erikoistuva-laakari/tyoskentelyjaksot', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          },
          timeout: 120000
        })
      ).data

      this.tyoskentelyjaksot.push(tyoskentelyjakso)
      tyoskentelyjakso.label = tyoskentelyjaksoLabel(this, tyoskentelyjakso)
      this.form.tyoskentelyjakso = tyoskentelyjakso

      if (this.form.tyoskentelyjaksot) {
        for (const [index, value] of this.form.tyoskentelyjaksot.entries()) {
          if (!value || !value.id) {
            this.form.tyoskentelyjaksot[index] = tyoskentelyjakso
            break
          }
        }
      }

      this.onTyoskentelyjaksoSelect(tyoskentelyjakso)
      modal.hide('confirm')
      toastSuccess(this, this.$t('uusi-tyoskentelyjakso-lisatty'))
    } catch (err) {
      const axiosError = err as AxiosError<ElsaError>
      const message = axiosError?.response?.data?.message
      toastFail(
        this,
        message
          ? `${this.$t('uuden-tyoskentelyjakson-lisaaminen-epaonnistui')}: ${this.$t(message)}`
          : this.$t('uuden-tyoskentelyjakson-lisaaminen-epaonnistui')
      )
    }
    params.saving = false
  }

  get tyoskentelyjaksotFormatted() {
    return this.tyoskentelyjaksot
      .filter((tj) => !tj.hyvaksyttyAiempaanErikoisalaan)
      .map((tj) => ({
        ...tj,
        label: tyoskentelyjaksoLabel(this, tj)
      }))
  }

  get tyoskentelyjaksonAlkamispaiva() {
    return this.form.tyoskentelyjakso?.alkamispaiva
  }

  get tyoskentelyjaksonPaattymispaiva() {
    return this.form.tyoskentelyjakso?.paattymispaiva
  }
}
