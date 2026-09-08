<template>
    <el-dialog
        :title="dialogTitle"
        class="buyIn"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        :before-close="handleDialogClose"
        :visible.sync="dialogVisible"
        width="520px">
        <Authorize :unAuthorizedList="unAuthorizedList" v-if="isShowUnAuthorizedList"/>
        <template v-else>
            <div class="content">
                <div><i class="icon el-icon-warning"></i></div>
                <span class="text">{{ showText }}</span>
            </div>
            <div slot="footer" class="dialog-footer">
                <afp-button size="medium" @click="handleDialogClose">关 闭</afp-button>
                <afp-button size="medium" type="primary" :plain="false" style="padding-inline: 18px" @click="authorizeAction">点我授权</afp-button>
            </div>
        </template>
    </el-dialog>
</template>

<script>
import Authorize from './components/authorize.vue'
import myUtils from "@/utils/utils";
import buyIn from "@/mixins/buyIn";

// 巨量授权成功自动关闭弹窗的监听别名，用于多监听模式下唯一标识与关闭时移除
const BUY_IN_AUTO_CLOSE_TASK = 'buyInAutoClose';

export default {
    components: {Authorize},
    mixins: [buyIn],
    props: {
        showText: {
            type: String,
            default: '检测到本账号的巨量百应授权失效、会导致无法同步数据以进行AI数据诊断'
        },
    },
    data() {
        return {
            dialogTitle: '友情提示',
            dialogVisible: false,
            unAuthorizedList: [],
            isShowUnAuthorizedList: true,
            dimensionSecUid: ''
        };
    },
    computed: {},
    watch: {},
    methods: {
        open(list) {
            if (!this.$store.state.loadRouterFlag) return
            this.isShowUnAuthorizedList = true
            this.unAuthorizedList = list
            myUtils.dailySession.set('buyIn');
            this.dialogVisible = true
            // 打开弹窗时用别名注册巨量授权成功监听，成功后自动关闭本弹窗；
            // 不通过 watchAuthorizedBuyInSuccess 注册，避免与页面组件的成功提示重复
            this.$CSharpNotify.addTask('juliangAuthSuccess', () => {
                this.handleDialogClose()
            }, undefined, BUY_IN_AUTO_CLOSE_TASK)
        },
        openDimension(secUid) {
            this.isShowUnAuthorizedList = false
            this.dialogVisible = true
            this.dimensionSecUid = secUid
        },
        authorizeAction() {
            this.dialogVisible = false
            this.buyInFront(this.dimensionSecUid)
        },
        handleDialogClose() {
            this.dialogVisible = false
            // 关闭时移除自动关闭监听，避免永久监听在后续授权成功时误触发
            this.$CSharpNotify.removeTask('juliangAuthSuccess', BUY_IN_AUTO_CLOSE_TASK)
            this.$root.$emit('buyIn-dialog-closed')
        }
    },
    created() {

    },
    mounted() {

    },
    beforeCreate() {
    }, //生命周期 - 创建之前
    beforeMount() {
    }, //生命周期 - 挂载之前
    beforeUpdate() {
    }, //生命周期 - 更新之前
    updated() {
    }, //生命周期 - 更新之后
    beforeDestroy() {
    }, //生命周期 - 销毁之前
    destroyed() {
    }, //生命周期 - 销毁完成
    activated() {
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.buyIn {
    .content {
        display: flex;
        align-items: flex-start;
        padding: 12px 20px;

        .icon {
            font-size: 16px;
            color: #FFCC32;
        }

        .text {
            margin-left: 6px;
        }
    }

    ::v-deep(.el-dialog__body) {
        padding: 0;
    }

    ::v-deep(.el-dialog__title) {
        font-weight: bold;
    }

    .dialog-footer {
        text-align: center;
    }
}

</style>