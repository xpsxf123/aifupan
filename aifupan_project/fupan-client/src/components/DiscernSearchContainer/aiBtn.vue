<template>
    <div class="flex-jc-e pd-r10 pd-t10">
        <showOther v-model="dataDisplay" :sentenceMarkData="sentenceMarkData" :barrageNum="barrageNum" :isCompare="isCompare" :hide="hide" @change="otherChange" :aiCueType="aiCueType" :disabled="!isText"></showOther>
        <el-radio-group v-if="!notTextType" v-model="textType" @change="textTypeChange" size="medium">
            <el-radio-button label="aiSharding">AI脚本拆解</el-radio-button>
            <el-radio-button label="aiOptimal">优化原文</el-radio-button>
            <el-radio-button label="text">分钟段落</el-radio-button>
<!--            <el-radio-button label="aiDiagnose">AI数据诊断</el-radio-button>-->
        </el-radio-group>
    </div>
</template>

<script>
import showOther from './showOther.vue'
import {trackEvent} from "@/utils/laTrack";
export default {
    components: {
        showOther
    },
    props:{
        value: {
            type: String,
            default: ''
        },
        notTextType:{
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
            dataDisplay:{},
            textType: 'text'
        };
    },
    computed: {
        isText(){
            return this.textType === 'text'

        },
        getTextType:{
            get(){
                return this.value || this.textType
            },
            set(val){
                this.$emit('input',val)
                this.textType = val
            }
        }
    },
    watch: {},
    methods: {
        textTypeChange(val){
            this.$emit('change',val)
            if (val === 'aiSharding') {
                trackEvent('P003_A0057')
            } else if (val === 'aiOptimal') {
                trackEvent('P003_A0058')
            }
        },
        otherChange(){
            this.$emit('other-change',this.dataDisplay)
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

</style>
