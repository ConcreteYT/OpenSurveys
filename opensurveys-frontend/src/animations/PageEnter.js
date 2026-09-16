import React, { useEffect, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import { animate } from 'animejs';

const BLOCK_SELECTOR = '.card, .bg-glass, .page-stagger-block';
const ITEM_SELECTOR = 'h1, h2, h3, p, label, .btn, a.btn, .form-control, .code-input, .alert';

function reveal(els) {
  els.forEach((el) => {
    el.style.opacity = '1';
    el.style.transform = '';
  });
}

/**
 * Stagger-enter cards, then text/controls.
 * Animates elements one-by-one (no createScope.revert) so Strict Mode
 * cannot leave the page stuck at opacity 0.
 */
export function usePageEnterStagger(rootRef, depsKey) {
  useEffect(() => {
    const root = rootRef.current;
    if (!root) return undefined;

    const blocks = Array.from(root.querySelectorAll(BLOCK_SELECTOR));
    const items = Array.from(root.querySelectorAll(ITEM_SELECTOR));
    const all = [...new Set([...blocks, ...items])];
    const animations = [];
    let cancelled = false;
    let failsafeId = 0;

    const startId = window.requestAnimationFrame(() => {
      if (cancelled) return;

      all.forEach((el) => {
        el.style.opacity = '0';
        el.style.transform = 'translateY(18px)';
      });

      blocks.forEach((el, index) => {
        animations.push(
          animate(el, {
            opacity: 1,
            translateY: 0,
            delay: index * 110,
            duration: 480,
            ease: 'out(2)',
          })
        );
      });

      items.forEach((el, index) => {
        animations.push(
          animate(el, {
            opacity: 1,
            translateY: 0,
            delay: 140 + index * 45,
            duration: 400,
            ease: 'out(2)',
          })
        );
      });

      failsafeId = window.setTimeout(() => {
        if (!cancelled) reveal(all);
      }, 1400);
    });

    return () => {
      cancelled = true;
      window.cancelAnimationFrame(startId);
      window.clearTimeout(failsafeId);
      animations.forEach((anim) => anim?.pause?.());
      reveal(all);
    };
  }, [rootRef, depsKey]);
}

export default function PageEnter({ children }) {
  const rootRef = useRef(null);
  const { pathname } = useLocation();
  usePageEnterStagger(rootRef, pathname);

  return (
    <div ref={rootRef} className="page-enter-root">
      {children}
    </div>
  );
}
