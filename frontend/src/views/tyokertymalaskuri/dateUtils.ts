import { differenceInDays } from 'date-fns/fp'

export function daysBetween(startDate: Date, endDate: Date) {
  return differenceInDays(startDate, endDate) + 1
}
