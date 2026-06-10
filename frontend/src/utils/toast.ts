import { ComponentPublicInstance } from 'vue'

// Global toast state for bootstrap-vue-next programmatic toasts
let _toastHandler: ((message: string, variant: string) => void) | null = null

export function registerToastHandler(handler: (message: string, variant: string) => void) {
  _toastHandler = handler
}

export function toastSuccess(vue: ComponentPublicInstance | any, message: any) {
  if (_toastHandler) {
    _toastHandler(String(message), 'success')
  } else {
    console.info('[Toast Success]', message)
  }
}

export function toastFail(vue: ComponentPublicInstance | any, message: any) {
  if (_toastHandler) {
    _toastHandler(String(message), 'danger')
  } else {
    console.error('[Toast Fail]', message)
  }
}
