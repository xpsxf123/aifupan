<template>
    <div class="more-config-panel" :style="panelStyle">
        <div v-if="showTipBox" class="more-config-tip-box">
            <div class="more-config-tip-item">1、输入参数仅影响您自己的提问和自定义问题</div>
            <div class="more-config-tip-item">2、选择的输入参数越多，分析消耗的算力越高</div>
        </div>
        <div class="more-config-group">
            <div class="font-s14 text-colorMain pd-b10">基础数据 <span class="font-s12 text-color3 pd-l4">（核心分析数据，<span class="text-colorErr">至少选一个</span>）</span></div>
            <div class="more-config-grid">
                <el-checkbox
                    v-for="item in basicItems"
                    :key="item.prop"
                    class="more-config-card"
                    :class="{'is-checked': !!getBasicData[item.prop]}"
                    :true-label="1"
                    :false-label="0"
                    :value="getBasicData[item.prop]"
                    @change="(val)=>onBasicChange(item.prop, val)"
                >
                    <span class="more-config-card__label">
                        <span>{{item.label}}</span>
                        <el-tooltip
                            v-if="item.description"
                            effect="dark"
                            placement="top"
                            :content="item.description"
                        >
                            <i class="el-icon-question more-config-card__tip"></i>
                        </el-tooltip>
                    </span>
                </el-checkbox>
            </div>
        </div>
        <div class="more-config-group">
            <div class="font-s14 text-colorMain pd-b10">扩展配置<span class="font-s12 text-color3 pd-l4">（可勾选扩展提问参数增加分析准确度）</span></div>
            <div class="more-config-grid">
                <el-checkbox
                    v-for="item in dynamicConfigItems"
                    :key="item.prop"
                    class="more-config-card"
                    :class="{'is-checked': !!getDynamicConfigs[item.prop]}"
                    :true-label="1"
                    :false-label="0"
                    :value="getDynamicConfigs[item.prop]"
                    @change="(val)=>onDynamicChange(item.prop, val)"
                >
                    <span class="more-config-card__label">{{item.label}}</span>
                </el-checkbox>
            </div>
        </div>
    </div>
</template>

<script>
/**
 * @file 更多配置面板。
 * @description 负责渲染更多配置的三组勾选项，可被页面弹层与自建问题表单同时复用。
 */
import {
    getBasicItems,
    getDynamicConfigItems,
    ensureDynamicConfigItems,
    setDynamicConfigItems,
    normalizeMoreConfigValue
} from './moreConfigShared';

export default {
    props:{
        value: {
            type: Object,
            default: () => ({})
        },
        hide:{
            type:Object,
            default: () => ({})
        },
        sentenceMarkData:{
            type:Object,
            default: () => ({})
        },
        aiCueType:{
            type:String,
            default: ''
        },
        isCompare:{
            type: Boolean,
            default: false
        },
        panelMaxHeight: {
            type: [String, Number],
            default: 520
        },
        showTipBox: {
            type: Boolean,
            default: true
        }
    },
    data() {
        return {
            localValue: {
                basicData: {},
                extraConditions: {},
                dynamicConfigs: {}
            },
            basicItems: getBasicItems(),
            dynamicConfigItems: getDynamicConfigItems(),
        };
    },
    computed: {
        normalizeOptions(){
            return {
                sentenceMarkData: this.sentenceMarkData,
                hide: this.hide,
                aiCueType: this.aiCueType,
                isCompare: this.isCompare
            }
        },
        panelStyle(){
            if (this.panelMaxHeight === '' || this.panelMaxHeight === null || typeof this.panelMaxHeight === 'undefined') {
                return {
                    maxHeight: 'none',
                    overflow: 'visible'
                }
            }
            return {
                maxHeight: typeof this.panelMaxHeight === 'number' ? `${this.panelMaxHeight}px` : this.panelMaxHeight,
                overflowY: 'auto'
            }
        },
        getBasicData(){
            return this.localValue?.basicData || {}
        },
        getDynamicConfigs(){
            return this.localValue?.dynamicConfigs || {}
        }
    },
    watch: {
        value: {
            handler(val){
                this.syncLocalValue(val)
            },
            deep: true,
            immediate: true
        },
        normalizeOptions: {
            handler(){
                this.syncLocalValue(this.localValue)
            },
            deep: true
        }
    },
    methods: {
        async loadDynamicConfigItems() {
            const list = await ensureDynamicConfigItems(this.$httpBack, { force: true })
            if (!Array.isArray(list) || !list.length) {
                return
            }
            setDynamicConfigItems(list)
            this.basicItems = getBasicItems()
            this.dynamicConfigItems = getDynamicConfigItems()
            const currentValue = JSON.stringify(this.value || {})
            this.syncLocalValue(this.value || {})
            const nextValue = JSON.stringify(this.localValue || {})
            if (currentValue !== nextValue) {
                this.emitValue(this.localValue)
            }
        },
        /**
         * @description 同步本地配置，保证结构完整且不丢默认值。
         * @param {Object} value 外部配置
         * @returns {void}
         */
        syncLocalValue(value = {}){
            const nextValue = normalizeMoreConfigValue(value, this.normalizeOptions)
            const currentValue = JSON.stringify(this.localValue || {})
            const compareValue = JSON.stringify(nextValue)
            if (currentValue === compareValue) {
                return
            }
            this.localValue = nextValue
        },
        /**
         * @description 向外同步更多配置值。
         * @param {Object} nextValue 下一份配置
         * @returns {void}
         */
        emitValue(nextValue){
            const normalizedValue = normalizeMoreConfigValue(nextValue, this.normalizeOptions)
            this.localValue = normalizedValue
            this.$emit('input', normalizedValue)
            this.$emit('change', normalizedValue)
        },
        /**
         * @description 校验基础数据三项至少保留一个，避免提交空核心分析内容。
         * @param {Object} basicData 基础数据勾选结果
         * @returns {boolean}
         */
        validateBasicData(basicData = {}){
            if (!Array.isArray(this.basicItems) || !this.basicItems.length) {
                return true
            }
            const hasChecked = Object.values(basicData || {}).some(item => !!item)
            if (hasChecked) {
                return true
            }
            this.$message.warning('如果都不选择将没有分析的核心内容，请勾选其中一个。')
            return false
        },
        /**
         * @description 处理基础数据变更。
         * @param {string} prop 字段名
         * @param {number} value 勾选值
         * @returns {void}
         */
        onBasicChange(prop, value){
            const nextBasicData = {
                ...this.getBasicData,
                [prop]: value
            }
            if (!this.validateBasicData(nextBasicData)) {
                return
            }
            this.emitValue({
                ...this.localValue,
                basicData: nextBasicData
            })
        },
        /**
         * @description 处理扩展配置变更。
         * @param {string} prop 字段名
         * @param {number} value 勾选值
         * @returns {void}
         */
        onDynamicChange(prop, value){
            this.emitValue({
                ...this.localValue,
                dynamicConfigs: {
                    ...this.getDynamicConfigs,
                    [prop]: value
                }
            })
        }
    },
    mounted() {
        this.loadDynamicConfigItems()
    }
}
</script>
<style lang='scss' scoped>
.more-config-panel{
    overflow: hidden;
    padding-right: 4px;
}
.more-config-tip-box{
    padding: 4px 0 12px;
}
.more-config-tip-item{
    font-size: 14px;
    line-height: 24px;
    color: #606266;
    font-weight: 500;
}
.more-config-group + .more-config-group{
    padding-top: 18px;
}
.more-config-grid{
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px 12px;
}
.more-config-card{
    margin-right: 0;
    ::v-deep(.el-checkbox__input){
        position: absolute;
        left: 10px;
        top: 50%;
        transform: translateY(-50%);
        z-index: 2;
        height: 14px;
    }
    ::v-deep(.el-checkbox__label){
        display: flex;
        align-items: center;
        width: 100%;
        min-height: 40px;
        padding: 8px 10px 8px 34px;
        border: 1px solid #E4E7ED;
        border-radius: 8px;
        background: #F7F8FA;
        color: #606266;
        font-size: 13px;
        line-height: 18px;
        box-sizing: border-box;
        transition: all 0.2s ease;
    }
    ::v-deep(.el-checkbox__input.is-checked + .el-checkbox__label){
        border-color: #7B61FF;
        background: #F5F3FF;
        color: #303133;
    }
    ::v-deep(.el-checkbox__input.is-disabled + .el-checkbox__label){
        background: #F5F7FA;
        color: #C0C4CC;
    }
    ::v-deep(.el-checkbox__input.is-disabled .el-checkbox__inner){
        background: #F5F7FA;
        border-color: #DCDFE6;
    }
}
.more-config-card__label{
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    line-height: 18px;
    font-weight: 500;
}
.more-config-card__tip{
    font-size: 14px;
    color: #909399;
    cursor: pointer;
}
</style>
