<template>
    <div class="ai-box flex-ai-s flex-column pd-t10 mr-b6">
        <div @mouseenter="onMouseenter" @mouseleave="onMouseleave" @wheel="handleScrollToTop"
        class="ai-content-box back-to-top w100 overflow_hidden overflow_auto_y pd-t12 pd-b12"
        style="flex: 1;" :id="`${type}_dom`" ref="scrollDiv">
            <div class="flex-ji-c"><el-button type="text" v-if="getIsMore" @click="getElsList('more')">加载更多</el-button></div>
            <div ref="dialogueBox" style="width: 95%;margin: 0 auto;">
                <div v-for="(item,index) in getEls" :key="index" style="overflow: hidden;">
                    <component :is="item.el" :class="`component_${index+1}`"
                    ref="aiContent"
                    :isUseBack="isUseBack" 
                    :bindConfig="bindConfig" 
                    v-bind="item.bind"
                    v-on="item.on"
                    :id="item.id"
                    :aiModel="aiModel"
                    :type="type"
                    :scene="scene" 
                    :isCompare="isCompare"
                    :otherOption="item.otherOption"
                    :readonly="readonly"
                    :pdfName="pdfName"
                    @share="share"
                    @shareHandler="shareHandler"
                    @commendClick="commendClick"
                    @generateChartsHandler="generateChartsHandler"
                    @answerAgain="answerAgain"
                    @aiCorrectFormat="aiCorrectFormat"
                    ></component>
                </div>
            </div>
        </div>
        <div v-if="$slots?.footer && !readonly" class="ai-footer-box pd-t6">
            <slot name="footer"></slot>
        </div>
        <slot name="after"></slot>
    </div>
</template>

<script>
import problem from './problem/index.vue';
import aiTip from './aiTip.vue';
import aiCustom from './aiCustom.vue';
import aiTitle from './aiTitle.vue';
import {listChannel} from '@/utils/aiCreateEle';
export default {
    components: {
        problem,
        aiTip,
        aiCustom,
        aiTitle
    },
    props:{
        els: {
            type: Array,
            default: ()=>{
                return []
            }
        },
        type: {
            type:String,
            default: ''
        },
        isCompare:{
            type:Boolean,
            default: false
        },
        bindConfig: {
            type: Object,
            default: ()=>{
                return {}
            }
        },
        aiModel: {
            type: Number,
            default: 0
        },
        scene: {
            type: String,
            default: ''
        },
        isUseBack: {
            type: Boolean,
            default: false
        },
        readonly: {
            type: Boolean,
            default: false
        },
        pdfName:{
            type: String,
            default: ''
        }
    },
    data() {
        return {};
    },
    computed: {
        getEls(){
            return listChannel(this.els, {type: this.type});
            // return listChannel(this.els, {
            //     type: this.type,
            //     renderContent: (text, renderOption = {}) => this.$parent?.getMdText?.(text, renderOption) || ''
            // });
            // return this.els.map(item=>{
            //     return aiCreateEle[item.el](item.bind);
            // })
        },
        getIsMore(){
            if(this.readonly){return false}
            return this.$parent.isMore
        }
    },
    watch: {
    },
    methods: {
        handleScrollToTop(event){
            if(this.readonly){return false}
            this.$parent?.handleScrollToTop(event);
        },
        addProblem(...arg){
            this.$parent?.addProblem(...arg);
        },
        commendClick(val){
            this.$emit('commendClick',val)
        },
        generateChartsHandler(id){
            this.$emit('generateChartsHandler',id)
        },
        answerAgain(values) {
            this.$emit('answerAgain', values)
        },
        aiCorrectFormat(val){
            this.$emit('aiCorrectFormat', val)
        },
        shareHandler(id){
            this.$refs?.aiContent.forEach(vNode=>{
                vNode?.foldThinking && vNode?.foldThinking(false)
            })
            this.$emit('shareHandler', id);
        },
        share(){
            this.$emit('share');
        },
        getElsList(type){
            if(this.readonly){return false}
            this.$parent?.getElsList(type);
        },
        emptySelection(event){
            let t = event?.target;
            let classList = t?.classList?.value?.split(' ') || []
            if(classList?.includes('ai-content-box')){return}
            if (window.getSelection) {
                const selection = window.getSelection();
                if (selection.empty) {
                    // 标准方法
                    selection.empty();
                } else if (selection.removeAllRanges) {
                    // 兼容旧版浏览器
                    selection.removeAllRanges();
                }
            } else if (document.selection) {
                // 兼容 IE 浏览器
                document.selection.empty();
            }
        },
        onMouseenter($event){
            this.emptySelection($event);
            if($event.target === this.$refs.scrollDiv){
                // 放开ctry+c的限制，c表示按键c，99 表示是否按下ctry健
                window.notKeyDown.c = 99;
            }
        },    
        onMouseleave($event){
            this.emptySelection($event);
            window.notKeyDown.c = 0;
        },
        // scrollToBottom() {
        //     this.$parent?.scrollToBottom();
        // }
    },
    created() {
    },
    mounted() {

    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.ai-footer-box{
    padding-top: 14px !important;
}

</style>
