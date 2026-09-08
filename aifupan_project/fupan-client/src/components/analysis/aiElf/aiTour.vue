<template>
    <dialog-box class="aiTour-dialog-box" :visible.sync="dialogVisible" 
    :close-on-click-modal="false" :show-close="true" @closed="hideCallback" width="468px">
        <!-- <img style="max-width: 100%;" src="@/assets/imgs/aiTour.png" alt="" srcset=""> -->
        <!-- <div class="aiTour-hint-box flex-ji-c">
            <div class="aiTour-hint-content font-s18 flex-ji-c">
                <div class="aiTour-hint-text pd-t20">
                    <div class="pd-b6"><span class="text-colorTheme">AI运营助手</span>--把顶级运营带回家！</div>
                    <div class="pd-t6"><span class="text-colorErr">AI违规助手</span>--查违规准确率90%！</div>
                </div>
            </div>
        </div> -->
    </dialog-box>
</template>

<script>
import DialogBox from '/src/components/dialog'
import dialogMixin from '@/mixins/dialog.js'
export default {
    components: {DialogBox},
    mixins: [dialogMixin],
    props:{
    },
    data() {
        return {
        
        };
    },
    computed: {},
    watch: {},
    methods: {
        hideCallback(){
            this.$emit('hide')
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
.aiTour-dialog-box{
    ::v-deep(.el-dialog) {
        padding: 0;
        height: 402px;
        background-image: url('~@/assets/imgs/aiTour.png');
        background-size: cover;
        background-color: transparent;
        // box-shadow: none;
        .el-dialog__header{
            display: block;
        }
        .el-dialog__headerbtn{
            font-size: 28px !important;
        }
        // .aiTour-hint-box{
        //     padding-top: 222px
        // }
        // .aiTour-hint-content{
        //     height: 110px;   
        // }
        // .aiTour-hint-text{
            
        // }
    }
}
</style>