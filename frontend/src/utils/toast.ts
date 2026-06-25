import { ComponentPublicInstance } from 'vue'

// Global toast state for bootstrap-vue-next programmatic toasts
type ToastHandler = (message: string, variant: string) => void
let _toastHandler: ToastHandler | null = null

export function registerToastHandler(handler: ToastHandler) {
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
