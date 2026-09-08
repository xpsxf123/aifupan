<!--
@description 通用折叠把手组件：用于右侧内容区在“折叠/展开”两种动作间切换，支持默认自增模式与外部图标态控制。
-->
<template>
    <div class="fold-box flex-ai-c flex-jc-c cs-p"  @click="setFlod">
        <i v-if="isExpandIcon" class="font_family icon-a-Frame566"></i>
        <i v-else class="font_family icon-a-Frame567"></i>
    </div>
</template>

<script>
/**
 * @description 通用折叠把手：默认按 0 -> max 循环发出 change 事件；当外部传入 expandMode 时，仅接管图标展示，不改变原事件语义。
 */
export default {
    components: {},
    props:{
        max:{
            type:Number,
            default: 0
        },
        expandMode: {
            type: Boolean,
            default: undefined
        }
    },
    data() {
        return {
            flod: 0
        };
    },
    computed: {
        /**
         * @description 计算当前图标是否展示为“展开”态，优先使用外部传入的 expandMode。
         * @returns {boolean}
         */
        isExpandIcon() {
            if (typeof this.expandMode === 'boolean') {
                return this.expandMode
            }
            return this.flod === this.max
        }
    },
    watch: {},
    methods: {
        /**
         * @description 切换折叠序号，并向父组件同步当前序号。
         * @returns {void}
         */
        setFlod(){
            this.flod++;
            if(this.flod>this.max){
                this.flod = 0;
            }
            this.$emit('change', this.flod);
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
.fold-box{
    opacity: 0;
    position: absolute;
    height: 60px;
    width: 30px;
    border-radius: 0 10px 10px 0;
    top: 50%;
    margin-top: -60px;
    left: -30px;
    transition: all 0.3s;
    background: #EAEBF0;
}
</style>
