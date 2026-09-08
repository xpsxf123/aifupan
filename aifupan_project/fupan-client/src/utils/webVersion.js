import {AUTO_WEB_VERSION, MANUAL_WEB_VERSION} from '@/constants/webVersion';

const matchNumberVersion = (value = '') => {
    const text = String(value ?? '');
    const m = text.match(/(\d+(?:\.\d+)*)/);
    return m?.[1] || '';
};

const normalizeManualVersion = (value = '') => {
    const raw = matchNumberVersion(value);
    if (!raw) {
        return '';
    }
    const cleaned = raw
        .replace(/[^0-9.]/g, '')
        .replace(/\.{2,}/g, '.')
        .replace(/^\./, '')
        .replace(/\.$/, '');
    return cleaned;
};

const pad2 = (value) => String(value ?? '').padStart(2, '0');

const toComparableNumber = (version = '') => {
    const v = normalizeManualVersion(version);
    if (!v) {
        return 0;
    }
    const parts = v.split('.').filter(Boolean);
    const major = parts[0] ?? '0';
    const minor = parts[1] ?? '0';
    const patch = parts[2] ?? '0';
    const base = `${Number(major) || 0}${pad2(minor)}${pad2(patch)}`;
    const decimalDigits = parts.slice(3).join('');
    const resultText = decimalDigits ? `${base}.${decimalDigits}` : base;
    const result = Number(resultText);
    return Number.isFinite(result) ? result : 0;
};

const resolveManualWebVersion = () => normalizeManualVersion(MANUAL_WEB_VERSION);

const resolveAutoWebVersion = () => normalizeManualVersion(AUTO_WEB_VERSION);

const resolveWebVersion = () => {
    const autoVersion = resolveAutoWebVersion();
    const manualVersion = resolveManualWebVersion();
    if (!manualVersion) {
        return autoVersion;
    }
    if (!autoVersion) {
        return manualVersion;
    }
    const autoNum = toComparableNumber(autoVersion);
    const manualNum = toComparableNumber(manualVersion);
    if (autoNum >= manualNum) {
        return autoVersion;
    }
    return manualVersion;
};

export {
    normalizeManualVersion,
    toComparableNumber,
    resolveAutoWebVersion,
    resolveManualWebVersion,
    resolveWebVersion
};

export default resolveWebVersion;
