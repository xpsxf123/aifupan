<template>
    <div>
        <div class="ai-title dialogue-box mg-t10 pd-6" style="max-width: 100% !important;width: 100%; background: rgba(255,140,0,0.1) !important;">
            <h3 v-if="title" class="mg-0 pd-t4 pd-b4 w100" :class="{'text-center': title && !html}">分析：{{ title }}</h3>
            <div v-if="!isTooltip && html" v-html="html" class="mg-t20 font-s12"></div>
            <template v-else-if="isTooltip && html">
                <el-tooltip class="item" effect="dark" placement="top">
                    <div slot="content" style="width: 600px;">
                        <div class="font-s12" v-html="html"></div>
                    </div>
                    <div :class="{'slh': isTooltip}">
                        <span class="font-s12" v-html="html"></span>
                    </div>
                </el-tooltip>
            </template>
        </div>
        <!-- <div class="flex-jc-sb">
            <div>
                <el-button @click="ohter" type="text" >对({{ title }})"提出问题"</el-button>
            </div>
            <div></div>
        </div> -->
    </div>
</template>

<script>

export default {
    components: {},
    props:{
        isTooltip: {
            type: Boolean,
            default: false
        },
        html: {
            type: String,
            default: ''
        },
        title: {
            type: String,
            default: ''
        },
        notProblem: {
            type: Boolean,
            default: false
        }
    },
    inject: ['AiVnode'],
    data() {
        return {
        
        };
    },
    computed: {},
    watch: {
    },
    methods: {
        ohter(){}
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