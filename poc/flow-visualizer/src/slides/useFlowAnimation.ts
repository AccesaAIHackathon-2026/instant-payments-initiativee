import { useState, useEffect, useCallback, useRef } from 'react'

export function useFlowAnimation(stepCount: number, active: boolean, stepDuration = 3000) {
  const [currentStep, setCurrentStep] = useState(0)
  const [playing, setPlaying] = useState(false)
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const clearTimer = useCallback(() => {
    if (timerRef.current) { clearTimeout(timerRef.current); timerRef.current = null }
  }, [])

  const play = useCallback(() => {
    setCurrentStep(0)
    setPlaying(true)
  }, [])

  const pause = useCallback(() => {
    clearTimer()
    setPlaying(false)
  }, [clearTimer])

  const resume = useCallback(() => {
    setPlaying(true)
  }, [])

  const reset = useCallback(() => {
    clearTimer()
    setCurrentStep(0)
    setPlaying(false)
  }, [clearTimer])

  const prevStep = useCallback(() => {
    clearTimer()
    setPlaying(false)
    setCurrentStep(s => Math.max(0, s - 1))
  }, [clearTimer])

  const nextStep = useCallback(() => {
    clearTimer()
    setPlaying(false)
    setCurrentStep(s => Math.min(stepCount - 1, s + 1))
  }, [clearTimer, stepCount])

  const finished = currentStep >= stepCount - 1

  useEffect(() => {
    if (!playing) return
    if (currentStep >= stepCount - 1) {
      // Reached the end, stop auto-advancing
      timerRef.current = setTimeout(() => setPlaying(false), 2000)
      return () => clearTimer()
    }
    timerRef.current = setTimeout(() => {
      setCurrentStep(s => s + 1)
    }, stepDuration)
    return () => clearTimer()
  }, [playing, currentStep, stepCount, stepDuration, clearTimer])

  // Auto-play when slide becomes active
  useEffect(() => {
    if (active) {
      const t = setTimeout(play, 600)
      return () => clearTimeout(t)
    } else {
      reset()
    }
  }, [active, play, reset])

  return { currentStep, playing, finished, play, pause, resume, reset, prevStep, nextStep }
}
