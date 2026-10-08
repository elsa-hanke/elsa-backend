import { appendFileSync, existsSync, mkdirSync, readdirSync, readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const inputDirectory = process.argv[2]
const outputDirectory = process.argv[3]
if (!inputDirectory || !outputDirectory) {
  throw new Error('Usage: node scripts/summarize-results.mjs <downloaded artifacts directory> <report directory>')
}

function filesIn(directory) {
  if (!existsSync(directory)) return []
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const filename = path.join(directory, entry.name)
    return entry.isDirectory() ? filesIn(filename) : [filename]
  })
}

const specsRoot = fileURLToPath(new URL('../cypress/e2e/', import.meta.url))
const expectedSpecs = new Set(filesIn(specsRoot)
  .filter((filename) => filename.endsWith('.cy.ts'))
  .map((filename) => `cypress/e2e/${path.relative(specsRoot, filename).split(path.sep).join('/')}`))
const fields = ['tests', 'passes', 'failures', 'pending', 'skipped']
const totals = Object.fromEntries(fields.map((field) => [field, 0]))
const issues = []
const shards = []
const assigned = new Set()
const reported = new Set()
let totalShards = null

for (const filename of filesIn(inputDirectory).filter((name) => path.basename(name) === 'manifest.json').sort()) {
  try {
    const manifest = JSON.parse(readFileSync(filename, 'utf8'))
    if (!Number.isInteger(manifest.totalShards) || manifest.totalShards < 1 ||
        !Number.isInteger(manifest.shard) || manifest.shard < 1 || manifest.shard > manifest.totalShards ||
        !Array.isArray(manifest.specs) || !manifest.specs.length) {
      throw new Error('Invalid shard manifest')
    }
    if (totalShards !== null && totalShards !== manifest.totalShards) {
      throw new Error('Shard totals do not match')
    }
    totalShards = manifest.totalShards
    if (shards.some((shard) => shard.shard === manifest.shard)) {
      throw new Error(`Duplicate shard ${manifest.shard}`)
    }
    const shard = {
      shard: manifest.shard,
      assignedSpecs: manifest.specs.length,
      reportedSpecs: 0,
      completed: false,
      ...Object.fromEntries(fields.map((field) => [field, 0])),
    }
    shards.push(shard)
    const directory = path.dirname(filename)
    const completedFile = path.join(directory, 'completed.json')
    shard.completed = existsSync(completedFile) && JSON.parse(readFileSync(completedFile, 'utf8')).completed === true
    for (const spec of manifest.specs) {
      if (!expectedSpecs.has(spec)) issues.push(`Unexpected assigned spec: ${spec}`)
      if (assigned.has(spec)) issues.push(`Spec assigned more than once: ${spec}`)
      assigned.add(spec)
    }
    for (const resultFile of filesIn(directory).filter((name) => name.endsWith('.json') && !['manifest.json', 'completed.json'].includes(path.basename(name)))) {
      const result = JSON.parse(readFileSync(resultFile, 'utf8'))
      if (!manifest.specs.includes(result.spec) || reported.has(result.spec)) {
        throw new Error(`Unexpected or duplicate spec result: ${result.spec}`)
      }
      for (const field of fields) {
        if (!Number.isInteger(result.stats?.[field]) || result.stats[field] < 0) {
          throw new Error(`Invalid ${field} count for ${result.spec}`)
        }
      }
      if (result.stats.tests !== result.stats.passes + result.stats.failures + result.stats.pending + result.stats.skipped) {
        throw new Error(`Test counts do not add up for ${result.spec}`)
      }
      reported.add(result.spec)
      shard.reportedSpecs++
      for (const field of fields) {
        shard[field] += result.stats[field]
        totals[field] += result.stats[field]
      }
    }
    if (!shard.completed) issues.push(`Shard ${shard.shard}: Cypress did not finish normally`)
    if (shard.reportedSpecs !== shard.assignedSpecs) {
      issues.push(`Shard ${shard.shard}: results for ${shard.reportedSpecs}/${shard.assignedSpecs} assigned specs`)
    }
  } catch (error) {
    issues.push(`${filename}: ${error.message}`)
  }
}

if (totalShards === null) issues.push('No shard manifests received; test totals are unavailable')
else {
  for (let index = 1; index <= totalShards; index++) {
    if (!shards.some((shard) => shard.shard === index)) issues.push(`Missing shard ${index} artifact`)
  }
}
const missingSpecs = [...expectedSpecs].filter((spec) => !reported.has(spec)).sort()
for (const spec of expectedSpecs) {
  if (!assigned.has(spec)) issues.push(`Spec not assigned to any received shard: ${spec}`)
}
const complete = issues.length === 0 && missingSpecs.length === 0
const jobResult = process.env.E2E_JOB_RESULT ?? 'unknown'
const report = { complete, jobResult, expectedSpecs: expectedSpecs.size, reportedSpecs: reported.size, totals, shards, issues, missingSpecs }
const markdown = [
  '## E2E combined report',
  '',
  `Shard jobs: **${jobResult}**. Report: **${complete ? 'Complete' : 'INCOMPLETE — totals are partial'}**.`,
  '',
  `Spec results: **${reported.size}/${expectedSpecs.size}**. Tests reported: **${totals.tests}**.`,
  '',
  '| Shard | Specs reported / assigned | Tests | Passing | Failing | Pending | Skipped | Run finished |',
  '| --- | --- | --- | --- | --- | --- | --- | --- |',
  ...shards.sort((a, b) => a.shard - b.shard).map((shard) =>
    `| ${shard.shard} | ${shard.reportedSpecs}/${shard.assignedSpecs} | ${fields.map((field) => shard[field]).join(' | ')} | ${shard.completed ? 'Yes' : 'No'} |`),
  `| **Total** | **${reported.size}/${expectedSpecs.size}** | ${fields.map((field) => `**${totals[field]}**`).join(' | ')} | |`,
  '',
  'Counts come from Cypress results for completed specs. Retries are not counted as additional tests.',
  '',
  ...(issues.length ? ['### Report issues', '', ...issues.map((issue) => `- ${issue}`), ''] : []),
  ...(missingSpecs.length ? ['### Specs without results', '', '```text', ...missingSpecs, '```', ''] : []),
].join('\n')
mkdirSync(outputDirectory, { recursive: true })
writeFileSync(path.join(outputDirectory, 'report.json'), JSON.stringify(report, null, 2) + '\n')
writeFileSync(path.join(outputDirectory, 'report.md'), markdown)
if (process.env.GITHUB_STEP_SUMMARY) appendFileSync(process.env.GITHUB_STEP_SUMMARY, markdown)
console.log(markdown)
if (!complete || totals.failures > 0 || (jobResult !== 'success' && jobResult !== 'unknown')) process.exitCode = 1
