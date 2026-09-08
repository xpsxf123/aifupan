<template>
    <popoverBt text="更多参数" :select="isSelect" :width="820">
        <template #title>
            <div class="font-s16 pd-b6 text-colorMain">更多参数</div>
        </template>
        <moreConfigPanel
            :value="value"
            :hide="hide"
            :sentenceMarkData="sentenceMarkData"
            :aiCueType="aiCueType"
            :isCompare="isCompare"
            @input="$emit('input', $event)"
            @change="$emit('change', $event)"
        ></moreConfigPanel>
    </popoverBt>
</template>

<script>
/**
 * @file 更多配置按钮组件。
 * @description 负责承载页面级“更多配置”弹出入口，内部复用独立的配置面板组件。
 */
import popoverBt from './popoverBt.vue';
import moreConfigPanel from './moreConfigPanel.vue';

export default {
    components: {
        popoverBt,
        moreConfigPanel
    },
    props:{
        value: {
            type: Object,
            default: () => ({})
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
        return {};
    },
    computed: {
        isSelect(){
            const basicChecked = Object.values(this.value?.basicData || {}).some(item => !!item)
            const extraChecked = Object.values(this.value?.extraConditions || {}).some(item => !!item)
            const dynamicChecked = Object.values(this.value?.dynamicConfigs || {}).some(item => !!item)
            return basicChecked || extraChecked || dynamicChecked
        }
    },
    watch: {},
    methods: {},
}
</script>
<style lang='scss' scoped>
</style>
