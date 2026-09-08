<template>
    <aiContent 
    :els="getEls"
    @commendClick="commendClick"
    @generateChartsHandler="generateChartsHandler"
    @shareHandler="shareHandler"
    @answerAgain="answerAgain"
    @aiCorrectFormat="aiCorrectFormat"
    :isUseBack="isUseBack" 
    :scene="scene" 
    :isCompare="isCompare" 
    :bindConfig="bindConfig" 
    :readonly="readonly"
    :pdfName="pdfName"
    :type="type">
        <template #footer>
            <btOrInput
                ref="inputField"
                :els="getEls"
                notIdentity
                :load="sendAskLoad"
                :shareStatus="shareStatus"
                :showModelType="showModelType"
                :modelTypeProps="modelTypeProps"
                :showKnowledgeBase="showKnowledgeBase"
                :moreConfigValue="moreConfigValue"
                :moreConfigProps="moreConfigProps"
                @model-change="$emit('model-change',$event)"
                @model-option-change="$emit('model-option-change',$event)"
                @knowledge-click="$emit('knowledge-click')"
                @more-config-change="$emit('more-config-change',$event)"
                @submit="onSubmit"
                @share="shareHandler"
            ></btOrInput>
        </template>
        <template #after>
            <violationDialog ref="violationDialog" :scene="scene" :sentenceMarkData="sentenceMarkData" @submit="dialogSubmit"></violationDialog>
        </template>
    </aiContent>
    
</template>

<script>
import aiContent from './../common/aiContent.vue';
import mixin from './../common/mixin.js';
import violationDialog from './violationDialog/index.vue';
import btOrInput from './../common/btOrInput.vue';
export default {
    components: {
        aiContent,
        violationDialog,
        btOrInput
    },
    props:{
        showModelType: {
            type: Boolean,
            default: false
        },
        modelTypeProps: {
            type: Object,
            default: () => ({})
        },
        showKnowledgeBase: {
            type: Boolean,
            default: false
        },
        moreConfigValue: {
            type: Object,
            default: () => ({})
        },
        moreConfigProps: {
            type: Object,
            default: () => ({})
        }
    },
    mixins: [mixin],
    data() {
        return {
        };
    },
    computed: {
        
    },
    watch: {
    },
    methods: {
        initMain(){
            this.$nextTick(()=>{
                // 初始化问题设置。
                this.initProblem();
                // 加载问题列表
                this.getProblem(1).finally((res)=>{
                    this.getHistoryParagraphList(1).finally(()=>{
                        // 执行添加
                        this.getElsList('init').finally(()=>{
                            this.$emit('loading');
                        });
                    })
                });
            })
        },
        initProblem(option){
            this.initAddProblem(option,(data)=>{
                this.$refs.violationDialog?.show(data)
            })
        },
        dialogSubmit(data){
            let html = '';
            let o ={
                reasonViolation: data?.reason,
                realContent: data?.problem,
                cueWordsId: data?.id,
                cueWordsType: 0 // 0: 系统提示词 1:用户自定义提示词。
            }
            if(data.paragraphCode === 0 || !data.paragraphCode){
                o.paragraphContent = data.paragraphContent;
                html = `分析内容: ${data.paragraphContent || '全文本'}`;
            }else{
                html = `分析段落：${data?.item?.alias}`;
                o.paragraphContent = '';
            }
            this.sendAsk(`<div>
                违规原因：${data?.reason}
                <br/><br/>
                ${html}
            </div>`,o);
        },
        onSubmit(val){
            const { textarea, extra, htmlText } = val;
            this.sendAsk(htmlText, {
                additionalList: extra,
                realContent: textarea,
            })
            // 清理掉输入框
            this.$nextTick(()=>{
                this.$refs?.inputField?.clear()
            })
        },
    },
    created() {
        
    },
    mounted() {

        this.initMain()
    },
    
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {
        this.clearInfo();
    }, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {
        this.initMain()
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.ai-box{
    height:100%;
} 
</style>
