export function sortByAsc(
  a: string | number | null | undefined,
  b: string | number | null | undefined,
  nullsLast = false
) {
  if (!a && !b) {
    return 0
  } else if (!a) {
    return nullsLast ? 1 : -1
  } else if (!b) {
    return nullsLast ? -1 : 1
  }
  return a < b ? -1 : b < a ? 1 : 0
}

export function sortByDesc(
  a: string | number | null | undefined,
  b: string | number | null | undefined
) {
  if (!a && !b) {
    return 0
  } else if (!a) {
    return 1
  } else if (!b) {
    return -1
  }
  return a < b ? 1 : b < a ? -1 : 0
}
