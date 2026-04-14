import { Vue } from 'vue-property-decorator'

import { KaytannonKoulutusTyyppi, TyoskentelyjaksoTyyppi } from '@/utils/constants'

export function ajankohtaLabel(vue: Vue, value: any) {
  return `${(vue as any).$date(value.alkamispaiva)}-${
    value.paattymispaiva
      ? (vue as any).$date(value.paattymispaiva)
      : (vue.$t('kesken') as string).toLowerCase()
  }`
}

export function tyoskentelyjaksoLabel(vue: Vue, value: any) {
  return `${value.tyoskentelypaikka.nimi} (${ajankohtaLabel(vue, value)})`
}

export function tyoskentelypaikkaTyyppiLabel(vue: Vue, value: TyoskentelyjaksoTyyppi) {
  switch (value) {
    case TyoskentelyjaksoTyyppi.KESKUSSAIRAALA:
      return vue.$t('keskussairaala')
    case TyoskentelyjaksoTyyppi.YLIOPISTOLLINEN_SAIRAALA:
      return vue.$t('yliopistollinen-sairaala')
    case TyoskentelyjaksoTyyppi.YKSITYINEN:
      return vue.$t('yksityinen')
    case TyoskentelyjaksoTyyppi.TERVEYSKESKUS:
      return vue.$t('terveyskeskus')
    case TyoskentelyjaksoTyyppi.MUU:
      return vue.$t('muu')
  }
}

export function tyoskentelyjaksoKaytannonKoulutusLabel(vue: Vue, value: KaytannonKoulutusTyyppi) {
  switch (value) {
    case KaytannonKoulutusTyyppi.OMAA_ERIKOISALAA_TUKEVA_KOULUTUS:
      return vue.$t('omaa-erikoisalaa-tukeva-tai-taydentava-koulutus')
    case KaytannonKoulutusTyyppi.OMAN_ERIKOISALAN_KOULUTUS:
      return vue.$t('oman-erikoisalan-koulutus')
    case KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO:
      return vue.$t('pakollinen-terveyskeskuskoulutusjakso')
    case KaytannonKoulutusTyyppi.TUTKIMUSTYO:
      return vue.$t('tutkimustyo')
  }
}
