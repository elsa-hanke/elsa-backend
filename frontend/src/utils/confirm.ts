import { Vue } from 'vue-property-decorator'

export async function confirmExit(vue: Vue) {
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

export async function confirmExitWithTexts(
  vue: Vue,
  title: string,
  content: string,
  okTitle: string,
  cancelTitle: string
) {
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

export async function confirmDelete(vue: Vue, title: string, name: string, additionalInfo = '') {
  const confirmText = additionalInfo
    ? `${vue.$t('haluatko-varmasti-poistaa', { name }) as string} ${additionalInfo}`
    : (vue.$t('haluatko-varmasti-poistaa', { name }) as string)
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
