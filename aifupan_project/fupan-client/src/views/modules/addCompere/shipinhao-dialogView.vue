<template>
    <dialog-box :visible.sync="dialogVisible" custom-class="shipinhao-dialog-img" :close-on-click-modal="false" width="640px">
        <div class="overflow_hidden overflow_auto_y h100 shipinhao-dialog-img-box text-center">
            <!-- src\assets\imgs\new\sphjc.png -->
            <img style="max-width: 100%;" src="~@/assets/imgs/new/sphjc.png" alt="">
        </div>
    </dialog-box>
</template>

<script>
import DialogBox from '/src/components/dialog/index'
import dialogMixin from '/src/mixins/dialog';
export default {
    components: {
        DialogBox,
    },
    mixins: [dialogMixin],
    props: {
    },
    provide() {
        return {
        }
    },
    data() {
        return {
        };
    },
    computed: {
       
    },
    watch: {},
    methods: {
        onCancel() {
            this.hide();
        },
        async showCallback() {
        },
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
<style lang='scss'>
.shipinhao-dialog-img{
    max-height: 100vh !important;
    .el-dialog__body{
        padding: 12px 0 !important;
    }
    .shipinhao-dialog-img-box{
        height: calc(100vh - 50px);
        
    }
}
</style>