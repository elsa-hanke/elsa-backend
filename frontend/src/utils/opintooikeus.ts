import { ErikoistuvaLaakari, Opintooikeus } from '@/types'

export function resolveOpintooikeusKaytossa(
  erikoistuvaLaakari: ErikoistuvaLaakari
): Opintooikeus | undefined {
  return erikoistuvaLaakari.opintooikeudet.find(
    (o) => o.id === erikoistuvaLaakari.opintooikeusKaytossaId
  )
}
