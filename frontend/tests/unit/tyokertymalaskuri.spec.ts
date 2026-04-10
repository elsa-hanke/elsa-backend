import { parseISO } from 'date-fns'

import {
  HyvaksiluettavatCounterData,
  TyokertymaLaskuriPoissaolo,
  TyokertymaLaskuriTyoskentelyjakso
} from '@/types'
import { KaytannonKoulutusTyyppi, PoissaolonSyyTyyppi } from '@/utils/constants'
import {
  calculateAmountOfReducedDaysAndUpdateHyvaksiluettavatCounter,
  getAmountOfReducedDaysAndHyvaksiluettavatUsed,
  getKeskeytysaikaMap,
  getVahennettavatPaivat
} from '@/views/tyokertymalaskuri/tyoskentelyjakson-pituus-counter'

describe('Tyokertymalaskuri', () => {
  it('pitaisi palauttaa oikeat keskeytysaika mapit', () => {
    const keskeytys1: Map<number, number> = getKeskeytysaikaMap(
      parseISO('2020-11-02'),
      parseISO('2021-06-19'),
      1.0,
      1.0
    )
    expect(keskeytys1.get(2020)).toBe(60.0)
    expect(keskeytys1.get(2021)).toBe(170.0)

    const keskeytys2: Map<number, number> = getKeskeytysaikaMap(
      parseISO('2021-07-26'),
      parseISO('2021-09-14'),
      1.0,
      1.0
    )
    expect(keskeytys2.get(2021)).toBe(51)
  })

  it('pitaisi palauttaa oikeat reduced days ja hyvaksiluettavat used paivat', () => {
    const reduced1: { amountOfReducedDays: number; hyvaksiluettavatUsed: number } =
      getAmountOfReducedDaysAndHyvaksiluettavatUsed(60.0, 30.0)
    expect(reduced1.amountOfReducedDays).toBe(30.0)
    expect(reduced1.hyvaksiluettavatUsed).toBe(30.0)

    const reduced2: { amountOfReducedDays: number; hyvaksiluettavatUsed: number } =
      getAmountOfReducedDaysAndHyvaksiluettavatUsed(170.0, 0.0)
    expect(reduced2.amountOfReducedDays).toBe(170.0)
    expect(reduced2.hyvaksiluettavatUsed).toBe(0.0)

    const reduced3: { amountOfReducedDays: number; hyvaksiluettavatUsed: number } =
      getAmountOfReducedDaysAndHyvaksiluettavatUsed(51.0, 0.0)
    expect(reduced3.amountOfReducedDays).toBe(51.0)
    expect(reduced3.hyvaksiluettavatUsed).toBe(0.0)
  })

  it('pitaisi palauttaa oikea reduced days arvo', () => {
    const hyvaksiluettavatCounter: HyvaksiluettavatCounterData = {
      hyvaksiluettavatDays: new Map<string, number>(),
      hyvaksiluettavatPerYearMap: new Map<number, number>()
    }
    hyvaksiluettavatCounter.hyvaksiluettavatDays.set(
      'Äitiysloma, isyysloma, vanhempainvapaa (palkallinen/palkaton), lapsi 1',
      0.0
    )
    hyvaksiluettavatCounter.hyvaksiluettavatPerYearMap.set(2020, 0.0)
    hyvaksiluettavatCounter.hyvaksiluettavatPerYearMap.set(2021, 30.0)

    const keskeytysaika: TyokertymaLaskuriPoissaolo = {
      alkamispaiva: '2021-07-26',
      paattymispaiva: '2021-09-14',
      tyoskentelyjakso: {
        id: 1,
        // alkamispaiva: '2020-01-09',
        paattymispaiva: '2021-12-29',
        kaytannonKoulutus: KaytannonKoulutusTyyppi.MUU_ERIKOISALA,
        osaaikaprosentti: 100,
        kahdenvuodenosaaikaprosentti: 100
      },
      poissaolonSyyId: 4,
      poissaolonSyy: {
        id: 4,
        nimi: 'Äitiysloma, isyysloma, vanhempainvapaa (palkallinen/palkaton), lapsi 1',
        vahennystyyppi: PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI,
        vahennetaanKerran: true,
        voimassaolonAlkamispaiva: '2020-08-01',
        voimassaolonPaattymispaiva: null
      },
      tyoskentelyjaksoId: 1,
      kokoTyoajanPoissaolo: true,
      poissaoloprosentti: 100
    }

    const test = calculateAmountOfReducedDaysAndUpdateHyvaksiluettavatCounter(
      keskeytysaika,
      1.0,
      hyvaksiluettavatCounter,
      parseISO('2021-12-29')
    )
    expect(test).toBe(51)
  })

  it('pitaisi palauttaa oikea maara vahennettavia paivia', () => {
    const tyoskentelyjaksot: TyokertymaLaskuriTyoskentelyjakso[] = [
      {
        id: 1,
        tyoskentelypaikka: { nimi: 'testipaikka', tyyppi: null, muuTyyppi: null },
        alkamispaiva: '2020-01-09',
        paattymispaiva: '2021-12-29',
        kaytannonKoulutus: KaytannonKoulutusTyyppi.MUU_ERIKOISALA,
        osaaikaprosentti: 100,
        kahdenvuodenosaaikaprosentti: 100,
        poissaolot: [
          {
            alkamispaiva: '2020-11-02',
            paattymispaiva: '2021-06-19',
            tyoskentelyjakso: {
              id: -1,
              osaaikaprosentti: 100,
              kahdenvuodenosaaikaprosentti: 100,
              paattymispaiva: '2021-12-29'
            },
            poissaolonSyyId: 0,
            poissaolonSyy: {
              id: 4,
              nimi: 'Äitiysloma, isyysloma, vanhempainvapaa (palkallinen/palkaton), lapsi 1',
              vahennystyyppi: PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI,
              vahennetaanKerran: true,
              voimassaolonAlkamispaiva: '2020-08-01',
              voimassaolonPaattymispaiva: null
            },
            tyoskentelyjaksoId: 0,
            kokoTyoajanPoissaolo: true,
            poissaoloprosentti: 100
          },
          {
            alkamispaiva: '2021-07-26',
            paattymispaiva: '2021-09-14',
            tyoskentelyjakso: {
              id: -1,
              osaaikaprosentti: 100,
              kahdenvuodenosaaikaprosentti: 100,
              paattymispaiva: '2021-12-29'
            },
            poissaolonSyyId: 0,
            poissaolonSyy: {
              id: 4,
              nimi: 'Äitiysloma, isyysloma, vanhempainvapaa (palkallinen/palkaton), lapsi 1',
              vahennystyyppi: PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI,
              vahennetaanKerran: true,
              voimassaolonAlkamispaiva: '2020-08-01',
              voimassaolonPaattymispaiva: null
            },
            tyoskentelyjaksoId: 0,
            kokoTyoajanPoissaolo: true,
            poissaoloprosentti: 100
          }
        ]
      }
    ]
    const vahennettavat: Map<number, number> = getVahennettavatPaivat(tyoskentelyjaksot)
    expect(vahennettavat.get(1)).toBe(251)
  })
})
