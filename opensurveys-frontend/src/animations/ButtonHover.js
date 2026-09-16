import { useEffect } from 'react';
import { animate } from 'animejs';

const SELECTOR = 'button, .btn, [role="button"]';

const HOVER = { scale: 1.04, y: -2 };
const REST = { scale: 1, y: 0 };
const PRESS = { scale: 0.98, y: 0 };
const DURATION = 180;

function resolveTarget(node) {
  if (!node || typeof node.closest !== 'function') return null;
  const el = node.closest(SELECTOR);
  if (!el) return null;
  if (el.matches(':disabled') || el.hasAttribute('disabled')) return null;
  if (el.getAttribute('aria-disabled') === 'true') return null;
  if (el.dataset.noHoverAnim != null) return null;
  return el;
}

function isLeavingTarget(el, related) {
  return related instanceof Node && el.contains(related);
}

/**
 * App-wide button hover/press via event delegation (anime.js scale + lift).
 * Opt out with data-no-hover-anim on an element.
 */
export default function ButtonHover() {
  useEffect(() => {
    const anims = new WeakMap();

    const play = (el, to) => {
      anims.get(el)?.pause();
      el.style.willChange = 'transform';
      el.style.transformOrigin = 'center';
      anims.set(
        el,
        animate(el, {
          scale: to.scale,
          translateY: to.y,
          duration: DURATION,
          ease: 'out(2)',
        })
      );
    };

    const onOver = (e) => {
      const el = resolveTarget(e.target);
      if (!el || isLeavingTarget(el, e.relatedTarget)) return;
      play(el, HOVER);
    };

    const onOut = (e) => {
      const el = resolveTarget(e.target);
      if (!el || isLeavingTarget(el, e.relatedTarget)) return;
      play(el, REST);
    };

    const onDown = (e) => {
      const el = resolveTarget(e.target);
      if (!el) return;
      play(el, PRESS);
    };

    const onUp = (e) => {
      const el = resolveTarget(e.target);
      if (!el) return;
      play(el, HOVER);
    };

    document.addEventListener('pointerover', onOver, true);
    document.addEventListener('pointerout', onOut, true);
    document.addEventListener('pointerdown', onDown, true);
    document.addEventListener('pointerup', onUp, true);

    return () => {
      document.removeEventListener('pointerover', onOver, true);
      document.removeEventListener('pointerout', onOut, true);
      document.removeEventListener('pointerdown', onDown, true);
      document.removeEventListener('pointerup', onUp, true);
    };
  }, []);

  return null;
}
