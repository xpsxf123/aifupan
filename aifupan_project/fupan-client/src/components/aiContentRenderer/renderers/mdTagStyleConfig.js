/**
 * @file aifupan 主题与皮肤枚举配置。
 * @description 定义 mdTag 模式下可用的主题、皮肤及其说明，供解析器和提示词文档复用。
 */

const AIFUPAN_THEME_MAP = {
    AURORA: 'aurora',
    BUSINESS: 'business',
    WARM: 'warm'
};

const AIFUPAN_SKIN_MAP = {
    GLASS: 'glass',
    PAPER: 'paper',
    MINIMAL: 'minimal'
};

const AIFUPAN_THEME_ENUMS = [
    {
        value: AIFUPAN_THEME_MAP.AURORA,
        label: '极光主题',
        description: '默认科技渐变风格，适合通用说明、产品介绍、视觉增强内容。'
    },
    {
        value: AIFUPAN_THEME_MAP.BUSINESS,
        label: '商务主题',
        description: '偏商务蓝与正式报告风，适合复盘、经营分析、周报月报、方案汇报。'
    },
    {
        value: AIFUPAN_THEME_MAP.WARM,
        label: '暖色主题',
        description: '偏暖色叙事风格，适合活动总结、海报文案、故事化内容与亮点展示。'
    }
];

const AIFUPAN_SKIN_ENUMS = [
    {
        value: AIFUPAN_SKIN_MAP.GLASS,
        label: '玻璃皮肤',
        description: '具有玻璃拟态和现代卡片感，适合科技风、活动页、视觉展示型内容。'
    },
    {
        value: AIFUPAN_SKIN_MAP.PAPER,
        label: '纸张皮肤',
        description: '接近 Word/PDF 报告质感，适合正式文档、总结材料、归档型输出。'
    },
    {
        value: AIFUPAN_SKIN_MAP.MINIMAL,
        label: '极简皮肤',
        description: '弱化装饰与阴影，适合规范文档、说明文、接口文档、技术清单。'
    }
];

export {
    AIFUPAN_THEME_MAP,
    AIFUPAN_SKIN_MAP,
    AIFUPAN_THEME_ENUMS,
    AIFUPAN_SKIN_ENUMS
};
