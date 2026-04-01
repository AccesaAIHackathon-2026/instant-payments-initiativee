const steps = [
  'Productize as accelerator for EU tier-2/3 bank onboarding',
  'Integrate with ECB Digital Euro sandbox when available',
]

export function NextSteps({ active }: { active: boolean }) {
  return (
    <div className="slide-next">
      <h2>Next Steps &rarr; Accelerator</h2>
      <ul className="next-steps-list">
        {steps.map((text, i) => (
          <li className="next-step-item" key={i}>
            <span className="next-step-number">{i + 1}</span>
            <span className="next-step-text">{text}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
