import React, { useEffect, useRef } from 'react';
import { animate } from 'animejs';

/**
 * Sun/moon glyph that flips when the theme changes.
 */
export function ThemeToggleIcon({ isDarkMode }) {
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
      rotateY: [90, 0],
      duration: 320,
      ease: 'out(2)',
    });
  }, [isDarkMode]);

  return (
    <span
      ref={iconRef}
      className="theme-toggle-icon"
      style={{ display: 'inline-block', transformOrigin: 'center' }}
      aria-hidden="true"
    >
      {isDarkMode ? '☀️' : '🌙'}
    </span>
  );
}

/**
 * Icon-only theme toggle button with flip animation.
 */
export default function ThemeToggleButton({
  isDarkMode,
  onToggleDarkMode,
  className = 'btn btn-sm btn-outline-secondary',
  style,
}) {
  return (
    <button
      type="button"
      className={className}
      onClick={onToggleDarkMode}
      aria-label="Toggle dark mode"
      style={{ width: '40px', height: '40px', perspective: '600px', ...style }}
    >
      <ThemeToggleIcon isDarkMode={isDarkMode} />
    </button>
  );
}
