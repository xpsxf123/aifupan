/*
    参考案例:
    const ANALYSIS_TYPE = {
        A: 'a',
        B: 'b',
        C: 'c',
    }
    const ANALYSIS_LABEL = {
        [ANALYSIS_TYPE.A]: '段落',
        [ANALYSIS_TYPE.B]: '句子',
        [ANALYSIS_TYPE.C]: '单词',
    }
*/

// 添加主播，平台类型枚举, 0:抖音, 1:快手, 2:视频号
export const PLATFORM_ENUM = {
    kuaishou: 1,
    douyin: 0,
    shipinhao: 2
}
export const PLATFORM_LABEL = {
    [PLATFORM_ENUM.kuaishou]: '快手',
    [PLATFORM_ENUM.douyin]: '抖音',
    [PLATFORM_ENUM.shipinhao]: '视频号',
}

//平台类型 0：全平台 1：抖音 2：快手 3：视频号 4：小红书
export const PLATFORM_TYPE_ENUM = {
    all: '0',
    douyin: '1',
    kuaishou: '2',
    shipinhao: "3",
    xiaohongshu: "4"
}

export const PLATFORM_TYPE_LABEL = {
    [PLATFORM_TYPE_ENUM.all]: '全平台',
    [PLATFORM_TYPE_ENUM.douyin]: '抖音',
    [PLATFORM_TYPE_ENUM.kuaishou]: '快手',
    [PLATFORM_TYPE_ENUM.shipinhao]: '视频号',
    [PLATFORM_TYPE_ENUM.xiaohongshu]: '小红书',
}

// 版本类型枚举
export const VERSION_TYPE = {
	PURE: 'PURE',
	AGENT: 'AGENT',
}
export const DEFINITION_LIST = [{label: '标清', value: -1, color: '#6B7F06'}, {
	label: '标清',
	value: 0,
	color: '#6B7F06'
}, {label: '高清', value: 1, color: '#C25705'}, {label: '超清', value: 2, color: '#047FB0'}, {
	label: '蓝光',
	value: 3,
	color: '#343ECC'
}]