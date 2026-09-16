import Vue from 'vue'

import { UNSUPPORTED_PDF_CHARACTERS_ERROR, formatPdfTextSaveError } from '@/utils/pdfTextError'

const fallback = 'Työskentelyjakson tallentaminen epäonnistui'
const invalidFileError =
  'error.dataillegal.tiedosto-ei-ole-kelvollinen-tai-samanniminen-tiedosto-on-jo-olemassa'

const vm = {
  $t: (key: string, params?: Record<string, unknown>) => {
    if (key === invalidFileError) {
      return 'tiedosto ei ole kelvollinen tai samanniminen tiedosto on jo olemassa'
    }
    if (key === UNSUPPORTED_PDF_CHARACTERS_ERROR) {
      return `Kenttä "${params?.field}" sisältää merkit ${params?.unsupportedCharacters}`
    }
    return key
  },
  $date: (value: string) => value
} as unknown as Vue

describe('formatPdfTextSaveError', () => {
  it('säilyttää muun backend-virheen tarkemman viestin', () => {
    const error = {
      response: {
        data: {
          errorKey: 'dataillegal',
          message: invalidFileError
        }
      }
    }

    expect(formatPdfTextSaveError(vm, error, fallback)).toBe(
      `${fallback}: tiedosto ei ole kelvollinen tai samanniminen tiedosto on jo olemassa`
    )
  })

  it('näyttää PDF-merkkivirheen yksityiskohtineen', () => {
    const error = {
      response: {
        data: {
          errorKey: 'dataillegal',
          message: UNSUPPORTED_PDF_CHARACTERS_ERROR,
          field: 'tyoskentelypaikka',
          unsupportedCharacters: ['✓']
        }
      }
    }

    expect(formatPdfTextSaveError(vm, error, fallback)).toBe(
      'Kenttä "tyoskentelypaikka" sisältää merkit ✓'
    )
  })

  it('käyttää yleistä viestiä, kun backend ei palauta tarkempaa virhettä', () => {
    expect(formatPdfTextSaveError(vm, new Error('network error'), fallback)).toBe(fallback)
  })
})
