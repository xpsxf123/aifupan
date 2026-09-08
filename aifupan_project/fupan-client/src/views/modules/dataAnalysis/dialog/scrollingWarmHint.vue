<template>
    <WarmHint ref="warmHint" @left-click="warmLeftClick" @right-click="warmRightClick"  :rightBt="getRightBt" leftBt="知道了">
       <div v-if="isActivate || isFree">
            您当前版本为<span class="text-colorTheme">{{getVersionName}}</span>，弹幕监控直播间数量<span class="text-colorErr">暂无授权</span>，请升级到高版本后再进行操作
       </div>
       <div v-else > 
            您的弹幕监控直播间数量<span  class="text-colorErr">授权不足</span>，请关闭其他直播间弹幕监控，或<span class="text-colorTheme">立即扩容</span>
       </div>
    </WarmHint>
</template>

<script>
import WarmHint from '@/components/warmHint/index.vue';
export default {
    components: {WarmHint},
    props:{
        
    },
    inject:['parent'],
    data() {
        return {
            
        };
    },
    computed: {
        // -1 激活版 0免费版 10个人 20企业 30旗舰
        getPackageLevel(){
            return this.$store.state?.userInfo?.packageLevel
        },
        // 激活版
        isActivate(){
            return this.getPackageLevel === -1;
        },
        // 免费版
        isFree(){
            return this.getPackageLevel === 0;
        },
        getVersionName(){
            if(this.isActivate){return '激活版'}
            if(this.isFree){return '免费版'}
            return ''
        },
        getRightBt(){
            if(this.isActivate||this.isFree){
                return '立即升级'
            }else{
                return '立即扩容'
            }
        }
    },
    watch: {},
    methods: {
        show(){
            this.$refs.warmHint.show();
        },
        warmLeftClick(){
            this.$refs.warmHint?.hide();
        },
        warmRightClick(){
            this.warmLeftClick();
            this.parent?.$refs?.customerServiceQrCode?.init()
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