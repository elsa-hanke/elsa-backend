import { appendFileSync, mkdirSync, readdirSync, readFileSync, writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

// Resolve from the script so local and Docker invocations select the same specs.
const e2eRoot = fileURLToPath(new URL('../', import.meta.url))
const specsRoot = path.join(e2eRoot, 'cypress/e2e')
const durations = JSON.parse(readFileSync(new URL('./spec-durations.json', import.meta.url), 'utf8'))
const shardIndex = Number(process.argv[2])
const shardTotal = Number(process.argv[3])

if (!Number.isInteger(shardTotal) || shardTotal < 1 ||
    !Number.isInteger(shardIndex) || shardIndex < 1 || shardIndex > shardTotal) {
  throw new Error('Usage: node scripts/split-specs.mjs <shard index (1-based)> <shard total>')
}

function discoverSpecs(directory, prefix = '') {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const relativePath = `${prefix}${entry.name}`
    if (entry.isDirectory()) {
      return discoverSpecs(path.join(directory, entry.name), `${relativePath}/`)
    }
    return entry.isFile() && entry.name.endsWith('.cy.ts') ? [relativePath] : []
  })
}

const knownDurations = Object.values(durations)
if (!knownDurations.length || knownDurations.some((value) => !Number.isFinite(value) || value <= 0)) {
  throw new Error('Spec durations must contain positive numbers of seconds')
}
const fallbackDuration = Math.round(knownDurations.reduce((sum, value) => sum + value, 0) / knownDurations.length)
const specs = discoverSpecs(specsRoot).map((spec) => ({
  spec,
  seconds: durations[spec] ?? fallbackDuration,
}))
if (specs.length < shardTotal) {
  throw new Error(`Cannot split ${specs.length} specs into ${shardTotal} nonempty shards`)
}

// Assign the longest specs first to the least loaded shard, balancing time
// without restricting how many files each shard can receive.
const comparePaths = (a, b) => a < b ? -1 : a > b ? 1 : 0
specs.sort((a, b) => b.seconds - a.seconds || comparePaths(a.spec, b.spec))
const shards = Array.from({ length: shardTotal }, () => ({
  specs: [],
  seconds: 0,
}))
for (const spec of specs) {
  const shard = shards.reduce((best, candidate) => {
    return !best || candidate.seconds < best.seconds ? candidate : best
  }, null)
  shard.specs.push(spec.spec)
  shard.seconds += spec.seconds
}

const selected = shards[shardIndex - 1]
selected.specs.sort(comparePaths)
if (process.env.E2E_RESULTS_DIR) {
  mkdirSync(process.env.E2E_RESULTS_DIR, { recursive: true })
  writeFileSync(path.join(process.env.E2E_RESULTS_DIR, 'manifest.json'), JSON.stringify({
    shard: shardIndex,
    totalShards: shardTotal,
    specs: selected.specs.map((spec) => `cypress/e2e/${spec}`),
  }, null, 2))
}
console.error(`E2E shard ${shardIndex}/${shardTotal}: ${selected.specs.length} specs, estimated ${selected.seconds}s`)
console.error(selected.specs.join('\n'))
if (process.env.GITHUB_STEP_SUMMARY) {
  appendFileSync(process.env.GITHUB_STEP_SUMMARY, [
    `### E2E shard ${shardIndex}/${shardTotal}`,
    '',
    `Assigned specs: **${selected.specs.length}**. Estimated Cypress execution time: **${selected.seconds}s**.`,
    '',
    'This is the assigned spec list, not a report of completed tests.',
    '',
    '```text',
    ...selected.specs.map((spec) => `cypress/e2e/${spec}`),
    '```',
    '',
    '',
  ].join('\n'))
}
// Keep stdout machine-readable for the Cypress --spec argument.
console.log(selected.specs.map((spec) => `cypress/e2e/${spec}`).join(','))
