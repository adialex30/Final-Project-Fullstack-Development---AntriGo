import React from 'react'

const STATUS_MAP = {
  QUEUED: { label: 'Diantre', className: 'bg-ember-100 text-ember-800' },
  PROCESSING: { label: 'Diproses', className: 'bg-blue-100 text-blue-700' },
  READY: { label: 'Siap Diambil', className: 'bg-sprout-500/15 text-sprout-600' },
  COMPLETED: { label: 'Selesai', className: 'bg-ink-100 text-ink-600' },
  CANCELLED: { label: 'Dibatalkan', className: 'bg-chili-500/10 text-chili-600' },
}

export default function StatusBadge({ status }) {
  const meta = STATUS_MAP[status] || { label: status, className: 'bg-ink-100 text-ink-600' }
  return (
    <span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ${meta.className}`}>
      {meta.label}
    </span>
  )
}
