import { AxiosError } from 'axios'
import Vue from 'vue'

import { ElsaError } from '@/types'

export const UNSUPPORTED_PDF_CHARACTERS_ERROR =
  'error.dataillegal.pdf-tiedostossa-tukemattomia-merkkeja'

export function formatPdfTextError(
  vm: Vue,
  error?: ElsaError,
  messageKey = UNSUPPORTED_PDF_CHARACTERS_ERROR
): string | undefined {
  if (error?.message !== UNSUPPORTED_PDF_CHARACTERS_ERROR) {
    return undefined
  }

  const field = error.field ? `${vm.$t(error.field)}` : ''
  const seurantajaksoContext = error.seurantajaksoStartDate
    ? ` (${vm.$date(error.seurantajaksoStartDate)})`
    : ''

  return `${vm.$t(messageKey, {
    field,
    unsupportedCharacters: error.unsupportedCharacters?.join(', ') ?? '',
    seurantajaksoContext
  })}`
}

export function formatPdfTextSaveError(vm: Vue, error: unknown, fallback: unknown): string {
  const axiosError = error as AxiosError<ElsaError>
  return formatPdfTextError(vm, axiosError.response?.data) ?? `${fallback}`
}
