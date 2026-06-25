import { ComponentPublicInstance } from 'vue'

type VueInstance = ComponentPublicInstance | any

export async function confirmExit(vue: VueInstance) {
  if (vue?.$bvModal?.msgBoxConfirm) {
    return vue.$bvModal.msgBoxConfirm(vue.$t('tallentamattomia-tietoja-vahvistus') as string, {
      title: vue.$t('vahvista-poistuminen-tallentamatta') as string,
      okVariant: 'outline-danger',
      okTitle: vue.$t('poistu-tallentamatta') as string,
      cancelTitle: vue.$t('peruuta') as string,
      cancelVariant: 'back',
      hideHeaderClose: false,
      centered: true
    })
  }
  return window.confirm(vue.$t('tallentamattomia-tietoja-vahvistus') as string)
}

export async function confirmExitWithTexts(
  vue: VueInstance,
  title: string,
  content: string,
  okTitle: string,
  cancelTitle: string
) {
  if (vue?.$bvModal?.msgBoxConfirm) {
    return vue.$bvModal.msgBoxConfirm(content, {
      title: title,
      okVariant: 'primary',
      okTitle: okTitle,
      cancelTitle: cancelTitle,
      cancelVariant: 'back',
      hideHeaderClose: false,
      centered: false
    })
  }
  return window.confirm(content)
}

export async function confirmDelete(
  vue: VueInstance,
  title: string,
  name: string,
  additionalInfo = ''
) {
  const confirmText = additionalInfo
    ? `${vue.$t('haluatko-varmasti-poistaa', { name }) as string} ${additionalInfo}`
    : (vue.$t('haluatko-varmasti-poistaa', { name }) as string)
  if (vue?.$bvModal?.msgBoxConfirm) {
    return vue.$bvModal.msgBoxConfirm(confirmText, {
      title,
      okVariant: 'outline-danger',
      okTitle: vue.$t('poista') as string,
      cancelTitle: vue.$t('peruuta') as string,
      cancelVariant: 'back',
      hideHeaderClose: false,
      centered: true
    })
  }
  return window.confirm(confirmText)
}
