<!--
@description AI 文本明细输入条件弹层：用于控制段落时间、在线人数、互动率等附加信息的显示项。
注意：
1) 默认仅初始化勾选数据，不再在进入页面时自动弹出；
2) 用户点击“输入条件”标签后仍可手动打开并调整配置。
-->
<template>
    <el-popover
        placement="bottom-start"
        width="200"
        :disabled="disabled"
        ref="popover"
        v-model="popoverOpen"
        trigger="manual">
        <div>
            <div v-for="item in getItems">
                <el-checkbox v-model="getData[item.prop]" v-if="!item.hide" :true-label="1" :false-label="0" :disabled="item.disabled" @change="onChange">{{item.label}}</el-checkbox>
            </div>
            <afp-button size="small" type="primary" :plain="false" @click="popoverOpen=false"
                        style="padding-inline: 12px;float: right">确认
            </afp-button>
        </div>
        <el-tag  slot="reference" ref="ref-tag" class="mg-r10 cs-p" @click.stop="()=>popoverOpen=!popoverOpen" style="border-radius: 28px" :type="disabled?'info':''" size="medium" :effect="isAll?'dark':'plain'">
            输入条件
        </el-tag>
    </el-popover>
</template>

<script>
/**
 * @description AI 文本明细输入条件脚本：负责输入条件的默认勾选、弹层开关与外部点击关闭逻辑。
 */

import myUtils from "@/utils/utils";
import { PLATFORM_TYPE_ENUM } from '@/enum';

export default {
    components: {},
    props:{
        value: {
            type: [Object,undefined],
            default: undefined
        },
        disabled: {
            type: Boolean,
            default: false
        },
        barrageNum:{
            type: Number,
            default: 0
        },
        hide:{
            type:Object,
            default: ()=>{
                return {}
            }
        },
        sentenceMarkData:{
            type:Object,
            default: ()=>{
                return {}
            }
        },
        aiCueType:{
            type:String,
            default: ''
        },
        isCompare:{
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            dataDisplay: {},
            popoverOpen: false,
            items:[
                {label: '开始时间',prop: 'startTime',default: 0},
                {label: '自然时间',prop: 'natureTime',default: 0},
                {label: '在线人数',prop: 'onlineNum',default: 0},
                {label: '语速',prop: 'analysisChar',default: 0},
                {label: '弹幕数量',prop: 'barrageNum', default: 0, hides: [PLATFORM_TYPE_ENUM.kuaishou] },
                {label: '成交数量',prop: 'dealNum', default: 0,disabled: true},
                {label: '互动率',prop: 'interactionRate',default: 0, hides: [PLATFORM_TYPE_ENUM.kuaishou]},
                {label: '成交率',prop: 'dealRate',default: 0,disabled: true, hides: [PLATFORM_TYPE_ENUM.kuaishou]},
                {label: '销售额',prop: 'sales',default: 0,disabled: true, hides: [PLATFORM_TYPE_ENUM.kuaishou]},
                {label: 'UV价值',prop: 'uv',default: 0,disabled: true, hides: [PLATFORM_TYPE_ENUM.kuaishou]},
                {label: '投放消耗',prop: 'qianchuanCost',default: 0,disabled: true, hides: [PLATFORM_TYPE_ENUM.kuaishou]},
            ]
        };
    },
    computed: {
        getPlatform(){
            return this.sentenceMarkData?.videoInfo?.PlatformType || this.sentenceMarkData?.fileInfo?.platformType
        },
        isKuaishou() {
            return this.getPlatform == PLATFORM_TYPE_ENUM.kuaishou
        },
        getData:{
            get(){
                return this.value || this.dataDisplay
            },
            set(val){
                try{
                    this.$set(this,'dataDisplay',val);
                    this.$emit('input',val);
                }catch(err){
                    console.error(err)
                    this.$emit('input', this,getData)
                }
            }
        },
        getItems(){
            return this.items.filter(item=>{
                if(item.hides && item.hides.length){
                    if(this.isKuaishou){
                        return !item.hides.includes(PLATFORM_TYPE_ENUM.kuaishou);
                    }else{
                        return true
                    }
                }else{
                    return true
                }
            })
        },
        isAll(){
            return Object.values(this.dataDisplay)?.every((item,index)=>{
                if(this.items[index]?.disabled){return true};
                return !!item;
            })
        },
        isBuyInData(){
            return this.sentenceMarkData?.dataSourceType === 1
        },
        showTotalDeal() {
            const {purchaseCountStart, purchaseCountEnd} = this.sentenceMarkData
            return myUtils.isGreaterThanZero(purchaseCountStart) || myUtils.isGreaterThanZero(purchaseCountEnd)
        },
        showUV(){
            const {uvValueStart,uvValueEnd} = this.sentenceMarkData
            return myUtils.isGreaterThanZero(uvValueStart) || myUtils.isGreaterThanZero(uvValueEnd)
        },
        showSales(){
            const {volumeStart,volumeEnd} = this.sentenceMarkData
            return myUtils.isGreaterThanZero(volumeStart) || myUtils.isGreaterThanZero(volumeEnd)
        },
        showQianchuanCost() {
            if (!this.$store.getters.largeEnterprises) return false
            return this.hasMetricValue(this.sentenceMarkData?.totalQianchuanCost) || this.hasParagraphMetric('qianchuanCost')
        },
        defaultRules(){
            if(this.isCompare){
                return ['startTime', 'onlineNum']
            }
            let defaultMap = {
                assistant: ['startTime', 'onlineNum', 'barrageNum', 'dealNum', 'interactionRate', 'dealRate', 'sales', 'uv', 'qianchuanCost'],
                violation: ['natureTime'],
                textAssistant: [
                    'startTime',
                    'interactionRate'
                ]
            };
            return defaultMap[this.aiCueType] || [];
        }

    },
    watch: {},
    methods: {
        onChange(){
            this.$emit('change',this.getData)
        },
        hasMetricValue(value) {
            return value !== undefined && value !== null && value !== ''
        },
        hasParagraphMetric(prop) {
            return (this.sentenceMarkData?.sentenceMarkList || []).some(item => this.hasMetricValue(item?.[prop]))
        },
        handlePointerDown(e) {
            const path = e.composedPath()
            // 是否点在任意 popover / reference / 关联弹层 内
            const isInside = () => {
                const popper = this.$refs.popover?.popperElm
                const refEl = this.$refs[`ref-tag`]?.$el

                // 当前 popover 自己的内容
                if (popper && path.includes(popper)) return true
                if (refEl && path.includes(refEl)) return true

                // 选择类组件的弹层 需要特殊处理;如有需要，可自行继续添加补充
                return path.some(el => {
                    if (!(el instanceof HTMLElement)) return false
                    return (
                        el.classList.contains('el-picker-panel') ||     // 日期
                        el.classList.contains('el-select-dropdown') ||  // 下拉选择
                        el.classList.contains('el-dropdown-menu') ||    // 下拉菜单
                        el.classList.contains('el-cascader-panel') ||   // 级联
                        el.classList.contains('el-color-dropdown')      // 颜色
                    )
                })
            }

            if (isInside()) return

            this.popoverOpen = false
        },
        initData(){
            this.$nextTick(()=>{
                let o = {};
                const {totalBarrageNum, interactionPercent} = this.sentenceMarkData;
                const disabledRules = {
                    barrageNum: () => totalBarrageNum <= 0,
                    dealNum: () => !(this.showTotalDeal && this.isBuyInData),
                    interactionRate: () => totalBarrageNum <= 0 || !interactionPercent,
                    dealRate: () => !(this.showTotalDeal && this.isBuyInData),
                    sales: () => !(this.showSales && this.isBuyInData),
                    uv: () => !(this.showUV && this.isBuyInData),
                    qianchuanCost: () => !this.showQianchuanCost,
                };
                this.items?.forEach(item => {
                    const { prop } = item;
                    if (prop === 'qianchuanCost' && !this.$store.getters.largeEnterprises) {
                        item.hide = true;
                    }
                    if (typeof this.hide[prop] !== 'undefined') {
                        item.hide = this.hide[prop];
                    }
                    if (disabledRules[prop]) {
                        item.disabled = disabledRules[prop]();
                    }
                    if (this.defaultRules?.includes(prop) && !item.disabled) {
                        item.default = 1;
                    }
                    o[prop] = typeof item.default !== 'undefined' ? item.default : (item.disabled ? 0 : 1);
                });
                this.getData = o;
                this.onChange();
            })
        },
    },
    created() {

    },
    mounted() {
        this.initData();
        document.addEventListener('pointerdown', this.handlePointerDown, true)
    },
    beforeCreate() {
    }, //生命周期 - 创建之前
    beforeMount() {
    }, //生命周期 - 挂载之前
    beforeUpdate() {
    }, //生命周期 - 更新之前
    updated() {
    }, //生命周期 - 更新之后
    beforeDestroy() {
        document.removeEventListener('pointerdown', this.handlePointerDown, true)
    }, //生命周期 - 销毁之前
    destroyed() {
    }, //生命周期 - 销毁完成
    activated() {
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>

</style>
