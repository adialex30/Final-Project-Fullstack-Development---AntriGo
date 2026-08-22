import React, { useEffect, useRef, useState, useCallback } from 'react'
import QRCode from 'qrcode'
import { formatIDR } from '../utils/currency'

const FALLBACK_DURATION_SECONDS = 15 * 60
const WARNING_THRESHOLD_SECONDS = 120

function formatCountdown(totalSeconds) {
  const s = Math.max(0, totalSeconds)
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}

function secondsUntil(isoTimestamp) {
  if (!isoTimestamp) return null
  const target = new Date(isoTimestamp).getTime()
  if (Number.isNaN(target)) return null
  return Math.floor((target - Date.now()) / 1000)
}

function isQrImageUrl(payload) {
  return typeof payload === 'string' && /^(https?:\/\/|data:image\/)/i.test(payload)
}

export default function QrisPaymentModal({ open, orderNumber, amount, qrPayload, expiresAt, onClose }) {
  const canvasRef = useRef(null)
  const isImage = isQrImageUrl(qrPayload)

  const [effectiveDeadline, setEffectiveDeadline] = useState(null)

  useEffect(() => {
    if (!open) return
    const remaining = secondsUntil(expiresAt)
    if (remaining === null || remaining <= 0) {
      if (expiresAt) {
        console.warn('[QRIS] paymentExpiresAt tidak valid / sudah lewat saat modal dibuka, pakai fallback durasi default:', expiresAt)
      }
      setEffectiveDeadline(Date.now() + FALLBACK_DURATION_SECONDS * 1000)
    } else {
      setEffectiveDeadline(Date.now() + remaining * 1000)
    }
  }, [open, expiresAt])

  const [secondsLeft, setSecondsLeft] = useState(FALLBACK_DURATION_SECONDS)

  useEffect(() => {
    if (!open || !effectiveDeadline) return
    const tick = () => setSecondsLeft(Math.max(0, Math.floor((effectiveDeadline - Date.now()) / 1000)))
    tick()
    const timer = setInterval(tick, 1000)
    return () => clearInterval(timer)
  }, [open, effectiveDeadline])

  const expired = effectiveDeadline !== null && secondsLeft <= 0
  const warning = !expired && secondsLeft <= WARNING_THRESHOLD_SECONDS
  const progress = effectiveDeadline
      ? Math.max(0, Math.min(1, secondsLeft / FALLBACK_DURATION_SECONDS))
      : 1

  const drawQr = useCallback((text) => {
    if (!canvasRef.current || !text) return
    QRCode.toCanvas(canvasRef.current, text, { width: 260, margin: 1, color: { dark: '#28241f', light: '#ffffff' } }).catch(() => {})
  }, [])

  useEffect(() => {
    if (!open || !qrPayload || isImage) return
    drawQr(qrPayload)
  }, [open, qrPayload, isImage, drawQr])

  if (!open) return null

  const handleDownload = () => {
    const link = document.createElement('a')
    if (isImage) {
      link.download = `QRIS-AntriGo-${orderNumber || 'sesi'}.png`
      link.href = qrPayload
      link.click()
      return
    }
    const canvas = canvasRef.current
    if (!canvas) return
    link.download = `QRIS-AntriGo-${orderNumber || 'sesi'}.jpg`
    link.href = canvas.toDataURL('image/jpeg', 0.95)
    link.click()
  }

  return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink-900/60 px-4">
        <div className="w-full max-w-sm rounded-2xl bg-white p-5 shadow-lg">
          <div className="flex items-center justify-between">
            <h2 className="font-display text-lg font-semibold text-ink-800">Scan QRIS</h2>
            <button onClick={onClose} aria-label="Tutup" className="rounded-full p-1 text-ink-400 hover:bg-ink-50 hover:text-ink-600">✕</button>
          </div>

          <p className="mt-1 text-sm text-ink-500">
            Scan dengan aplikasi e-wallet atau m-banking, lalu tunggu — halaman ini otomatis lanjut.
          </p>

          <div
              className={`mt-4 flex items-center justify-between rounded-xl border px-4 py-2.5 ${
                  expired
                      ? 'border-chili-200 bg-chili-50'
                      : warning
                          ? 'border-amber-200 bg-amber-50'
                          : 'border-sprout-200 bg-sprout-50'
              }`}
          >
          <span className={`text-sm font-medium ${expired ? 'text-chili-600' : warning ? 'text-amber-700' : 'text-sprout-700'}`}>
            {expired ? 'Waktu pembayaran habis' : 'Selesaikan dalam'}
          </span>
            {!expired && (
                <span className={`font-display text-lg font-bold tabular-nums ${warning ? 'text-amber-700' : 'text-sprout-700'}`}>
              {formatCountdown(secondsLeft)}
            </span>
            )}
          </div>

          <div className="mt-4 flex flex-col items-center rounded-xl border border-ink-100 bg-ink-50 p-4">
            <div className="relative">
              <div className={`transition-opacity duration-500 ${expired ? 'opacity-25' : 'opacity-100'}`}>
                {isImage ? (
                    <img src={qrPayload} alt="QRIS" width={260} height={260} className="rounded-lg bg-white" />
                ) : (
                    <canvas ref={canvasRef} className="rounded-lg" />
                )}
              </div>
              {expired && (
                  <div className="absolute inset-0 flex flex-col items-center justify-center gap-1 text-center">
                    <p className="rounded-lg bg-white/90 px-3 py-1.5 text-sm font-semibold text-chili-500 shadow-sm">
                      Sesi kedaluwarsa
                    </p>
                  </div>
              )}
            </div>

            {!expired && (
                <div className="mt-3 h-1 w-full max-w-[260px] overflow-hidden rounded-full bg-ink-200">
                  <div
                      className={`h-full rounded-full transition-all duration-1000 ease-linear ${
                          warning ? 'bg-amber-500' : 'bg-sprout-500'
                      }`}
                      style={{ width: `${progress * 100}%` }}
                  />
                </div>
            )}

            <p className="mt-3 font-display text-2xl font-bold text-ink-800">{formatIDR(amount)}</p>
            {orderNumber && <p className="mt-1 text-xs text-ink-400">{orderNumber}</p>}

            {expired && (
                <p className="mt-2 text-xs text-ink-500">
                  Pesanan ini otomatis dibatalkan. Silakan ulangi checkout.
                </p>
            )}
          </div>

          <button onClick={handleDownload} disabled={expired} className="btn-secondary mt-3 w-full py-2 text-sm disabled:cursor-not-allowed disabled:opacity-50">
            {isImage ? 'Unduh QR (PNG)' : 'Unduh QR (JPG)'}
          </button>

          {expired ? (
              <button onClick={onClose} className="btn-primary mt-2 w-full py-2.5">Tutup &amp; Pesan Ulang</button>
          ) : (
              <button onClick={onClose} className="mt-2 w-full py-1.5 text-center text-sm text-ink-400 hover:text-ink-600">
                Batalkan
              </button>
          )}

          <p className="mt-3 text-center text-[11px] text-ink-300">Powered by Midtrans QRIS (Sandbox)</p>
        </div>
      </div>
  )
}