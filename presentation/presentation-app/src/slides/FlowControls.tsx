interface Props {
  currentStep: number
  stepCount: number
  playing: boolean
  finished: boolean
  onPrev: () => void
  onNext: () => void
  onPause: () => void
  onResume: () => void
  onReset: () => void
}

export function FlowControls({ currentStep, stepCount, playing, finished, onPrev, onNext, onPause, onResume, onReset }: Props) {
  return (
    <div className="flow-controls">
      <button
        className="flow-ctrl-btn"
        onClick={onPrev}
        disabled={currentStep <= 0}
        title="Previous step"
      >
        ←
      </button>

      <button
        className="flow-ctrl-btn flow-ctrl-play"
        onClick={playing ? onPause : onResume}
        title={playing ? 'Pause' : 'Play'}
      >
        {playing ? '⏸' : '▶'}
      </button>

      <button
        className="flow-ctrl-btn"
        onClick={onReset}
        title="Reset to beginning"
      >
        ⟳
      </button>

      <button
        className="flow-ctrl-btn"
        onClick={onNext}
        disabled={currentStep >= stepCount - 1}
        title="Next step"
      >
        →
      </button>
    </div>
  )
}
