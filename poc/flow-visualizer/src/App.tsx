import { useState, useEffect, useCallback } from 'react'
import { SolutionOverview } from './slides/SolutionOverview'
import { CompetitiveAdvantage } from './slides/CompetitiveAdvantage'
import { SameBankFlow } from './slides/SameBankFlow'
import { CrossBankFlow } from './slides/CrossBankFlow'
import { OfflineNFCFlow } from './slides/OfflineNFCFlow'
import { QRCodeSlide } from './slides/QRCodeSlide'
import { NextSteps } from './slides/NextSteps'

const slides = [
  { title: 'Solution Overview', component: SolutionOverview },
  { title: 'Competitive Advantage', component: CompetitiveAdvantage },
  { title: 'Demo — Same-Bank Payment', component: SameBankFlow },
  { title: 'Demo — Cross-Bank Payment', component: CrossBankFlow },
  { title: 'Demo — Offline NFC Transfer', component: OfflineNFCFlow },
  { title: 'Try It Yourself', component: QRCodeSlide },
  { title: 'Next Steps — Accelerator', component: NextSteps },
]

export default function App() {
  const [current, setCurrent] = useState(0)

  const prev = useCallback(() => setCurrent(i => Math.max(0, i - 1)), [])
  const next = useCallback(() => setCurrent(i => Math.min(slides.length - 1, i + 1)), [])
  const goTo = useCallback((i: number) => setCurrent(i), [])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'ArrowRight' || e.key === ' ') { e.preventDefault(); next() }
      if (e.key === 'ArrowLeft') { e.preventDefault(); prev() }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [next, prev])

  const Slide = slides[current].component

  return (
    <div className="carousel">
      <div className="carousel-viewport">
        <div
          className="carousel-track"
          style={{ transform: `translateX(-${current * 100}vw)` }}
        >
          {slides.map((s, i) => (
            <div className="carousel-slide" key={i}>
              {Math.abs(i - current) <= 1 ? <s.component active={i === current} /> : null}
            </div>
          ))}
        </div>
      </div>

      <div className="carousel-controls">
        <div className="slide-title">
          <img src="/img/BlinkPay-logo.png" alt="BlinkPay" className="brand-logo" />
          {slides[current].title}
        </div>

        <div className="carousel-dots">
          {slides.map((_, i) => (
            <button
              key={i}
              className={`carousel-dot ${i === current ? 'active' : ''}`}
              onClick={() => goTo(i)}
            />
          ))}
        </div>

        <div className="carousel-nav">
          <button className="carousel-btn" onClick={prev} disabled={current === 0}>
            ← Prev
          </button>
          <span className="slide-counter">{current + 1} / {slides.length}</span>
          <button className="carousel-btn" onClick={next} disabled={current === slides.length - 1}>
            Next →
          </button>
        </div>
      </div>

      <div className="keyboard-hint">← → to navigate</div>
    </div>
  )
}
