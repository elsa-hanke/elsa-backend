import Vue from 'vue'

import { formatSaveError } from '@/utils/errorMessage'

const fallback = 'Teoriakoulutuksen muokkaus epäonnistui'
const invalidFileError =
  'error.dataillegal.tiedosto-ei-ole-kelvollinen-tai-samanniminen-tiedosto-on-jo-olemassa'
const translatedInvalidFileError =
  'Liitetiedostoa ei voitu käsitellä tai samanniminen tiedosto on jo olemassa.'

const vm = {
  $t: (key: string) => (key === invalidFileError ? translatedInvalidFileError : key)
} as unknown as Vue

describe('formatSaveError', () => {
  it('näyttää backendin palauttaman validoidun virhesyyn', () => {
    const error = {
      response: {
        data: {
          errorKey: 'dataillegal',
          message: invalidFileError
        }
      }
    }

    expect(formatSaveError(vm, error, fallback)).toBe(`${fallback}: ${translatedInvalidFileError}`)
  })

  it('käyttää yleistä viestiä verkkovirheelle', () => {
    expect(formatSaveError(vm, new Error('network error'), fallback)).toBe(fallback)
  })

  it('käyttää yleistä viestiä, jos backend ei palauta viestiavainta', () => {
    expect(formatSaveError(vm, { response: { data: {} } }, fallback)).toBe(fallback)
  })
})
