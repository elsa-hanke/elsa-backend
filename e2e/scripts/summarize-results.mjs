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
const failures = []
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
      durationMs: null,
      specDurationMs: 0,
      artifacts: {},
      ...Object.fromEntries(fields.map((field) => [field, 0])),
    }
    shards.push(shard)
    const directory = path.dirname(filename)
    const completedFile = path.join(directory, 'completed.json')
    if (existsSync(completedFile)) {
      const completion = JSON.parse(readFileSync(completedFile, 'utf8'))
      shard.completed = completion.completed === true
      if (Number.isFinite(completion.durationMs)) shard.durationMs = completion.durationMs
    }
    const artifactsFile = path.join(directory, 'artifacts.json')
    if (existsSync(artifactsFile)) shard.artifacts = JSON.parse(readFileSync(artifactsFile, 'utf8'))
    for (const spec of manifest.specs) {
      if (!expectedSpecs.has(spec)) issues.push(`Unexpected assigned spec: ${spec}`)
      if (assigned.has(spec)) issues.push(`Spec assigned more than once: ${spec}`)
      assigned.add(spec)
    }
    for (const resultFile of filesIn(directory).filter((name) => name.endsWith('.json') && !['manifest.json', 'completed.json', 'artifacts.json'].includes(path.basename(name)))) {
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
      if (Number.isFinite(result.stats.duration)) shard.specDurationMs += result.stats.duration
      for (const failure of result.failures ?? []) {
        failures.push({ shard: shard.shard, spec: result.spec, ...failure })
      }
      if (result.stats.failures > (result.failures?.length ?? 0) || result.error) {
        failures.push({
          shard: shard.shard,
          spec: result.spec,
          title: 'Spec failure (individual test details unavailable)',
          error: result.error || 'This artifact contains counts only. Run again to collect failing test names and errors.',
        })
      }
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
const failed = !complete || totals.failures > 0 || (jobResult !== 'success' && jobResult !== 'unknown')
const status = failed ? 'FAILED' : 'PASSED'
const report = { status, complete, jobResult, expectedSpecs: expectedSpecs.size, reportedSpecs: reported.size, totals, shards, failures, issues, missingSpecs }
const clean = (value) => String(value ?? '').replace(/\u001b\[[0-9;]*m/g, '')
const html = (value) => clean(value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
const cell = (value) => html(value).replace(/\|/g, '&#124;').replace(/`/g, '&#96;').replace(/[\r\n]+/g, ' ')
const duration = (milliseconds) => {
  if (milliseconds === null) return 'Unavailable'
  const seconds = Math.round(milliseconds / 1000)
  return `${Math.floor(seconds / 60)}m ${String(seconds % 60).padStart(2, '0')}s`
}
const artifactLinks = (shard) => ['screenshots', 'logs']
  .filter((name) => /^https?:\/\//.test(shard.artifacts[name] ?? ''))
  .map((name) => `[${name}](<${shard.artifacts[name].replace(/[<>\r\n]/g, '')}>)`).join(' · ') || '—'
shards.sort((a, b) => a.shard - b.shard)
const markdown = [
  `## ${failed ? '❌' : '✅'} E2E: ${status}`,
  '',
  `**${totals.passes} passed · ${totals.failures} failed · ${totals.pending} pending · ${totals.skipped} skipped**`,
  '',
  `Coverage: **${reported.size}/${expectedSpecs.size} specs**. Tests reported: **${totals.tests}**. ${complete ? 'All spec results received.' : '**INCOMPLETE: totals are partial.**'}`,
  '',
  ...(process.env.E2E_RUN_URL ? [`[Open workflow run and artifacts](${process.env.E2E_RUN_URL})`, ''] : []),
  '### Shards',
  '',
  '| Shard | Specs | Tests | Passed | Failed | Pending | Skipped | Cypress time | Artifacts |',
  '| --- | --- | --- | --- | --- | --- | --- | --- | --- |',
  ...shards.map((shard) =>
    `| ${shard.shard}${shard.completed ? '' : ' ⚠️'} | ${shard.reportedSpecs}/${shard.assignedSpecs} | ${fields.map((field) => shard[field]).join(' | ')} | ${duration(shard.durationMs ?? (shard.specDurationMs || null))}${shard.durationMs === null && shard.specDurationMs ? ' (partial)' : ''} | ${artifactLinks(shard)} |`),
  `| **Total** | **${reported.size}/${expectedSpecs.size}** | ${fields.map((field) => `**${totals[field]}**`).join(' | ')} | | |`,
  '',
  'Cypress time excludes service setup and dependency installation. Retries are not counted as additional tests. ⚠️ indicates an unfinished shard.',
  '',
  ...(failures.length ? [
    '### Failures', '',
    '| Shard | Spec | Test | Error |',
    '| --- | --- | --- | --- |',
    ...failures.map((failure) => `| ${failure.shard} | ${cell(failure.spec)} | ${cell(failure.title)} | ${cell(clean(failure.error).slice(0, 200))} |`),
    '',
    ...failures.flatMap((failure) => [
      '<details>',
      `<summary>Shard ${failure.shard}: ${html(failure.title)}</summary>`,
      '',
      `<p>${html(failure.spec)}${failure.attempts ? ` · Attempts: ${failure.attempts}` : ''}</p>`,
      `<pre>${html(failure.error)}</pre>`,
      '</details>', '',
    ]),
  ] : []),
  ...(issues.length ? ['### Report issues', '', ...issues.map((issue) => `- ${cell(issue)}`), ''] : []),
  ...(missingSpecs.length ? ['<details>', '<summary>Specs without results</summary>', '', '<pre>', ...missingSpecs.map(html), '</pre>', '</details>', ''] : []),
].join('\n')
mkdirSync(outputDirectory, { recursive: true })
writeFileSync(path.join(outputDirectory, 'report.json'), JSON.stringify(report, null, 2) + '\n')
writeFileSync(path.join(outputDirectory, 'report.md'), markdown)
if (process.env.GITHUB_STEP_SUMMARY) appendFileSync(process.env.GITHUB_STEP_SUMMARY, markdown)
// Logs do not render Markdown. Keep them readable and reserve tables for Summary.
console.log(`E2E ${status}: ${totals.tests} tests — ${totals.passes} passed, ${totals.failures} failed, ${totals.pending} pending, ${totals.skipped} skipped.`)
console.log(`Coverage: ${reported.size}/${expectedSpecs.size} specs (${complete ? 'complete' : 'INCOMPLETE; totals are partial'}). Shard jobs: ${jobResult}.`)
for (const shard of shards) {
  console.log(`  Shard ${shard.shard}: ${shard.reportedSpecs}/${shard.assignedSpecs} specs, ${shard.tests} tests, ${shard.failures} failed, Cypress time ${duration(shard.durationMs ?? (shard.specDurationMs || null))}.`)
}
for (const failure of failures) {
  console.log(`  FAILED [shard ${failure.shard}] ${failure.spec}: ${clean(failure.title)}`)
  console.log(clean(failure.error).split('\n').map((line) => `    ${line}`).join('\n'))
}
for (const issue of issues) console.log(`  Report issue: ${clean(issue)}`)
console.log('Open this workflow run’s Summary tab for the formatted report and artifact links.')
if (failed) process.exitCode = 1
