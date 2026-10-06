import Vue from 'vue'
import VueI18n from 'vue-i18n'

import { formatSaveError } from '@/utils/errorMessage'
import { UNSUPPORTED_PDF_CHARACTERS_ERROR } from '@/utils/pdfTextError'

const fi = jest.requireActual('@/locales/fi.json')

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

describe('formatSaveError PDF character details', () => {
  Vue.use(VueI18n)
  const localizedVm = new Vue({
    i18n: new VueI18n({ locale: 'fi', messages: { fi } })
  })

  it.each([
    ['koulutuksen-nimi', 'Koulutuksen nimi'],
    ['sanallinen-itsearviointi', 'Sanallinen itsearviointi'],
    ['sanallinen-kokonaisarviointi', 'Sanallinen kokonaisarviointi'],
    ['koejakson-suorituspaikka', 'Koejakson suorituspaikka'],
    ['vahvuudet', 'Vahvuudet'],
    ['selvitys-jatkotoimista', 'Selvitys jatkotoimista']
  ])('interpolates field %s and the actual unsupported character', (field, label) => {
    const error = {
      response: {
        data: {
          errorKey: 'dataillegal',
          message: UNSUPPORTED_PDF_CHARACTERS_ERROR,
          field,
          unsupportedCharacters: ['💗 (U+1F497)']
        }
      }
    }

    const message = formatSaveError(localizedVm, error, fallback)
    expect(message).toContain(fallback)
    expect(message).toContain(`Kenttä "${label}"`)
    expect(message).toContain('💗 (U+1F497)')
    expect(message).not.toContain('{field}')
    expect(message).not.toContain('{unsupportedCharacters}')
  })

  it('uses the assessment question title for an answer field', () => {
    const error = {
      response: {
        data: {
          errorKey: 'dataillegal',
          message: UNSUPPORTED_PDF_CHARACTERS_ERROR,
          field: 'Erikoistujan nimi',
          unsupportedCharacters: ['💗 (U+1F497)']
        }
      }
    }
    expect(formatSaveError(localizedVm, error, fallback)).toContain('Kenttä "Erikoistujan nimi"')
  })
})
