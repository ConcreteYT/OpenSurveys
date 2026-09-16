import { flushSync } from 'react-dom';

let wipeBusy = false;

function getOrigin(event) {
  const target = event?.currentTarget;
  if (target?.getBoundingClientRect) {
    const rect = target.getBoundingClientRect();
    return {
      x: rect.left + rect.width / 2,
      y: rect.top + rect.height / 2,
    };
  }

  return {
    x: event?.clientX ?? window.innerWidth / 2,
    y: event?.clientY ?? window.innerHeight / 2,
  };
}

/**
 * Circular reveal of the fully themed UI from the clicked control.
 * Uses the View Transitions API when available (with blur + slower timing).
 */
export function runThemeWipe({ event, onApply }) {
  if (typeof document === 'undefined') {
    onApply?.();
    return;
  }

  if (wipeBusy) return;
  wipeBusy = true;

  const { x, y } = getOrigin(event);
  // Mask feathers over 48px to transparent at wipe-r, so the final radius must
  // reach past the farthest corner or that corner stays faded until the VT ends.
  const FEATHER_PX = 48;
  const BUFFER_PX = 24;
  const maxRadius =
    Math.hypot(
      Math.max(x, window.innerWidth - x),
      Math.max(y, window.innerHeight - y)
    ) +
    FEATHER_PX +
    BUFFER_PX;

  const root = document.documentElement;
  root.style.setProperty('--theme-wipe-x', `${x}px`);
  root.style.setProperty('--theme-wipe-y', `${y}px`);
  root.style.setProperty('--theme-wipe-r', `${Math.ceil(maxRadius)}px`);

  const applyTheme = () => {
    flushSync(() => {
      onApply?.();
    });
  };

  const finish = () => {
    wipeBusy = false;
  };
  if (typeof document.startViewTransition !== 'function') {
    applyTheme();
    finish();
    return;
  }

  const transition = document.startViewTransition(applyTheme);
  transition.finished.finally(finish);
}
