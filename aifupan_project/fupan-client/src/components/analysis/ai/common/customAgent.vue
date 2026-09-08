<template>
    <div class="custom_agent">
        <el-dialog
            :visible.sync="customAgentVisible"
            :close-on-click-modal="false"
            :close-on-press-escape="false"
            :show-close="false"
            center>
            <div class="content">
                <div class="item">1. 个性化解决日常运营核心关注点</div>
                <div class="item">2.个性化整合运营经验和认知特点</div>
                <div class="item">3.个性化整合日常运营报表习惯</div>
                <div class="item">4.个性化整合细分行业特有运营逻辑</div>
                <div class="item">5.个性化解决直播日常运营问题</div>
            </div>

            <span slot="footer" class="dialog-footer">
                <afp-button type="primary" size="default" :plain="false" @click="agentCancel">不定制</afp-button>
                <afp-button type="primary" size="default" @click="agentOk"
                            class="agent_ok">去定制</afp-button>
            </span>
        </el-dialog>
    </div>
</template>

<script>
export default {
    components: {},
    inject: ['APP'],
    props: {},
    data() {
        return {
            customAgentVisible: false
        };
    },
    computed: {},
    watch: {},
    methods: {
        showCustomAgent() {
            this.customAgentVisible = true
        },
        agentCancel(){
            this.customAgentVisible = false
        },
        agentOk(){
            this.customAgentVisible = false
            this.APP?.showQrCode?.()
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
.custom_agent {
    ::v-deep(.el-dialog) {
        box-shadow: none;
        width: 328px;
        height: 431px;
        background: url("~@/assets/imgs/2_5_8/custom_agent.png") no-repeat center center;
    }

    .content {
        padding-top: 140px;

        .item {
            margin-top: 8px;
            margin-left: 25px;
            color: #4A3FBD;
        }
    }

    .agent_ok {
        border: none;
        color: #B9660C;
        background: linear-gradient(90deg, #FCE9B4 0%, #FECE81 100%);
        margin-left: 36px;
        &:hover {
            color: #B9660C;
            background: linear-gradient(90deg, #FCE9B4 0%, #FECE81 100%);
        }
    }
}
</style>