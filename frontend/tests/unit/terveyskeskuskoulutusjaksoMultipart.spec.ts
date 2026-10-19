import { TerveyskeskuskoulutusjaksonHyvaksyntaForm } from '@/types'
import { hasTooManyTerveyskeskuskoulutusjaksoParts } from '@/utils/multipart'

const form = (
  addedCount: number,
  deletedCount: number
): TerveyskeskuskoulutusjaksonHyvaksyntaForm => ({
  laillistamispaiva: null,
  laillistamispaivanLiite: null,
  tyoskentelyjaksoAsiakirjat: [
    {
      id: 1,
      addedFiles: Array(addedCount).fill(null) as File[],
      deletedFiles: Array(deletedCount).fill(1)
    }
  ]
})

describe('Terveyskeskuskoulutusjakson multipart-osat', () => {
  it('laskee YEK-lähetyksessä lisäykset ja poistot samaan 100 osan rajaan', () => {
    expect(hasTooManyTerveyskeskuskoulutusjaksoParts(form(90, 10), true)).toBe(false)
    expect(hasTooManyTerveyskeskuskoulutusjaksoParts(form(90, 11), true)).toBe(true)
  })

  it('laskee erikoistuvan lähetyksessä vain yhden lisäyksen ja poistot pyyntöä kohti', () => {
    expect(hasTooManyTerveyskeskuskoulutusjaksoParts(form(90, 99), false)).toBe(false)
    expect(hasTooManyTerveyskeskuskoulutusjaksoParts(form(90, 100), false)).toBe(true)
    expect(hasTooManyTerveyskeskuskoulutusjaksoParts(form(0, 100), false)).toBe(false)
    expect(hasTooManyTerveyskeskuskoulutusjaksoParts(form(0, 101), false)).toBe(true)
  })

  it('tarkistaa kaikki työsuhteet ennen ensimmäistä tallennusta', () => {
    const changes = form(1, 0)
    changes.tyoskentelyjaksoAsiakirjat.push(form(90, 11).tyoskentelyjaksoAsiakirjat[0])
    expect(hasTooManyTerveyskeskuskoulutusjaksoParts(changes, true)).toBe(true)
  })
})
