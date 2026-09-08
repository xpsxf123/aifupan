<template>
    <dialog-box :visible.sync="dialogVisible" class="mark-dialog-box" :close-on-click-modal="false" width="540px">
        <div class="pd-10">
            <div class="text-center mg-b12"><img src="@/assets/imgs/hint.png" alt="" srcset=""></div>
            弹幕监控对电脑和服务器性能要求较高，如果直播间的弹幕过多，可能会超出您电脑的硬件性能！建议不要开启太多直播间的弹幕监控！<br>
            还可监控弹幕直播间数量<span class="text-colorTheme">（{{useNum}}/{{countNum}}）</span>
            <div class="text-center pd-t20">
                <afp-button  @click="hide">暂不开启</afp-button>
                <afp-button  style="margin-left: 30px;" @click="onClick">{{ isBtText }}</afp-button>
            </div>
        </div>
    </dialog-box>
</template>

<script>
import DialogBox from '/src/components/dialog/index'
import dialogMixin from '/src/mixins/dialog'
export default {
    components: {
        DialogBox
    },
    mixins: [dialogMixin],
    props: {
        
    },
    data() {
        return {
            useNum: 0,
            countNum: 0
        };
    },
    computed: {
        isDel(){
            // 使用位超过可用位
            return this.useNum >= this.countNum
        },
        isBtText(){
            return this.isDel ? '立即关闭' : '立即开启'
        }
    },
    watch: {},
    methods: {
        showCallback(){
            const {useNum = 0,countNum = 0} = this.dialogData;
            this.useNum = useNum;
            this.countNum = countNum;
        },  
        onClick(){
            if(typeof this.dialogData?.callback === 'function' && !this.isDel){
                this.dialogData?.callback()
            }else{
                this.hide();
            }
        }
    },
    created() {

    },
    mounted() {
    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() { }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() { }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
</style>