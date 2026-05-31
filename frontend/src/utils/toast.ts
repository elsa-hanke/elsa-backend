import { Vue } from 'vue-property-decorator'

export function toastSuccess(vue: Vue, message: any) {
  const h = vue.$root.$createElement
  const vNodesMsg = h('div', { class: ['d-flex', 'align-items-center'] }, [
    h('font-awesome-icon', {
      props: { icon: 'check-circle', 'fixed-width': true, size: 'lg' },
      attrs: { 'aria-hidden': 'true' },
      class: ['mr-2']
    }),
    `${message}`
  ])

  vue.$root.$bvToast.toast([vNodesMsg], {
    variant: 'success',
    solid: true,
    bodyClass: 'shadow rounded p-3 pr-4',
    // Aria attributes – polite for success notifications
    toastClass: 'accessible-toast',
    autoHideDelay: 5000,
    appendToast: true
  })
}

export function toastFail(vue: Vue, message: any) {
  const h = vue.$root.$createElement

  const vNodesMsg = h('div', { class: ['d-flex', 'align-items-center'] }, [
    h('font-awesome-icon', {
      props: { icon: 'exclamation-circle', 'fixed-width': true, size: 'lg' },
      attrs: { 'aria-hidden': 'true' },
      class: ['mr-2']
    }),
    `${message}`
  ])

  vue.$root.$bvToast.toast(vNodesMsg, {
    variant: 'danger',
    solid: true,
    bodyClass: 'shadow rounded p-3 pr-4',
    // Assertive live region for error notifications
    toastClass: 'accessible-toast',
    autoHideDelay: 7000,
    noAutoHide: false,
    appendToast: true
  })
}
