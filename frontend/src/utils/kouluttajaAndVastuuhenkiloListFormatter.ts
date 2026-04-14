import Vue from 'vue'

import { Kayttaja } from '@/types'

export function formatList(vue: Vue, kouluttajatAndVastuuhenkilot?: Kayttaja[]): Kayttaja[] {
  return kouluttajatAndVastuuhenkilot
    ? kouluttajatAndVastuuhenkilot?.map((kayttaja: Kayttaja) => {
        if (kayttaja.sahkoposti) {
          const formattedKayttaja = Object.assign({}, kayttaja)
          formattedKayttaja.nimi = `${kayttaja.nimi} (${kayttaja.sahkoposti})`
          return formattedKayttaja
        } else {
          return kayttaja
        }
      })
    : []
}
