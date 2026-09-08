<template>
    <div class="codeImgContainer">
        <img :src="imgInfo.url" class="serverCodeImg">
        <div style="display: flex; align-items: center; flex-direction: column;" v-if="userInfo.packageLevel == -1">
            <div style="margin-top: 16px;color: #666;">微信扫码添加产品顾问微信</div>
            <div style="margin-top: 8px;color: #666;">领取免费试用资格</div>
        </div>
        <div style="display: flex; align-items: center; flex-direction: column;" v-else>
            <div style="margin-top: 16px;color: #666;">微信扫码添加企业客服微信</div>
            <div style="margin-top: 8px;color: #666;">咨询更多工具使用问题</div>
        </div>
    </div>
</template>

<script>
export default {

    data() {
        return {
            imgInfo: {},
            userInfo: {
                packageLevel: -1
            },
        };
    },

    mounted() {
        this.getCodeImg();
    },

    methods: {
        getCodeImg() {
            this.imgInfo = {};
            this.$httpBack.user.infoByClient({}).then(res => {
                if (res.code == 0 && res.data) {
                    this.userInfo = res.data;
                    this.$httpBack.file.getCurrentUserSale().then((res) => {
                        if (res.code == 0) {
                            this.imgInfo = res.data?.qrcodeImgInfo;
                        }
                    });
                }
            });
        }
    },
};
</script>
<style>
.serverCodeImg {
    width: 380px;
    height: 380px;
}

.codeImgContainer {
    height: calc(100% - 40px);
    display: flex;
    flex-direction: column;
    align-items: center;
    /* margin-top: 50px; */
}
</style>