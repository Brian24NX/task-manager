const DAYS_FULL = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday']
const DAYS_SHORT = ['sun', 'mon', 'tue', 'tues', 'wed', 'thu', 'thurs', 'fri', 'sat']
const DAY_SHORT_TO_IDX = { sun: 0, mon: 1, tue: 2, tues: 2, wed: 3, thu: 4, thurs: 4, fri: 5, sat: 6 }

function isoDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function startOfToday() {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  return d
}

export function parseNaturalDate(input) {
  if (!input) return null
  const raw = String(input).trim().toLowerCase()
  if (!raw) return null

  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) return raw

  const today = startOfToday()

  if (raw === 'today' || raw === 'tod') return isoDate(today)
  if (raw === 'tomorrow' || raw === 'tmr' || raw === 'tmrw') {
    const d = new Date(today); d.setDate(d.getDate() + 1); return isoDate(d)
  }
  if (raw === 'yesterday' || raw === 'yday') {
    const d = new Date(today); d.setDate(d.getDate() - 1); return isoDate(d)
  }

  const inMatch = raw.match(/^in\s+(\d+)\s+(day|days|week|weeks|month|months)$/)
  if (inMatch) {
    const n = parseInt(inMatch[1], 10)
    const unit = inMatch[2]
    const d = new Date(today)
    if (unit.startsWith('day')) d.setDate(d.getDate() + n)
    else if (unit.startsWith('week')) d.setDate(d.getDate() + n * 7)
    else if (unit.startsWith('month')) d.setMonth(d.getMonth() + n)
    return isoDate(d)
  }

  const plusMatch = raw.match(/^\+(\d+)(d|w|m)?$/)
  if (plusMatch) {
    const n = parseInt(plusMatch[1], 10)
    const unit = plusMatch[2] || 'd'
    const d = new Date(today)
    if (unit === 'd') d.setDate(d.getDate() + n)
    else if (unit === 'w') d.setDate(d.getDate() + n * 7)
    else if (unit === 'm') d.setMonth(d.getMonth() + n)
    return isoDate(d)
  }

  const parts = raw.split(/\s+/)
  let forceNext = false
  let dayIdx = -1
  for (const p of parts) {
    if (p === 'next') { forceNext = true; continue }
    if (p === 'this') continue
    const fullIdx = DAYS_FULL.indexOf(p)
    if (fullIdx >= 0) { dayIdx = fullIdx; continue }
    if (DAY_SHORT_TO_IDX[p] !== undefined) { dayIdx = DAY_SHORT_TO_IDX[p]; continue }
  }
  if (dayIdx >= 0) {
    const d = new Date(today)
    let diff = (dayIdx - d.getDay() + 7) % 7
    if (diff === 0) diff = 7
    if (forceNext && diff < 7) diff += 7
    d.setDate(d.getDate() + diff)
    return isoDate(d)
  }

  return null
}
