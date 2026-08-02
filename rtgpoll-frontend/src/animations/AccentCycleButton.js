import React, { useEffect, useRef } from 'react';
import { animate } from 'animejs';

export const ACCENT_COLORS = ['purple', 'red', 'blue', 'green', 'rainbow', 'mono'];

export const ACCENT_SWATCH = {
  purple: '#ad1fff',
  red: '#ff4d4d',
  blue: '#1f8fff',
  green: '#2ecc71',
  rainbow: '#ff2d55',
  mono: '#111111',
};

function accentLabel(color) {
  return color.charAt(0).toUpperCase() + color.slice(1);
}

/**
 * Colored swatch that pulses when the accent changes.
 */
export function AccentCycleIcon({ accent }) {
  const iconRef = useRef(null);
  const isFirstRender = useRef(true);

  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }

    const el = iconRef.current;
    if (!el) return;

    animate(el, {
      scale: [0.7, 1],
      duration: 280,
      ease: 'out(2)',
    });
  }, [accent]);

  return (
    <span
      ref={iconRef}
      className={[
        'accent-cycle-icon',
        accent === 'rainbow' ? 'accent-cycle-icon--rainbow' : '',
        accent === 'mono' ? 'accent-cycle-icon--mono' : '',
      ]
        .filter(Boolean)
        .join(' ')}
      style={{
        display: 'inline-block',
        width: 14,
        height: 14,
        borderRadius: '50%',
        background: ACCENT_SWATCH[accent] || ACCENT_SWATCH.purple,
        boxShadow: '0 0 0 2px rgba(0,0,0,0.12)',
        verticalAlign: 'middle',
      }}
      aria-hidden="true"
    />
  );
}

/** Accent color choices for account-menu dropdowns. */
export function AccentMenuItems({ accent, onSetAccent }) {
  return ACCENT_COLORS.map((color) => (
    <li key={color}>
      <button
        type="button"
        className={`dropdown-item${accent === color ? ' active' : ''}`}
        onClick={(event) => {
          if (color === accent) return;
          onSetAccent?.(color, event);
        }}
      >
        <AccentCycleIcon accent={color} />{' '}
        {accentLabel(color)}
      </button>
    </li>
  ));
}

/**
 * Accent picker dropdown: current swatch trigger + color list.
 */
export default function AccentCycleButton({
  accent,
  onSetAccent,
  className = 'btn btn-sm btn-outline-secondary',
  style,
}) {
  const detailsRef = useRef(null);
  const label = `Accent color: ${accentLabel(accent)}. Open to choose.`;

  const handleSelect = (color, event) => {
    if (detailsRef.current) {
      detailsRef.current.open = false;
    }
    if (color === accent) return;
    onSetAccent?.(color, event);
  };

  return (
    <details ref={detailsRef} className="dropdown" style={{ position: 'relative' }}>
      <summary
        className={className}
        aria-label={label}
        title={label}
        style={{
          listStyle: 'none',
          cursor: 'pointer',
          width: '40px',
          height: '40px',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          ...style,
        }}
      >
        <AccentCycleIcon accent={accent} />
      </summary>
      <ul
        className="dropdown-menu"
        style={{
          display: 'block',
          minWidth: '8rem',
          position: 'absolute',
          right: 0,
          left: 'auto',
          zIndex: 1050,
        }}
      >
        {ACCENT_COLORS.map((color) => (
          <li key={color}>
            <button
              type="button"
              className={`dropdown-item${accent === color ? ' active' : ''}`}
              onClick={(event) => handleSelect(color, event)}
            >
              <AccentCycleIcon accent={color} />{' '}
              {accentLabel(color)}
            </button>
          </li>
        ))}
      </ul>
    </details>
  );
}
