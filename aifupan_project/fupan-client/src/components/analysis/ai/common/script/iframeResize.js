export const IFRAME_RESIZE_JS = `
  let lastHeight = 0;
  function postHeight() {
    const height = document.body.scrollHeight;
    if (Math.abs(height - lastHeight) < 2) return;
    lastHeight = height;
    window.parent.postMessage({ type: 'iframe-resize', height: height, load: false }, '*');
  }
  function init() {
    const target = document.body;
    if (!target) return;
    const ro = new ResizeObserver(postHeight);
    ro.observe(target);
    postHeight();
  }
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
`;

