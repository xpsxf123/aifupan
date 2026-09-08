<template>
    <div class="statusLeftContainer">
        <slot>
            <slot name="titleLeft"></slot>
            <div>
                可用智能复盘时长：<span> {{ retainDecimals(getAiAnalvsisTime/60) }}</span>时长
            </div>
            <div>
                <el-button type="text" size="small" style="padding: 0px;" @click="$emit('leftClick')">立即扩容</el-button>
            </div>
            <div>
                已检测：<span>{{ detectionTime }}</span>
            </div>
        </slot>
    </div>
</template>

<script>
import myUtils from '/src/utils/utils';
export default {
components: {},
props:{
    detectionTime: {
        type: [Number,String],
        default: ''
    },
    aiAnalysisTime: {
        type:Number,
        default: 0
    }
},
data() {
    return {
    };
},
computed: {
    getAiAnalvsisTime(){
        return this.$store.getters?.getUserproperty?.aiAnalysisTime || 0
    }
},
watch: {},
methods: {
    retainDecimals: myUtils.retainDecimals,
    // getUserProperty
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
.statusLeftContainer {
    font-size: 13px;
    color: #2E3742;
    display: flex;
    // align-items: center;
    // justify-items: center;
    margin: 0 -10px;
    >div{
        padding: 0 10px;
        >span{
            padding: 0 3px;
        }
    }
    .el-button{
        font-size: 13px;
    }
}
</style>