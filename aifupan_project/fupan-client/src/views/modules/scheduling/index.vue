<template>
    <div class="scheduling">
        <div class="item item1">
            <img src="@/assets/imgs/2_6_0/1.png" alt="">
            <el-button type="text" class="text" @click="openAdmin">前往查看
                <img src="@/assets/imgs/2_6_0/right.png" alt="" class="right">
                <img src="@/assets/imgs/2_6_0/right.png" alt="" class="right">
            </el-button>
        </div>
        <div class="item item2">
            <img src="@/assets/imgs/2_6_0/2.png" alt="">
            <el-button type="text" class="text" @click="openAdmin">前往查看
                <img src="@/assets/imgs/2_6_0/right.png" alt="" class="right">
                <img src="@/assets/imgs/2_6_0/right.png" alt="" class="right">
            </el-button>
        </div>
        <div class="item item3">
            <img src="@/assets/imgs/2_6_0/3.png" alt="">
            <el-button type="text" class="text" @click="openAdmin">前往查看
                <img src="@/assets/imgs/2_6_0/right.png" alt="" class="right">
                <img src="@/assets/imgs/2_6_0/right.png" alt="" class="right">
            </el-button>
        </div>
        <div class="item item4">
            <img src="@/assets/imgs/2_6_0/4.png" alt="">
            <el-button type="text" class="text" @click="openAdmin">前往查看
                <img src="@/assets/imgs/2_6_0/right.png" alt="" class="right">
                <img src="@/assets/imgs/2_6_0/right.png" alt="" class="right">
            </el-button>
        </div>
        <div class="item item5">
            <img src="@/assets/imgs/2_6_0/5.png" alt="">
            <el-button type="text" class="text" @click="openAdmin">前往查看
                <img src="@/assets/imgs/2_6_0/right.png" alt="" class="right">
                <img src="@/assets/imgs/2_6_0/right.png" alt="" class="right">
            </el-button>
        </div>
        <div class="item item6">
            <img src="@/assets/imgs/2_6_0/6.png" alt="">
            <el-button type="text" class="text" @click="openAdmin">前往查看
                <img src="@/assets/imgs/2_6_0/right.png" alt="" class="right">
                <img src="@/assets/imgs/2_6_0/right.png" alt="" class="right">
            </el-button>
        </div>
        <afp-button @click="openAdmin" type="primary" :plain="false" size="default" class="open_admin">
            点我打开企业直播管理后台
        </afp-button>
        <customer-service-qr-code ref="customerServiceQrCode">
            <div class="text-base text-center">
                <div class="color-red">当前为 {{ getVersionName }}，暂无权限使用此功能</div>
                <div style="color: #151719">请联系产品顾问升级版本</div>
            </div>
        </customer-service-qr-code>
    </div>
</template>

<script>


import CustomerServiceQrCode from "@/views/commonComponent/customerServiceQrCode.vue";

export default {
    components: {CustomerServiceQrCode},
    data() {
        return {}
    },
    computed: {
        getLevel() {
            return this.$store.getters.getPackageLevel
        },
        getPackageLevel() {
            return this.$store.state?.userInfo?.packageLevel
        },
        getVersionName() {
            return this.$store.getters.getPackageLevelName
        },
    },
    methods: {
        openAdmin() {
            if (this.getLevel < 20) {
                this.$refs.customerServiceQrCode.init();
            } else {
                if (this.$httpClient?.system?.openGovernanceWeb) {
                    this.$httpClient.system.openGovernanceWeb()
                    return
                }
                // let url = ''
                // if (window.SITE_CONFIG.env === 'production') {
                //     url = 'https://ent.aifupan.com.cn'
                // } else if (window.SITE_CONFIG.env === 'release') {
                //     url = 'https://yzent.aifupan.com.cn'
                // } else if (window.SITE_CONFIG.env === 'test') {
                //     url = 'https://testent.aifupan.com.cn'
                // }
                // this.$httpClient.system.openUrl({url: `${url}?token=${this.$store.getters.getToken}`});
            }
        }
    }
};
</script>

<style scoped lang="scss">
.scheduling {
    .item {
        position: relative;

        img {
            width: 100%;
        }

        .text {
            position: absolute;
            left: 12.2%;
            bottom: 26%;
            font-size: 16px;

            .right {
                width: 14px;
            }
        }
    }

    .item2, .item4, .item6 {
        .text {
            position: absolute;
            right: 27.1%;
            left: auto;
            bottom: 26%
        }
    }

    .open_admin {
        width: 650px;
        font-size: 24px!important;
        height: 62px !important;
        border-radius: 62px !important;
        position: fixed;
        bottom: 0;
        left: 55%;
        /* 同时实现水平和垂直居中 */
        transform: translate(-50%, -55%);
    }
}

</style>
