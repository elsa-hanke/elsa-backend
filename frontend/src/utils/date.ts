import { parseISO } from 'date-fns'
import compareAsc from 'date-fns/compareAsc'
import compareDesc from 'date-fns/compareDesc'
import isAfter from 'date-fns/isAfter'
import isBefore from 'date-fns/isBefore'
import isDate from 'date-fns/isDate'

export function dateBetween(
  date: string | Date | undefined,
  startDate: string | Date | undefined,
  endDate: string | Date | null | undefined
) {
  if (date) {
    if (
      (startDate &&
        isBefore(
          isDate(date) ? (date as Date) : parseISO(date as string),
          isDate(startDate) ? (startDate as Date) : parseISO(startDate as string)
        )) ||
      (endDate &&
        isAfter(
          isDate(date) ? (date as Date) : parseISO(date as string),
          isDate(endDate) ? (endDate as Date) : parseISO(endDate as string)
        ))
    ) {
      return false
    }
  }
  return true
}

export function sortByDateDesc(
  d1: string | Date | null | undefined,
  d2: string | Date | null | undefined
) {
  // Suurimmasta pieninmpään lajitellessa tyhjät päivämäärät (null tai undefined) nostetaan ylimmäksi,
  // koska esim. työskentelyjaksoja lajitellessa kesken olevalla jaksolla ei ole päättymispäivää
  // ja se tulisi olla listan kärjessä.
  if (!d1) {
    return -1
  } else if (!d2) {
    return 1
  }
  return compareDesc(parseISO(d1 as string), parseISO(d2 as string))
}

export function sortByDateAsc(
  d1: string | Date | null | undefined,
  d2: string | Date | null | undefined
) {
  // Vastaavasti taas pienimmästä suurimpaan lajitellessa tyhjät päivämäärät näytetään
  // listan lopussa.
  if (!d1) {
    return 1
  } else if (!d2) {
    return -1
  }
  return compareAsc(parseISO(d1 as string), parseISO(d2 as string))
}

export function isInPast(date: string | Date | null | undefined) {
  return isBefore(isDate(date) ? (date as Date) : parseISO(date as string), new Date())
}

export function daysBetweenDates(date1: Date, date2: Date): number {
  const utcDate1 = Date.UTC(date1.getFullYear(), date1.getMonth(), date1.getDate())
  const utcDate2 = Date.UTC(date2.getFullYear(), date2.getMonth(), date2.getDate())
  const timeDifference = Math.abs(utcDate2 - utcDate1)
  return Math.floor(timeDifference / (1000 * 60 * 60 * 24))
}
