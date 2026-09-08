<template>
    <div class="bar-chart-content font-s12 text-colorMain"  style="height: 100%;">
        <div style="height: 100%;" class="flex-ai-c flex-jc-c">
            <div>
                <span v-if="label" class="mg-r4">{{ label }}</span>
                <slot></slot>
            </div>
            <el-popover
                    v-if="chatData"
                    :placement="placement"
                    width="400"
                    trigger="click"
                    @after-enter="afterEnter"
                    @after-leave="afterLeave"
                >
                <div style="width: 100%;height: 240px;">
                    <Title>{{ label }}</Title>
                    <Chat v-if="showChat" isPopover :compareData="compareData" :datas="chatData"></Chat>
                </div>
                <span  slot="reference" :id="id">
                    <img class="bar-chat-img mg-l8" src="@/assets/imgs/barChart.png" alt="" srcset="">
                </span>
            </el-popover>
        </div>
        <slot name="after"></slot>
    </div>
</template>

<script>
import Chat from './chat.vue';
 import Title from '@/components/title/index.vue'
export default {
    components: {Chat, Title},
    props:{
        placement:{
            type: String,
            default: 'top-start'
        },
        chatData: {
            type: Array,
            default: null
        },
        label: {
            type:String,
            default: ''
        },
        compareData: {
            type: Object,
            default: () => {
                return {}
            }
        },
        id: {
            type: String,
            default: ''
        }
    },
    data() {
        return {
            showChat: false
        };
    },
    computed: {},
    watch: {},
    methods: {
        afterEnter(){
            this.$nextTick(() => {
                this.showChat = true
            });
        },
        afterLeave(){
            this.$nextTick(() => {
                this.showChat = false
            });
        },
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
.bar-chat-img{
    margin-top: 2px;
    max-width: 16px;
}
</style>