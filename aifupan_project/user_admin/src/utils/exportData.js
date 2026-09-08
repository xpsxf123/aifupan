const pad2 = (n) => String(n).padStart(2, '0')

const buildTimestamp = () => {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = pad2(d.getMonth() + 1)
  const dd = pad2(d.getDate())
  const hh = pad2(d.getHours())
  const mi = pad2(d.getMinutes())
  const ss = pad2(d.getSeconds())
  return `${yyyy}${mm}${dd}_${hh}${mi}${ss}`
}

const sanitizeFilename = (name) => {
  const raw = String(name || '').trim()
  const safe = raw.replace(/[\\/:*?"<>|]+/g, '_').replace(/\s+/g, ' ')
  return safe || 'export'
}

const escapeXml = (value) => {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}

const normalizeValue = (value) => {
  if (value === null || value === undefined) return ''
  if (typeof value === 'string') return value
  if (typeof value === 'number' || typeof value === 'boolean' || typeof value === 'bigint') return String(value)
  if (Array.isArray(value))
    return value
      .map((v) => normalizeValue(v))
      .filter(Boolean)
      .join(', ')
  if (value instanceof Date) return value.toISOString()
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value)
    } catch (e) {
      return String(value)
    }
  }
  return String(value)
}

const normalizeColumns = (columns) => {
  const list = Array.isArray(columns) ? columns : []
  return list
    .map((c) => ({
      label: c?.label ?? c?.title ?? c?.name ?? '',
      prop: c?.prop,
      formatter: typeof c?.formatter === 'function' ? c.formatter : null
    }))
    .filter((c) => c.prop)
}

const pickColumns = (data, columns) => {
  const normalized = normalizeColumns(columns)
  if (normalized.length) return normalized
  const first = Array.isArray(data) && data.length ? data[0] : null
  const keys = first && typeof first === 'object' ? Object.keys(first) : []
  return keys.map((k) => ({ label: k, prop: k, formatter: null }))
}

const getCellString = (row, col) => {
  if (!row || !col?.prop) return ''
  try {
    if (col.formatter) return normalizeValue(col.formatter(row))
  } catch (e) {
    return normalizeValue(row[col.prop])
  }
  return normalizeValue(row[col.prop])
}

const csvEscape = (cell, delimiter) => {
  const s = String(cell ?? '')
  const needs = s.includes('"') || s.includes('\n') || s.includes('\r') || s.includes(delimiter)
  const escaped = s.replace(/"/g, '""')
  return needs ? `"${escaped}"` : escaped
}

const buildCsv = ({ data, columns, delimiter }) => {
  const cols = pickColumns(data, columns)
  const header = cols.map((c) => csvEscape(c.label || c.prop, delimiter)).join(delimiter)
  const rows = (Array.isArray(data) ? data : []).map((row) =>
    cols.map((c) => csvEscape(getCellString(row, c), delimiter)).join(delimiter)
  )
  return [header, ...rows].join('\r\n')
}

const buildTxt = ({ data, columns }) => {
  const cols = pickColumns(data, columns)
  const header = cols.map((c) => String(c.label || c.prop)).join('\t')
  const rows = (Array.isArray(data) ? data : []).map((row) => cols.map((c) => getCellString(row, c)).join('\t'))
  return [header, ...rows].join('\r\n')
}

const buildXml = ({ data, columns }) => {
  const cols = pickColumns(data, columns)
  const rows = (Array.isArray(data) ? data : []).map((row) => {
    const fields = cols.map((c) => `<${c.prop}>${escapeXml(getCellString(row, c))}</${c.prop}>`).join('')
    return `<record>${fields}</record>`
  })
  return `<?xml version="1.0" encoding="UTF-8"?><records>${rows.join('')}</records>`
}

const buildHtmlTable = ({ data, columns, title }) => {
  const cols = pickColumns(data, columns)
  const thead = `<tr>${cols.map((c) => `<th>${escapeXml(c.label || c.prop)}</th>`).join('')}</tr>`
  const tbody = (Array.isArray(data) ? data : [])
    .map((row) => `<tr>${cols.map((c) => `<td>${escapeXml(getCellString(row, c))}</td>`).join('')}</tr>`)
    .join('')
  const safeTitle = escapeXml(title || '')
  return `<!DOCTYPE html><html><head><meta charset="UTF-8" /><title>${safeTitle}</title></head><body><table border="1"><thead>${thead}</thead><tbody>${tbody}</tbody></table></body></html>`
}

export const buildExportPayload = ({ data, columns, format, fileName }) => {
  const fmt = String(format || '').toLowerCase()
  const safeName = sanitizeFilename(fileName)
  const nameWithTs = `${safeName}_${buildTimestamp()}`

  if (fmt === 'json') {
    return {
      content: JSON.stringify(Array.isArray(data) ? data : [], null, 2),
      mimeType: 'application/json',
      fileName: `${nameWithTs}.json`
    }
  }
  if (fmt === 'xml') {
    return { content: buildXml({ data, columns }), mimeType: 'application/xml', fileName: `${nameWithTs}.xml` }
  }
  if (fmt === 'csv') {
    return {
      content: `\ufeff${buildCsv({ data, columns, delimiter: ',' })}`,
      mimeType: 'text/csv',
      fileName: `${nameWithTs}.csv`
    }
  }
  if (fmt === 'txt') {
    return { content: `\ufeff${buildTxt({ data, columns })}`, mimeType: 'text/plain', fileName: `${nameWithTs}.txt` }
  }
  if (fmt === 'word') {
    return {
      content: buildHtmlTable({ data, columns, title: safeName }),
      mimeType: 'application/msword',
      fileName: `${nameWithTs}.doc`
    }
  }
  if (fmt === 'excel') {
    return {
      content: buildHtmlTable({ data, columns, title: safeName }),
      mimeType: 'application/vnd.ms-excel',
      fileName: `${nameWithTs}.xls`
    }
  }

  return null
}

export const downloadExportFile = ({ content, mimeType, fileName }) => {
  const blob = new Blob([content], { type: `${mimeType};charset=utf-8` })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}
