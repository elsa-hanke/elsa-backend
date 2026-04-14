/* eslint-disable @typescript-eslint/no-non-null-assertion */

import { endOfYear, parseISO, startOfYear } from 'date-fns'

import { daysBetween } from './dateUtils'

import {
  HyvaksiluettavatCounterData,
  TyokertymaLaskuriPoissaolo,
  TyokertymaLaskuriTyoskentelyjakso
} from '@/types'
import { PoissaolonSyyTyyppi } from '@/utils/constants'

const HYVAKSILUETTAVAT_DAYS = 30.0

export function getVahennettavatPaivat(
  tyoskentelyjaksot: TyokertymaLaskuriTyoskentelyjakso[]
): Map<number, number> {
  const result = new Map<number, number>()
  const hyvaksiluettavatCounter: HyvaksiluettavatCounterData = {
    hyvaksiluettavatDays: new Map<string, number>(),
    hyvaksiluettavatPerYearMap: getHyvaksiluettavatPerYearMap(tyoskentelyjaksot)
  }
  const now = new Date()
  tyoskentelyjaksot
    .flatMap((jakso: TyokertymaLaskuriTyoskentelyjakso) =>
      jakso.poissaolot
        .filter((poissaolo) => poissaolo.alkamispaiva)
        .map((poissaolo: TyokertymaLaskuriPoissaolo) => {
          return {
            ...poissaolo,
            tyoskentelyjakso: jakso,
            tyoskentelyjaksoId: jakso.id,
            poissaolonSyyId: poissaolo.poissaolonSyy.id || 0
          }
        })
    )
    .sort((a, b) => parseISO(a.alkamispaiva).getTime() - parseISO(b.alkamispaiva).getTime())
    .forEach((keskeytys: TyokertymaLaskuriPoissaolo) => {
      const tyoskentelyjaksoFactor = (keskeytys.tyoskentelyjakso?.osaaikaprosentti ?? 100) / 100.0

      const endDate = keskeytys.tyoskentelyjakso?.paattymispaiva
        ? parseISO(keskeytys.tyoskentelyjakso.paattymispaiva)
        : now
      const effectiveEndDate = endDate.getTime() > now.getTime() ? now : endDate

      const amountOfReducedDays = calculateAmountOfReducedDaysAndUpdateHyvaksiluettavatCounter(
        keskeytys,
        tyoskentelyjaksoFactor,
        hyvaksiluettavatCounter,
        effectiveEndDate
      )
      const tyoskentelyjaksoId = keskeytys.tyoskentelyjakso.id
      result.set(tyoskentelyjaksoId, (result.get(tyoskentelyjaksoId) ?? 0) + amountOfReducedDays)
    })
  return result
}

export function calculateHyvaksiluettavatDaysLeft(
  tyoskentelyjaksot: TyokertymaLaskuriTyoskentelyjakso[],
  calculateUntilDate: Date | null
): HyvaksiluettavatCounterData {
  const hyvaksiluettavatCounterData: HyvaksiluettavatCounterData = {
    hyvaksiluettavatDays: new Map(),
    hyvaksiluettavatPerYearMap: getHyvaksiluettavatPerYearMap(tyoskentelyjaksot)
  }

  const keskeytykset = tyoskentelyjaksot
    .flatMap((jakso: TyokertymaLaskuriTyoskentelyjakso) =>
      jakso.poissaolot.map((poissaolo: TyokertymaLaskuriPoissaolo) => {
        poissaolo.tyoskentelyjakso = jakso
        return poissaolo
      })
    )
    .sort((a: TyokertymaLaskuriPoissaolo, b: TyokertymaLaskuriPoissaolo) => {
      if (!a.alkamispaiva && !b.alkamispaiva) return 0
      if (!a.alkamispaiva) return 1
      if (!b.alkamispaiva) return -1
      const dateA = parseISO(a.alkamispaiva)
      const dateB = parseISO(b.alkamispaiva)
      return dateA.getTime() - dateB.getTime()
    })

  keskeytykset.forEach((keskeytys: TyokertymaLaskuriPoissaolo) => {
    const tyoskentelyjaksoFactor = keskeytys.tyoskentelyjakso?.osaaikaprosentti || 100 / 100.0
    calculateAmountOfReducedDaysAndUpdateHyvaksiluettavatCounter(
      keskeytys,
      tyoskentelyjaksoFactor,
      hyvaksiluettavatCounterData,
      calculateUntilDate
    )
  })

  return hyvaksiluettavatCounterData
}

export function getHyvaksiluettavatPerYearMap(
  tyoskentelyjaksot: TyokertymaLaskuriTyoskentelyjakso[]
): Map<number, number> {
  const hyvaksiluettavatPerYearMap = new Map<number, number>()

  if (tyoskentelyjaksot.length === 0) {
    return hyvaksiluettavatPerYearMap
  }

  const minDate = new Date(
    Math.min(...tyoskentelyjaksot.map((j) => parseISO(j.alkamispaiva).getTime()))
  )
  const maxDate = new Date(
    Math.max(
      ...tyoskentelyjaksot.map((j) =>
        j.paattymispaiva === null ? Date.now() : parseISO(j.paattymispaiva).getTime()
      )
    )
  )

  for (let year = minDate.getFullYear(); year <= maxDate.getFullYear(); year++) {
    hyvaksiluettavatPerYearMap.set(year, HYVAKSILUETTAVAT_DAYS)
  }

  return hyvaksiluettavatPerYearMap
}

export function calculateAmountOfReducedDaysAndUpdateHyvaksiluettavatCounter(
  keskeytysaika: TyokertymaLaskuriPoissaolo,
  tyoskentelyjaksoFactor: number,
  hyvaksiluettavatCounterData: HyvaksiluettavatCounterData,
  calculateUntilDate: Date | null
): number {
  const endDate = getEndDate(parseISO(keskeytysaika.paattymispaiva!), calculateUntilDate)
  const keskeytysaikaDaysBetween = daysBetween(parseISO(keskeytysaika.alkamispaiva!), endDate)

  if (keskeytysaikaDaysBetween < 1) return 0.0

  const keskeytysaikaProsentti = keskeytysaika.poissaoloprosentti ?? 100
  const keskeytysaikaFactor = keskeytysaikaProsentti / 100.0

  const keskeytysaikaLength =
    keskeytysaikaFactor * tyoskentelyjaksoFactor * keskeytysaikaDaysBetween
  const vahennetaanKerran = keskeytysaika.poissaolonSyy!.vahennetaanKerran

  switch (keskeytysaika.poissaolonSyy?.vahennystyyppi) {
    case PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN: {
      return keskeytysaikaLength
    }
    case PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI: {
      if (vahennetaanKerran) {
        if (
          !hyvaksiluettavatCounterData.hyvaksiluettavatDays.has(keskeytysaika.poissaolonSyy.nimi)
        ) {
          hyvaksiluettavatCounterData.hyvaksiluettavatDays.set(
            keskeytysaika.poissaolonSyy.nimi,
            30.0
          )
        }
      }

      const keskeytysaikaMap = getKeskeytysaikaMap(
        parseISO(keskeytysaika.alkamispaiva!),
        endDate,
        keskeytysaikaFactor,
        tyoskentelyjaksoFactor
      )
      let reducedDaysTotal = 0.0
      keskeytysaikaMap.forEach((days: number, year: number) => {
        // Tarkistetaan hyväksiluettavat päivät vuosittaisesta määrästä ja
        // vain kerran hyväksyttävien keskeytyksien (vanhempainvapaat) osalta
        // poissaolokohtaisesta määrästä. Hyväksiluetaan näistä niin paljon kuin
        // pystytään ja päivitetään molemmat laskurit.

        const hyvaksiLuettavatLeft = vahennetaanKerran
          ? Math.min(
              hyvaksiluettavatCounterData.hyvaksiluettavatPerYearMap.get(year)!,
              hyvaksiluettavatCounterData.hyvaksiluettavatDays.get(
                keskeytysaika.poissaolonSyy.nimi
              )!
            )
          : hyvaksiluettavatCounterData.hyvaksiluettavatPerYearMap.get(year)!

        const { amountOfReducedDays, hyvaksiluettavatUsed } =
          getAmountOfReducedDaysAndHyvaksiluettavatUsed(days, hyvaksiLuettavatLeft)

        if (vahennetaanKerran) {
          hyvaksiluettavatCounterData.hyvaksiluettavatDays.set(
            keskeytysaika.poissaolonSyy.nimi,
            Math.max(
              0.0,
              hyvaksiluettavatCounterData.hyvaksiluettavatDays.get(
                keskeytysaika.poissaolonSyy.nimi
              )! - hyvaksiluettavatUsed
            )
          )
        }

        hyvaksiluettavatCounterData.hyvaksiluettavatPerYearMap.set(
          year,
          Math.max(
            0.0,
            hyvaksiluettavatCounterData.hyvaksiluettavatPerYearMap.get(year)! - hyvaksiluettavatUsed
          )
        )
        reducedDaysTotal += amountOfReducedDays
      })
      return reducedDaysTotal
    }
  }
  return 0.0
}

export function getAmountOfReducedDaysAndHyvaksiluettavatUsed(
  keskeytysaikaLength: number,
  hyvaksiluettavatLeft: number
): { amountOfReducedDays: number; hyvaksiluettavatUsed: number } {
  let amountOfReducedDays = 0.0
  let hyvaksiluettavatUsed = 0.0

  if (hyvaksiluettavatLeft === 0.0) {
    // Jäljellä olevia hyväksiluettavia päiviä ei ole jäljellä, joten vähennetään
    // keskeytysajan pituus.
    amountOfReducedDays = keskeytysaikaLength
  } else if (keskeytysaikaLength >= hyvaksiluettavatLeft) {
    // Keskeytysajan pituus on suurempi tai yhtä suuri kuin jäljellä olevien
    // hyväksiluettavien päivien määrä, joten vähennetään työskentelyjaksosta
    // keskeystysajan pituuden ja jäljellä olevien hyväksiluettavien päivien erotus sekä
    // asetetaan jäljellä olevien hyväksiluettavien päivien määrä nollaan.
    amountOfReducedDays = parseFloat((keskeytysaikaLength - hyvaksiluettavatLeft).toFixed(2))
    hyvaksiluettavatUsed = hyvaksiluettavatLeft
  } else {
    // Keskeytysajan pituus on pienempi kuin jäljellä olevien hyväksiluettavien päivien määrä,
    // vähennetään vain jäljellä olevien hyväksiluettavien päivien määrää.
    hyvaksiluettavatUsed = keskeytysaikaLength
  }
  return { amountOfReducedDays, hyvaksiluettavatUsed }
}

export function getKeskeytysaikaMap(
  startDate: Date,
  endDate: Date,
  keskeytysaikaFactor: number,
  tyoskentelyjaksoFactor: number
): Map<number, number> {
  const keskeytysaikaMap = new Map<number, number>()
  let currentYear = startDate.getFullYear()
  const lastYear = endDate.getFullYear()

  if (currentYear === lastYear) {
    const days = daysBetween(startDate, endDate)
    keskeytysaikaMap.set(currentYear, days * keskeytysaikaFactor * tyoskentelyjaksoFactor)
  } else {
    const daysInFirstYear = daysBetween(startDate, endOfYear(startDate))
    keskeytysaikaMap.set(
      currentYear,
      daysInFirstYear * keskeytysaikaFactor * tyoskentelyjaksoFactor
    )

    currentYear++

    while (currentYear < lastYear) {
      const fullYearDays = daysBetween(
        startOfYear(new Date(currentYear, 0, 1)),
        endOfYear(startOfYear(new Date(currentYear, 0, 1)))
      )
      keskeytysaikaMap.set(currentYear, fullYearDays * keskeytysaikaFactor * tyoskentelyjaksoFactor)
      currentYear++
    }

    const daysInLastYear = daysBetween(startOfYear(new Date(lastYear, 0, 1)), endDate)
    keskeytysaikaMap.set(currentYear, daysInLastYear * keskeytysaikaFactor * tyoskentelyjaksoFactor)
  }
  return keskeytysaikaMap
}

function getEndDate(endDate: Date, calculateUntilDate: Date | null): Date {
  if (calculateUntilDate) {
    return endDate > calculateUntilDate ? calculateUntilDate : endDate
  } else {
    return endDate
  }
}
