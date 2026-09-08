<template>
    <el-dialog :visible.sync="visible" :width="width" top="280px" :close-on-click-modal="false"  :custom-class="customClass" :append-to-body="true">
        <div class="customer-service-close"><img src="../../assets/imgs/close.png" @click="customerServiceCloseClick"
                style="width: 16px;height: 16px;"></div>
        <div class="customerServiceContainer">
            <slot>
                <div class="customerServiceTitle">请扫码添加企业微信沟通</div>
            </slot>
            <img :src="imgInfo?.url" class="codeImg">
            <slot name="footer"></slot>
        </div>
    </el-dialog>

</template>

<script>
export default {
    props: {
        width: {
            type: String,
            default: '330px'
        },
        customClass: {
            type: String,
            default: ''
        }
    },
    data() {
        return {
            visible: false,
            imgInfo: {},
        };
    },

    mounted() {
    },

    methods: {
        init() {
            this.visible = true;
            this.getCodeImg();
        },
        getCodeImg() {
            if(!this.$store.state.loadRouterFlag){return}
            this.imgInfo = {};
            this.$httpBack.user.infoByClient({}).then(res => {
                if (res.code == 0 && res.data) {
                    this.$httpBack.file.getCurrentUserSale().then((res) => {
                        if (res.code == 0) {
                            this.imgInfo = res.data?.qrcodeImgInfo;
                        }
                    });
                }
            });
        },
        customerServiceCloseClick() {
            this.visible = false
        }
    },
};
</script>
<style scoped>
.codeImg {
    width: 256px;
    height: 256px;
    margin-top: 16px;
}

.customerServiceTitle {
    font-weight: 600;
    font-size: 16px;
    color: #2E3742;
}

.customerServiceContainer {
    padding: 22px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    
    background-size: cover;
}

.customer-service-close {
    display: flex;
    justify-content: end;
    margin-right: 13px;
    padding-top: 13px;
    cursor: pointer;
}

::v-deep .el-dialog__body {
    padding: 0;
    background-image: url('~@/assets/imgs/1_9_30/qrBg.png');
}

::v-deep .el-dialog__header {
    display: none;
}
</style>