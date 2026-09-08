export function trackEvent(code) {
    if (!code || typeof window === 'undefined') {
        return;
    }
    const tracker = window.LA;
    if (tracker && typeof tracker.track === 'function') {
        tracker.track(code);
    }
}
