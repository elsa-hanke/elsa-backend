export function getKeskiarvoFormatted(keskiarvo: number) {
  const decimalsAmount =
    Math.floor(keskiarvo) === keskiarvo ? 0 : keskiarvo.toString().split('.')[1]?.length || 0
  return decimalsAmount > 2 ? keskiarvo.toFixed(2) : keskiarvo
}
