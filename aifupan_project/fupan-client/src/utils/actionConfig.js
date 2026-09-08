const actionConfig = {//对应的诊断选项
    fileConfig: [{
        label: '账号阶段',
        value: 'accountStage',
        selectType: 'single',
        accountType: [0,1]
    }, {
        label: '账号水平',
        value: 'accountWaterLevel',
        selectType: 'single',
        accountType: [0,1]
    }, {
        label: '流量结构',
        value: 'accountFlow',
        selectType: 'single',
        accountType: [0,1]
    }],

    compereConfig: [{
        label: '直播目标',
        value: 'livingTarget',
        selectType: 'single',
        accountType: [0,1]
    }, {
        label: '营销组件',
        value: 'marketing',
        selectType: 'single',
        accountType: [0,1]
    }, {
        label: '优化方向（可多选）',
        value: 'optimizeDirection',
        selectType: 'multiple',
        accountType: [0,1]
    }],

    analysisConfig: [{
        label: '账号阶段',
        value: 'accountStage',
        selectType: 'single',
        accountType: [0,1]
    }, {
        label: '直播目标',
        value: 'livingTarget',
        selectType: 'single',
        accountType: [0,1]
    }, {
        label: '账号水平',
        value: 'accountWaterLevel',
        selectType: 'single',
        accountType: [0,1]
    }, {
        label: '流量结构',
        value: 'accountFlow',
        selectType: 'single',
        accountType: [0,1]
    }, {
        label: '直播形式',
        value: 'livingModality',
        selectType: 'single',
        accountType: [0,1]
    }, {
        label: '营销组件',
        value: 'marketing',
        selectType: 'single',
        accountType: [0,1]
    }, {
        label: '优化方向（可多选）',
        value: 'optimizeDirection',
        selectType: 'multiple',
        accountType: [0]
    }, {
        label: '学习方向（可多选）',
        value: 'learning',
        selectType: 'multiple',
        accountType: [1]
    }],
}

const ENUM_OBJ = {
    accountStage: 'account_stage',
    accountWaterLevel: 'account_water_level',
    accountFlow: 'account_flow',
    engSerViceType: 'eng_ser_vice_type',
    livingTarget: 'living_target',
    livingModality: 'living_modality',
    marketing: 'marketing',
    optimizeDirection: 'optimize_direction',
    learning: 'learning'
}

export {actionConfig, ENUM_OBJ}
export default {
    actionConfig,
    ENUM_OBJ
}