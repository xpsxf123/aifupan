<!--
@description AI 助手通用布局：负责标题区、背景配置弹层、退出操作与内容承载区域的统一编排。
注意：
1) 背景配置改为仅回填数据，不再在进入页面时自动弹出；
2) 手动点击“背景配置”按钮后仍可正常打开对应弹层。
-->
<template>
    <div class="ai-layout-box">
        <div v-if="shareId" class="ai-layout-share-header flex-ji-c w100 pd-t10 pd-b10">
                <img style="max-width: 20px;" src="@/assets/imgs/new_logoT.png" alt="">
                <span class="font-s12">以下内容由爱复盘+{{ modeName }}+{{ getUserInfo.nickName }}联合提供</span>
        </div>
        <div v-if="!shareId && type === 'assistant' && !readonly" class="ai-layout-left-actions">
            <div v-for="(item,index) in getConfigList" :key="item.name" class="ai-layout-left-actions__item">
                <el-popover
                    placement="right-start"
                    width="280"
                    v-model="popoverOpen[item.name]"
                    trigger="manual"
                    ref="popovers"
                    popper-class="ai-setting-popper"
                >
                    <afp-button
                        slot="reference"
                        :class="[`config-action-btn config-popover-${index}`]"
                        type="primary"
                        circle
                        :ref="'ref-' + item.name"
                        @click.stop="()=>popoverStatus(!popoverOpen[item.name],item.name)"
                    >
                        <span style="line-height: 16px;" v-html="formatConfigLabel(item.label)"></span>
                    </afp-button>
                    <AccountInfo
                        :targetType="targetType"
                        v-if="popoverOpen[item.name]"
                        :config="config"
                        :uploadScreenshot="uploadScreenshot"
                        :uploadBoard="uploadBoard"
                        @update:uploadScreenshot="$emit('update:uploadScreenshot', $event)"
                        @update:uploadBoard="$emit('update:uploadBoard', $event)"
                        :sentenceMarkData="getSentenceMarkData(item.key)"
                        @popoverStatus="(val)=>popoverStatus(val,item.name)"
                        :backgroundConfig="backgroundConfig[item.name]"
                        @backgroundConfigChange="(val)=>backgroundConfigChange(val,item.name)"
                    />
                </el-popover>
            </div>
            <afp-button v-if="getSyncScene===1" class="config-action-btn" type="primary" circle @click="viewOptimize">
                <span style="line-height: 16px;">本场<br/>优化</span>
            </afp-button>
            <slot name="left-actions"></slot>
        </div>
        <div v-if="!readonly" class="ai-layout-quit-fixed">
            <afp-button type="primary" plain round size="medium" @click="quitAi">
                <span class="flex-ji-c"><span>退出</span></span>
            </afp-button>
        </div>
        <div class="ai-window-box" :class="{'ai-window-box--share': !!shareId}" @contextmenu.prevent="rightContextMenuHanlder">
            <slot></slot>
        </div>
        <OptimizeActions ref="optimize_actions"/>
    </div>
</template>

<script>
/**
 * @description AI 助手通用布局脚本：处理背景配置回填、弹层开关与右键菜单等交互逻辑。
 */
import {pick} from 'lodash'
import contextmenu from '/src/mixins/contextmenu.js';
import AccountInfo from "@/components/analysis/ai/common/accountInfo.vue";
import OptimizeActions from '@/components/optimizeActions'
import {ENUM_OBJ} from '@/utils/actionConfig.js';
import {trackEvent} from "@/utils/laTrack";
export default {
    components: {
        AccountInfo,
        OptimizeActions
    },
    props:{
        title: {
            type: String,
            default: ''
        },
        type: {
            type: String,
            default: ''
        },
        isCompare: {
            type: Boolean,
            default: false
        },
        uploadScreenshot: {
            type: Number,
            default: 0
        },
        uploadBoard: {
            type: Number,
            default: 0
        },
        config: {
            type: Object,
            default: ()=>{
                return {}
            }
        },
        readonly: {
            type: Boolean,
            default: false
        },
        shareId:{
            type: String,
            default: ''
        },
        modeName:{
            type: String,
            default: 'DeepSeek'
        },
        targetType:{
            type: String,
            default: ''
        },
        sentenceMarkData: {
            type: Object,
            required: ()=>{return {}}
        },
    },
    data() {
        return {
            popoverOpen: {
                one: false,
                two: false
            },
            backgroundConfig: {
                one: {},
                two: {}
            }
        };
    },
    mixins: [contextmenu],
    computed: {
        getUserInfo(){
            return this.$store.getters.getUserInfo;
        },
        getType(){
            // 违规
            return this.type === 'assistant' ? '违规助手' : '运营助手'
        },
        getSentenceMarkData(){
            return (key) => {
                if (this.isCompare) {
                    return this.sentenceMarkData[key]
                } else {
                    return this.sentenceMarkData
                }
            }
        },
        getSourceInfo(){
            if (this.isCompare) {
                return {
                    one: {
                        sourceId: this.sentenceMarkData?.data1?.videoInfo?.VideoId || this.sentenceMarkData?.data1?.fileInfo?.fileId,
                        sourceType: this.sentenceMarkData?.data1?.videoInfo?.VideoId ? 1 : 2
                    },
                    two: {
                        sourceId: this.sentenceMarkData?.data2?.videoInfo?.VideoId || this.sentenceMarkData?.data2?.fileInfo?.fileId,
                        sourceType: this.sentenceMarkData?.data2?.videoInfo?.VideoId ? 1 : 2
                    }
                }
            } else {
                return {
                    one: {
                        sourceId: this.sentenceMarkData?.videoInfo?.VideoId || this.sentenceMarkData?.fileInfo?.fileId,
                        sourceType: this.sentenceMarkData?.videoInfo?.VideoId ? 1 : 2
                    }
                }
            }
        },
        getSyncScene() {
            //syncScene:对比使用场景(对比分析才有) 1：对比上一次场、2：不同直播间对比、3：同直播间对比
            return this.sentenceMarkData?.info?.syncScene
        },
        getConfigList() {
            const syncScene = this.getSyncScene;
            let configList = [];

            const singleConfig = [
                {name: 'one', label: '背景配置', key: 'data1'}
            ]
            const compareConfig = [
                {name: 'one', label: '优化场次背景', key: 'data1'},
                {name: 'two', label: '参考场次背景', key: 'data2'}
            ]
            if (!this.isCompare) {
                const {fileType} = this.sentenceMarkData?.fileInfo || {}
                if (fileType !== undefined && fileType === 0) {
                    configList = singleConfig
                }
                if (this.sentenceMarkData?.videoInfo) {
                    configList = singleConfig
                }
            } else {
                const {info} = this.sentenceMarkData;
                const needCompare = info?.contrastType !== 0 || syncScene === 2
                configList = needCompare ? compareConfig : singleConfig
            }

            return configList
        }
    },
    watch: {},
    methods: {
        quitAi(){
            this.$emit('quit')
        },
        formatConfigLabel(label = '') {
            const text = String(label || '')
            if (!text) return ''
            if (text.length <= 4) {
                return `${text.slice(0, 2)}<br/>${text.slice(2)}`
            }
            return `${text.slice(0, 3)}<br/>${text.slice(3)}`
        },
        popoverStatus(status, name) {
            if (status && name === 'one') {
                trackEvent('P003_A0059')
            }
            this.popoverOpen = {
                one: false,
                two: false,
                [name]: status
            }
        },
        backgroundConfigChange(compereForm, name) {
            this.backgroundConfig = {
                ...this.backgroundConfig,
                [name]: compereForm
            }
            this.$nextTick(() => {
                this.$emit('backgroundConfigChange', this.backgroundConfig)
            })
        },
        // 运营 assistant
        // 违规 violation
        onToggle(){
            this.$emit('toggle',this.type === 'assistant'?'violation':'assistant')
        },
        rightContextMenuHanlder(event){
            let selectText = window.getSelection()?.toString()?.trim()

            this.rightContextMenu(event,{
                copyTxt: selectText
            })
        },
        async getAnchorInfo(name) {
            if (!this.getSourceInfo[name]?.sourceId) return
            const result = await this.$httpBack.words.getAiPartial({
                sourceId: this.getSourceInfo[name]?.sourceId,
                sourceType: this.getSourceInfo[name]?.sourceType,
            })
            if (result.code !== 0) return
            const keys = Object.keys(ENUM_OBJ)
            return {
                [name]: {
                    ...pick(result.data, [...keys,'premiereDate','anchorSituation','accountType']),
                }
            }
        },
        getAnchorInfos() {
            let httpServers = null

            if (this.isCompare) {
                if (this.getSyncScene === 2) {
                    httpServers = [this.getAnchorInfo('one'), this.getAnchorInfo('two')]
                } else {
                    httpServers = [this.getAnchorInfo('one')]
                }
            } else {
                httpServers = [this.getAnchorInfo('one')]
            }
            Promise.all(httpServers).then((results) => {
                this.backgroundConfig = {
                    one: results[0]?.one,
                    two: results[1]?.two
                }
                this.$nextTick(() => {
                    this.$emit('backgroundConfigChange', this.backgroundConfig)
                })
            })
        },
        viewOptimize(){
            this.$refs?.optimize_actions?.change(true,this.sentenceMarkData?.data1?.videoInfo);
        },
        handlePointerDown(e) {
            const path = e.composedPath()
            // 是否点在任意 popover / reference / 关联弹层 内
            const isInside = this.$refs.popovers?.some((popover, index) => {
                const popper = popover?.popperElm
                const refEl = this.$refs[`ref-${this.getConfigList[index].name}`]?.$el

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
            })

            if (isInside) return

            // 区域之外 全部关闭
            this.popoverOpen = {
                one: false,
                two: false
            }
        }
    },
    created() {

    },
    async mounted() {
        if (this.type === 'assistant' && !this.readonly) this.getAnchorInfos()

        this.setContextMenuData({
            permission: ['copy']
        })
        document.addEventListener('pointerdown', this.handlePointerDown, true)
    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {
        document.removeEventListener('pointerdown', this.handlePointerDown, true)
    }, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.ai-window-box{
    height: 100%;
}
.ai-window-box--share{
    height: calc(100% - 48px);
}
.ai-layout-quit-fixed{
    position: fixed;
    top: 12px;
    right: 12px;
    z-index: 1002;
}
.ai-layout-left-actions{
    position: absolute;
    left: 8px;
    bottom: 0;
    display: flex;
    flex-direction: column;
    gap: 6px;
    z-index: 1001;
}
.ai-layout-box{
    position: relative;
}
.config-action-btn{
    width: 42px !important;
    height: 42px !important;
    padding: 0 !important;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    text-align: center;
    background: #fff !important;
    border: 1px solid rgba(76, 141, 255, 0.9) !important;
    color: #4C8DFF !important;
    font-size: 12px !important;
    line-height: 16px !important;
    font-weight: 400 !important;
    box-shadow: none !important;
}
.config-action-btn:hover,
.config-action-btn:focus{
    background: rgba(76, 141, 255, 0.10) !important;
    border-color: #4C8DFF !important;
    color: #4C8DFF !important;
    box-shadow: 0px 2px 8px 0px rgba(76, 141, 255, 0.24) !important;
}
.config-action-btn:active{
    background: rgba(76, 141, 255, 0.14) !important;
    border-color: #4C8DFF !important;
}
</style>
<style lang='scss'>
    .ai-setting-popper{
        padding: 0!important;
    }
</style>
