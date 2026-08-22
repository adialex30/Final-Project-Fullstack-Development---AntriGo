import React, { useEffect, useRef, useState, useCallback } from 'react'
import QRCode from 'qrcode'
import { formatIDR } from '../utils/currency'

function formatCountdown(totalSeconds) {
  const s = Math.max(0, totalSeconds)
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}

function secondsUntil(isoTimestamp) {
  if (!isoTimestamp) return 0
  return Math.floor((new Date(isoTimestamp).getTime() - Date.now()) / 1000)
}

function isQrImageUrl(payload) {
  return typeof payload === 'string' && /^(https?:\/\/|data:image\/)/i.test(payload)
}

export default function QrisPaymentModal({ open, orderNumber, amount, qrPayload, expiresAt, onClose }) {
  const canvasRef = useRef(null)
  const [secondsLeft, setSecondsLeft] = useState(() => secondsUntil(expiresAt))
  const expired = secondsLeft <= 0
  const isImage = isQrImageUrl(qrPayload)

  const drawQr = useCallback((text) => {
    if (!canvasRef.current || !text) return
    QRCode.toCanvas(canvasRef.current, text, { width: 260, margin: 1, color: { dark: '#28241f', light: '#ffffff' } }).catch(() => {})
  }, [])

  useEffect(() => {
    if (!open || !qrPayload || isImage) return
    drawQr(qrPayload)
  }, [open, qrPayload, isImage, drawQr])

  useEffect(() => {
    if (!open) return
    setSecondsLeft(secondsUntil(expiresAt))
    const timer = setInterval(() => setSecondsLeft(secondsUntil(expiresAt)), 1000)
    return () => clearInterval(timer)
  }, [open, expiresAt])

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

          <div className="mt-4 flex flex-col items-center rounded-xl border border-ink-100 bg-ink-50 p-4">
            <div className={`relative ${expired ? 'opacity-30' : ''}`}>
              {isImage ? (
                  <img src={qrPayload} alt="QRIS" width={260} height={260} className="rounded-lg bg-white" />
              ) : (
                  <canvas ref={canvasRef} className="rounded-lg" />
              )}
            </div>
            {expired && (
                <div className="-mt-24 mb-4 flex flex-col items-center gap-2 px-4 text-center">
                  <p className="text-sm font-semibold text-chili-500">Sesi pembayaran kedaluwarsa</p>
                  <p className="text-xs text-ink-500">Pesanan ini otomatis dibatalkan. Silakan ulangi checkout.</p>
                </div>
            )}
            <p className="mt-3 font-display text-2xl font-bold text-ink-800">{formatIDR(amount)}</p>
            {orderNumber && <p className="mt-1 text-xs text-ink-400">{orderNumber}</p>}
            {!expired && <p className="mt-1 text-xs text-ink-500">Berlaku <span className="font-semibold text-ink-700">{formatCountdown(secondsLeft)}</span></p>}
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