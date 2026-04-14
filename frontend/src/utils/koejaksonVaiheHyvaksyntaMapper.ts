import Vue from 'vue'

import {
  KoejaksonVaiheHyvaksynta,
  Kouluttaja,
  KoejaksonVaiheHyvaksyja,
  Vastuuhenkilo
} from '@/types'

export function mapHyvaksyntaErikoistuva(
  vue: Vue,
  erikoistuvanNimi?: string | null,
  allekirjoitusPvm?: string | null
): KoejaksonVaiheHyvaksynta | null {
  return allekirjoitusPvm
    ? {
        nimiAndNimike: `${erikoistuvanNimi}, ${(vue.$t('erikoistuja') as string).toLowerCase()}`,
        pvm: allekirjoitusPvm
      }
    : null
}

export function mapHyvaksynnatSopimuksenKouluttajat(
  kouluttajat: Kouluttaja[]
): KoejaksonVaiheHyvaksynta[] | null {
  return kouluttajat
    .filter((k) => k.sopimusHyvaksytty)
    .map((k) => ({
      nimiAndNimike: `${k.nimi}${k.nimike ? ', ' + k.nimike : ''}`,
      pvm: k.kuittausaika
    }))
}

export function mapHyvaksyntaLahikouluttaja(
  vue: Vue,
  kouluttaja?: KoejaksonVaiheHyvaksyja
): KoejaksonVaiheHyvaksynta | null {
  return kouluttaja?.sopimusHyvaksytty
    ? {
        nimiAndNimike: `${kouluttaja.nimi}, ${(vue.$t('lahikouluttaja') as string).toLowerCase()}`,
        pvm: kouluttaja.kuittausaika
      }
    : null
}

export function mapHyvaksyntaLahiesimies(
  vue: Vue,
  esimies?: KoejaksonVaiheHyvaksyja
): KoejaksonVaiheHyvaksynta | null {
  return esimies?.sopimusHyvaksytty
    ? {
        nimiAndNimike: `${esimies.nimi}, ${(vue.$t('lahiesimies') as string).toLowerCase()}`,
        pvm: esimies.kuittausaika
      }
    : null
}

export function mapHyvaksyntaVastuuhenkilo(
  vastuuhenkilo: Vastuuhenkilo | null
): KoejaksonVaiheHyvaksynta | null {
  return vastuuhenkilo && vastuuhenkilo.sopimusHyvaksytty
    ? {
        nimiAndNimike: `${vastuuhenkilo.nimi}${
          vastuuhenkilo.nimike ? ', ' + vastuuhenkilo.nimike : ''
        }`,
        pvm: vastuuhenkilo.kuittausaika
      }
    : null
}
