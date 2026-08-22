import React from 'react'

export default class ErrorBoundary extends React.Component {
    state = { hasError: false }

    static getDerivedStateFromError() {
        return { hasError: true }
    }

    componentDidCatch(error, info) {
        console.error('App crashed:', error, info)
    }

    render() {
        if (this.state.hasError) {
            return (
                <div className="flex min-h-screen flex-col items-center justify-center gap-3 p-6 text-center">
                    <p className="font-display text-lg font-semibold">Terjadi kesalahan</p>
                    <p className="text-sm text-ink-400">Coba muat ulang halaman.</p>
                    <button
                        onClick={() => window.location.reload()}
                        className="rounded-full bg-ink-800 px-4 py-2 text-sm text-white"
                    >
                        Muat Ulang
                    </button>
                </div>
            )
        }
        return this.props.children
    }
}