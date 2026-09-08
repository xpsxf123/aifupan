<template>
    <aiContent 
    :els="getEls" 
    :isCompare="isCompare" 
    :isUseBack="isUseBack" 
    :scene="scene"
    :aiModel="aiModel"
    :readonly="readonly"
    @commendClick="commendClick"
    @generateChartsHandler="generateChartsHandler"
    @answerAgain="answerAgain"
    @aiCorrectFormat="aiCorrectFormat"
    @shareHandler="shareHandler" 
    :bindConfig="bindConfig" 
    :pdfName="pdfName"
    :type="type">
        <template #footer>
            <btOrInput
                ref="inputField"
                :els="getEls"
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
    </aiContent>
</template>

<script>
import aiContent from './../common/aiContent.vue';
import mixin from './../common/mixin.js';
import btOrInput from './../common/btOrInput.vue';
export default {
    components: {
        aiContent,
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
            this.getProblem(this.getCueType).finally((res)=>{
                this.getHistoryParagraphList(this.getCueType).finally(()=>{
                    // 执行添加
                    this.getElsList('init').finally(()=>{
                        this.$emit('loading');
                    });
                })
            });
        })
        },
        onSubmit(val){
            this.$emit('submit',val);
            const { textarea, identity, extra, htmlText } = val;
            this.sendAsk(htmlText, {
                additionalList: extra,
                identity,
                realContent: textarea,
            })
            this.$nextTick(()=>{
                this.$refs?.inputField?.clear()
            })
        },
        initProblem(option){
            this.initAddProblem(option);
        }
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
        this.initMain();
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.ai-box{
    height: 100%
} 
</style>
