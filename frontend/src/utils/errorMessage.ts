import { AxiosError } from 'axios'
import Vue from 'vue'

import { ElsaError } from '@/types'
import { formatPdfTextError } from '@/utils/pdfTextError'

/**
 * Adds a localized backend validation reason to a view-specific fallback message.
 * Network errors and responses without a message retain the existing fallback.
 */
export function formatSaveError(vm: Vue, error: unknown, fallback: unknown): string {
  const axiosError = error as AxiosError<ElsaError>
  const responseError = axiosError.response?.data
  const pdfTextError = formatPdfTextError(vm, responseError)
  if (pdfTextError) {
    return `${fallback}: ${pdfTextError}`
  }
  const message = responseError?.message

  return message ? `${fallback}: ${vm.$t(message)}` : `${fallback}`
}
