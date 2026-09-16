import { AxiosError } from 'axios'
import Vue from 'vue'

import { ElsaError } from '@/types'

/**
 * Adds a localized backend validation reason to a view-specific fallback message.
 * Network errors and responses without a message retain the existing fallback.
 */
export function formatSaveError(vm: Vue, error: unknown, fallback: unknown): string {
  const axiosError = error as AxiosError<ElsaError>
  const message = axiosError.response?.data?.message

  return message ? `${fallback}: ${vm.$t(message)}` : `${fallback}`
}
