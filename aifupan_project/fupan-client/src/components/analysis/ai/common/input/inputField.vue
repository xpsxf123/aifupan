<template>
    <div class="input-box" style="width: 80%;">
        <div class="input-top-actions flex-jc-sb flex-ai-c">
            <div class="ohter-box flex-ai-c">
                <identity v-model="options.identity" v-if="!notIdentity" :list="identity"></identity>
                <extra  v-model="options.extra"  v-if="!notExtra" :list="extra"></extra>
                <!-- <CustomPrompt v-model="options.textarea" :textarea="options.textarea" v-if="!notCustomPrompt" :list="customPrompt" @addPromptToInput="addPromptToInput"/> -->
            </div>
            <div v-if="showMoreConfig" class="more-config-box">
                <moreConfig
                    v-model="options.moreConfig"
                    v-bind="moreConfigProps"
                    @change="onMoreConfigChange"
                ></moreConfig>
            </div>
        </div>
        <div :class="{'b-cTheme': isFoucs,'b-c1':!isFoucs}" class="textarea-box b-all1 brs-6 pd-6 mg-t6" @keydown.enter.stop="handleKeyDown">
            <el-input
                type="textarea"
                class="w100 textarea-input"
                v-model="options.textarea"
                :autosize="{ minRows: 2, maxRows: 4}"
                placeholder="请输入您想分析和解决的问题，越详细越好，输入前请务必填写背景配置，AI才更能了解您的账号情况"
                @focus="isFoucs = true"
                @blur="isFoucs = false"
                >
            </el-input>
            <div class="input-bottom-row flex-ai-c pd-t6">
                <div class="input-bottom-right flex-ai-c">
                    <ModelType
                        v-if="showModelType"
                        v-bind="modelTypeProps"
                        variant="inline"
                        @change="onModelChange"
                        @change-model="onModelOptionChange"
                    ></ModelType>
                    <span v-if="showModelType" class="input-separator">|</span>
                    <span v-if="showKnowledgeBase" class="cs-p action-link" @click="onKnowledgeClick">
                        知识库 &gt;
                    </span>
                    <span class="input-separator">|</span>
                    <span class="cs-p font-s12 action-link" @click="hideInput">隐藏输入框</span>
                    <span class="send-img-box cs-p mg-l16" :class="{'not-hover':load,'sned-highlight': !!options?.textarea}" @click="handleKeyDown('click')">
                        <img class="send-img" src="~@/assets/imgs/aiSend.png">
                        <img class="send-img-hover" src="~@/assets/imgs/aiSendHover.png">
                    </span>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import extra from './extra.vue';
import identity from './identity.vue';
import moreConfig from './moreConfig.vue';
import CustomPrompt from './customPrompt.vue'
import ModelType from '@/components/analysis/ai/common/modelType/index.vue'
import myUtils from '@/utils/utils.js';
export default {
    components: {
        extra,
        identity,
        moreConfig,
        CustomPrompt,
        ModelType
    },
    props:{
        notIdentity: {
            type: Boolean,
            default: false
        },
        notExtra: {
            type: Boolean,
            default: false
        },
        identity: {
            type:Array,
            default: () => []
        },
        extra: {
            type:Array,
            default: () => []
        },
        notCustomPrompt:{
            type: Boolean,
            default: false
        },
        customPrompt:{
            type:Array,
            default: () => []
        },
        load: {
            type: Boolean,
            default: false
        },
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
    data() {
        return {
            isFoucs: false,
            options: {
                textarea: '',
            },
            myThrottle: myUtils.throttle(1000)
        };
    },
    computed: {
        showMoreConfig() {
            return !this.moreConfigProps?.isCompare && !this.moreConfigProps?.hideMoreConfig
        }
    },
    watch: {
        moreConfigValue: {
            handler(val){
                this.$set(this.options, 'moreConfig', {
                    ...(val || {})
                })
            },
            deep: true,
            immediate: true
        }
    },
    methods: {
        onModelChange(val){
            this.$emit('model-change', val)
        },
        onModelOptionChange(val){
            this.$emit('model-option-change', val)
        },
        onKnowledgeClick(){
            this.$emit('knowledge-click')
        },
        onMoreConfigChange(val){
            this.$emit('more-config-change', val)
        },
        init(){
            this.$set(this,'options',{
                ...this.options,
                textarea: ''
            })
        },
        clear(){
            this.options.textarea = '';
        },
        handleKeyDown(event){
            // load和文本都不发送消息，或者文本为
            if(this.load || !this.options.textarea) {
                return
            };
            this.myThrottle(()=>{
                if (event === 'click' || event?.key === 'Enter' && !event?.ctrlKey && !event?.shiftKey) {
                    event?.preventDefault && event?.preventDefault();
                    // 获取textarea的值
                    let text = this.options.textarea;
                    // 在HTML元素中显示，使用<br>标签替换换行符
                    this.options.htmlText = text.replace(/\r?\n/g, '<br>');
                    this.$emit('submit',this.options);
                }
            });
            event?.preventDefault &&event?.preventDefault();

        },
        hideInput(){
            this.$emit('hide');
            this.$nextTick(()=>{
                this.init();
            })
        },
        addPromptToInput(item) {
            this.$set(this, 'options', {
                textarea: `${this.options.textarea}${item.promptContent}`
            })
            // if (this.options.textarea) {
            // } else {
            //     this.options.textarea = item.promptContent;
            // }
        }
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
.input-top-actions{
    width: 100%;
}
.ohter-box{
    margin:-5px;
    >*{
        margin: 5px;
    }
}
.input-bottom-row{
    justify-content: flex-end;
}
.input-bottom-right{
    color: var(--color-main);
    font-size: 12px;
    line-height: 16px;
}
.action-link{
    color: var(--color-main);
}
.input-separator{
    color: #C0C4CC;
    margin: 0 8px;
}
.textarea-box{
    transition: all 0.3s;
    .textarea-input{
        ::v-deep(.el-textarea__inner){
            white-space: pre-wrap;
            resize: none;
            border: none !important;
        }
    }
}
.send-img-box{
    position: relative;
    display: inline-block;
    width: 28px;
    height: 28px;
    &:hover .send-img-hover{
        opacity: 1;
    }
    &:hover .send-img{
        opacity: 0;
    }
    img{
        transition: all 0.3s;
        position: absolute;
        max-width: 100%;
        max-height: 100%;
        left: 0;
        top: 0;
    }
    .send-img{
        opacity: 1;
    }
    .send-img-hover{
        opacity: 0;
    }
}
.not-hover{
    &:hover .send-img-hover{
        opacity: 0;
    }
    &:hover .send-img{
        opacity: 1;
    }
}
.sned-highlight{
    .send-img-hover{
        opacity: 1 !important;
    }
}
</style>
