import React, { useEffect, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { animate } from 'animejs';

const DESKTOP = {
  shape1: { top: '-6%', left: '-5%', width: 280, height: 280 },
  shape2: { top: '62%', left: '72%', width: 340, height: 340 },
};

const MOBILE = {
  shape1: { top: '-4%', left: '-8%', width: 150, height: 150 },
  shape2: { top: '70%', left: '68%', width: 160, height: 160 },
};

/** Route → absolute positions (viewport-relative) for the persistent blobs */
const LAYOUTS = {
  '/home': true,
  '/auth/login': true,
  '/auth/signup': true,
  '/access': true,
  '/login-home': true,
  '/surveys': true,
  '/editor': true,
  '/admin': true,
};

function useIsMobile(breakpoint = 768) {
  const [isMobile, setIsMobile] = useState(
    () => typeof window !== 'undefined' && window.matchMedia(`(max-width: ${breakpoint - 0.02}px)`).matches
  );

  useEffect(() => {
    const mq = window.matchMedia(`(max-width: ${breakpoint - 0.02}px)`);
    const onChange = () => setIsMobile(mq.matches);
    onChange();
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, [breakpoint]);

  return isMobile;
}

function layoutForPath(pathname, isMobile) {
  const entry = LAYOUTS[pathname];
  if (entry && entry.desktop) {
    return isMobile ? entry.mobile : entry.desktop;
  }
  return isMobile ? MOBILE : DESKTOP;
}

function applyLayout(el, pos) {
  if (!el) return;
  el.style.top = pos.top;
  el.style.left = pos.left;
  el.style.width = `${pos.width}px`;
  el.style.height = `${pos.height}px`;
}

/**
 * App-level blobs: morph/spin via CSS (seamless infinite loops).
 * Anime only tweens outer shell position on route change.
 */
export default function PersistentBlobs() {
  const location = useLocation();
  const isMobile = useIsMobile();
  const shell1Ref = useRef(null);
  const shell2Ref = useRef(null);
  const hasMountedRef = useRef(false);

  useEffect(() => {
    const initial = layoutForPath(location.pathname, isMobile);
    applyLayout(shell1Ref.current, initial.shape1);
    applyLayout(shell2Ref.current, initial.shape2);
    hasMountedRef.current = true;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!hasMountedRef.current) return;

    const layout = layoutForPath(location.pathname, isMobile);
    const tween = { duration: 750, ease: 'inOut(2)' };

    animate(shell1Ref.current, {
      top: layout.shape1.top,
      left: layout.shape1.left,
      width: layout.shape1.width,
      height: layout.shape1.height,
      ...tween,
    });

    animate(shell2Ref.current, {
      top: layout.shape2.top,
      left: layout.shape2.left,
      width: layout.shape2.width,
      height: layout.shape2.height,
      ...tween,
    });
  }, [location.pathname, isMobile]);

  return (
    <div className="persistent-blobs" aria-hidden="true">
      <div ref={shell1Ref} className="persistent-blob-shell">
        <div className="persistent-blob persistent-blob-1 shadow-5-strong" />
      </div>
      <div ref={shell2Ref} className="persistent-blob-shell">
        <div className="persistent-blob persistent-blob-2 shadow-5-strong" />
      </div>
    </div>
  );
}
