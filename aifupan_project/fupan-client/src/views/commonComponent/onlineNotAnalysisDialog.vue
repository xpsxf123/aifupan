<template>
    <div>
        <el-dialog :visible.sync="visible" width="460px" :close-on-click-modal="false">
            <!-- <div class="video-mark-add-close"><img src="../../assets/imgs/close.png" style="width: 16px;height: 16px;"
                    @click="visible = false">
            </div> -->
            <div class="onlineTitle">分享复盘</div>
            <div class="onlineSubTitle">将视频上传至云空间以生成分享复盘</div>
            <div class="onlineItemContainer pd-l40">
                <div class="onlineItemLabel">空间容量</div>
                <div class="onlineItemText">{{ "已用 " + retainDecimals((userProperty.totalStorageNum -
                    userProperty.storageNum)
                    / 1024 / 1024) + "G / " + retainDecimals(userProperty.totalStorageNum / 1024 / 1024) + "G" }}</div>
            </div>
            <div class="onlineItemContainer pd-l40">
                <div class="onlineItemLabel">大小时长</div>
                <div class="onlineItemText">{{ "时长：" + (onlineFileInfo.duration || 0) + "分钟，预计消耗" + onlineFileInfo.fileSize +
                    "M" }}</div>
            </div>

            <div class="onlineTips">
                当前云空间容量不足，还请联系客服充值后再使用
            </div>

            <div class="codeImgContainer">
                <img :src="imgInfo.url" class="codeImg">
            </div>


            <div style="display: flex;justify-content: center; padding-bottom: 31px;margin-top: 23px;">
                <afp-button class="dialog-button add-yes" @click="visible = false">知道了</afp-button>
            </div>
        </el-dialog>
    </div>
</template>

<script>
import myUtils from '../../utils/utils';
export default {
    data() {
        return {
            visible: false,
            userProperty: {},
            onlineFileInfo: {},
            imgInfo: {},
        };
    },
    created() {
        this.getCodeImg();
    },
    methods: {
        getCodeImg() {
            this.imgInfo = {};
            this.$httpBack.file.getCurrentUserSale().then((res) => {
                if (res.code == 0) {
                    this.imgInfo = res.data?.qrcodeImgInfo;
                }
            });
        },
        init(userProperty, onlineFileInfo) {
            this.userProperty = userProperty;
            this.onlineFileInfo = onlineFileInfo;
            this.visible = true;
        },
        retainDecimals(val) {
            return myUtils.retainDecimals(val);
        },
    }
}
</script>

<style >
.codeImgContainer {
    display: flex;
    justify-content: center;
    margin-top: 30px;
}

.codeImg {
    width: 120px;
    height: 120px;

}

.onlineTips {
    font-size: 14px;
    color: #FF3A19;
    margin-top: 24px;
    display: flex;
    justify-content: center;
}

.onlineItemText {
    font-size: 14px;
    color: #2E3742;
    margin-left: 25px;
}

.onlineItemLabel {
    font-weight: bold;
    font-size: 14px;
    color: #2E3742;
}

.onlineItemContainer {
    display: flex;
    align-items: center;
    padding-left: 40px;
    margin-top: 24px;
}

.video-mark-add-close {
    display: flex;
    justify-content: end;
    margin-right: 13px;
    padding-top: 14px;
    cursor: pointer;
}

.onlineSubTitle {
    display: flex;
    justify-content: center;
    font-size: 14px;
    color: #677583;
    margin-top: 6px;
}

.onlineTitle {
    display: flex;
    justify-content: center;
    font-size: 16px;
    color: #2E3742;
}

.form-time {
    display: flex;
    align-items: center;
    margin: 19px 0px 0px 39px
}

.form-tiem-item {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 88px;
    height: 32px;
    border: 1px solid #CFD4DB;
    border-radius: 4px;
}

.select-bacc-color {
    display: flex;
    justify-content: center;
    position: absolute;
    color: #2E3742;
    z-index: 2;
    width: 66px;
    border-radius: 4px;
}

.select-color-red {
    background-color: red;
}

.select-color-yellow {
    background-color: yellow;
}

.select-color-blue {
    background-color: var(--color-main);
}

.dialog-button {
    width: 110px;
    height: 40px;
    border-radius: 4px;
    border: 1px solid #B4BCCA;
    color: 14px;
}

.add-yes {
    background-color: var(--color-main);
    color: #FFFFFF;
}
</style>