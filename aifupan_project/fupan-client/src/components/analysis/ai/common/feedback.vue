<template>
    <div>
        <el-dialog
            :close-on-click-modal="false"
            :close-on-press-escape="false"
            title="再分析一次"
            :visible.sync="dialogVisible"
            custom-class="prompt-dialog-list"
            width="50vw">
            <div class="content">
                <div class="subTitle text-sm">你对本次分析不满意的地方是？以及你有哪些额外要求：</div>
                <div class="flex items-center justify-between" style="margin-top: 16px">
                    <div class="flex-1">
                        <span>1.不满意的地方：</span>
                        <el-input
                            style="margin-top: 12px"
                            type="textarea"
                            :rows="8"
                            maxlength="300" show-word-limit
                            :placeholder="'你输出的分析内容很好，但是输出的格式有问题，都是黑色字体，不美观，表格输出不规范，可读性差，数据有遗漏，粗体和细体不规范、数据可能有错误。你给我的优化方向或学习方向很好，但是不够详细。\n'+
                                '1、如果这是自有直播间：我是新手小白运营，选出你觉得当前我最需要优化且最能达成我当前目标的优化方向，给我更详细的落地方案，不低于5000字。\n'+
                                '2、如果这是竞品直播间：我是新手小白运营，结合本场我想学习的方向，选出你觉得这场直播我最值得学习的方向，给我更详细的落地方案，不低于5000字。'"
                            v-model="feedbackValue">
                        </el-input>
                    </div>
                    <div style="width: 20px"></div>
                    <div class="flex-1">
                        <span>2.额外要求：<span style="color: red">（以下要求是相互矛盾的，请编辑后提问）</span></span>
                        <el-input
                            style="margin-top: 12px"
                            type="textarea"
                            :rows="8"
                            maxlength="300" show-word-limit
                            :placeholder="'额外要求1：优化你上次输出结果里我不满意的地方，然后再将完整的分析结果完整保留后输出给我。反复检查你重新输出的结果和上次的分析结果，你重新输出的结果，不要有遗漏的内容。\n' +
                                '\n' +
                                '额外要求2：优化你上次输出结果里我不满意的地方。仅输出你优化过后的这个方向，不要输出其他内容了。输出更详细的执行落地方案，我是新手小白，给我5000字以上的落地方案。'"
                            v-model="extraRequire">
                        </el-input>
                    </div>

                </div>
                <div class="flex items-center cursor-pointer" style="padding:12px 0 20px 0">
                    <div style="width: 95px;">重新选择模型</div>
                    <ModelType ref="model_type" type="assistant" :isCompare="isCompare" @change="modelChange" />
                </div>
            </div>
            <div slot="footer" class="flex items-center justify-center">
                <afp-button @click="onCancel" size="default">取消</afp-button>
                <afp-button type="primary" :plain="false" @click="answerAgain" size="default" style="margin-left: 10%">再分析一次</afp-button>
            </div>
        </el-dialog>
    </div>
</template>

<script>
import aiTypeMixin from '@/mixins/aiTypeMixin'
import ModelType from "@/components/analysis/ai/common/modelType/index.vue";

export default {
    mixins: [aiTypeMixin],
    components: {ModelType},
    props: {
        isCompare: {
            type: Boolean,
            default: false
        },
        aiModel: {
            type: Number,
            default: 0
        }
    },
    data() {
        return {
            dialogVisible:false,
            modelType: 1,
            feedbackValue:'',
            extraRequire:''
        };
    },
    computed: {},
    watch: {
        dialogVisible:{
            handler() {
                this.feedbackValue = '你输出的分析内容很好，但是输出的格式有问题，都是黑色字体，不美观，表格输出不规范，可读性差，数据有遗漏，粗体和细体不规范、数据可能有错误。你给我的优化方向或学习方向很好，但是不够详细。\n' +
                    '1、如果这是自有直播间：我是新手小白运营，选出你觉得当前我最需要优化且最能达成我当前目标的优化方向，给我更详细的落地方案，不低于5000字。\n' +
                    '2、如果这是竞品直播间：我是新手小白运营，结合本场我想学习的方向，选出你觉得这场直播我最值得学习的方向，给我更详细的落地方案，不低于5000字。'
                this.extraRequire = '额外要求1：优化你上次输出结果里我不满意的地方，然后再将完整的分析结果完整保留后输出给我。反复检查你重新输出的结果和上次的分析结果，你重新输出的结果，不要有遗漏的内容。\n' +
                    '\n' +
                    '额外要求2：优化你上次输出结果里我不满意的地方。仅输出你优化过后的这个方向，不要输出其他内容了。输出更详细的执行落地方案，我是新手小白，给我5000字以上的落地方案。'
            },
            immediate:true,
            deep:true
        }
    },
    methods: {
        onCancel(){
            this.dialogVisible = false
        },
        open({modelType = 1} ){
            this.dialogVisible = true;
            this.$nextTick(()=>{
                this.$refs.model_type.setSelectedModel(modelType)
            })
        },
        answerAgain(){
            this.$emit('answerAgain', {
                feedbackValue: this.feedbackValue,
                extraRequire: this.extraRequire,
                modelType: this.modelType
            })
            this.onCancel()
        },
        modelChange(data){
            this.modelType = data;
        },
    },
    created() {

    },
    mounted() {

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
    }, //生命周期 - 销毁之前
    destroyed() {
    }, //生命周期 - 销毁完成
    activated() {
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>

<style lang="scss">
.prompt-dialog-list{
    .el-dialog__body{
        padding: 10px;
    }
    .el-dialog__header{
        padding: 10px;
        background: #F4F9FF;
    }
    .el-dialog__headerbtn{
        top: 10px;
    }
    .subTitle{
        font-weight: 400;
        color: #151719;
    }
}
</style>

<style lang='scss' scoped>

::v-deep(.buy-in-front) {
    .el-message-box__btns {
        border: 1px red solid;
    }
}

.group-box {
    height: 370px;
    overflow: hidden;
    overflow-y: auto;
    border-radius: 10px;
    border: 1px solid #DFEAF6;
    .group-box-item {
        padding: 10px;
        &:hover {
            background: #F7F7F7;
        }
    }

    .select-item {
        background: rgba(0, 0, 0, 0.1);
        cursor: not-allowed;
    }

    .not-select {
        color: #798AAD;;
    }
}

.is-select {
    border-color: var(--color-main) !important;
    color: var(--color-main) !important;
}
</style>