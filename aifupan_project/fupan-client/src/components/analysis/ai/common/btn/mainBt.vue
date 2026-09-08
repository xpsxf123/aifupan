<template>
    <afp-button type="primary" class="ai-main-Bt" size="default" :class="{'animation': isAnimation}" plain round  @click="onClick">
        <span class="font-s16 flex-ji-c">
             我自己来提问AI
            <span class="icon-box mg-l10">
                <i class="font_family icon-shangxia-4 font-s14"></i>
            </span>
        </span>
        <span class="mask"></span>
    </afp-button>
</template>

<script>

import {trackEvent} from "@/utils/laTrack";

export default {
    components: {},
    props:{
        isAnimation: {
            type: Boolean,
            default: false
        },
        isType: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {

        };
    },
    computed: {},
    watch: {},
    methods: {
        onClick(){
            this.$emit('click')
            trackEvent('P003_A0060')
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
.icon-box{
    width: 24px;
    height: 24px;
    background: var(--color-main);
    border-radius: 50%;
    color: #fff;
    line-height: 24px;
    display: inline-block;
}
@keyframes shrinkFromRight {
    from {
        width: 210px;
    }
    to {
        width: 42px;
    }
}
@keyframes spanWidth {
    from {
        width: 0;
        height: 0;
        top: 21px;
        left: 105px;
    }
    to {
        width: 42px;
        height: 42px;
        top: 0;
        left: 0;
    }
}
.ai-main-Bt{
    width: 210px;
    transform-origin: right;
    position: relative;
    height: 42px!important;
    color: var(--color-main) !important;
    border: 2px solid !important;
    background: #EEF8FF !important;
    border-color: linear-gradient(81deg, rgba(1, 195, 204, 1), rgba(0, 119, 255, 1), rgba(145, 93, 217, 1)) 2 2 !important;
    box-shadow: 0px 3px 6px 0px rgba(45,81,139,0.1), 0px 11px 11px 0px rgba(45,81,139,0.09), 0px 25px 15px 0px rgba(45,81,139,0.05), 0px 44px 18px 0px rgba(45,81,139,0.01), 0px 69px 19px 0px rgba(45,81,139,0);
    border-radius: 64px 64px 64px 64px !important;
    &:hover{
        border: 2px solid !important;
        background: #EEF8FF !important;
        border-color: linear-gradient(81deg, rgba(1, 195, 204, 1), rgba(0, 119, 255, 1), rgba(145, 93, 217, 1)) 2 2 !important;
        box-shadow: 0px 3px 6px 0px rgba(45,81,139,0.1), 0px 11px 11px 0px rgba(45,81,139,0.09), 0px 25px 15px 0px rgba(45,81,139,0.05), 0px 44px 18px 0px rgba(45,81,139,0.01), 0px 69px 19px 0px rgba(45,81,139,0);
        border-radius: 64px 64px 64px 64px !important;
    }
    .mask{
        position: absolute;
        top: 28px;
        left: 160px;
        width: 0;
        height: 0;
        z-index: 9999;
        background: linear-gradient(81deg, rgba(1, 195, 204, 1), rgba(0, 119, 255, 1), rgba(145, 93, 217, 1));
    }
}
.animation{
    animation: shrinkFromRight 1s ease-in-out forwards;
    overflow: hidden;
    .mask{
        border-radius: 50%;
        animation: spanWidth 1s ease-in-out forwards;
    }
}
</style>