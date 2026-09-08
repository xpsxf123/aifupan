<template>
    <div>
        <div class="avatarUpdateContainer">
            <img :src="userInfo.avatar" class="avatar" v-if="userInfo.avatar">
            <img src="@/assets/imgs/default_avatar.png" class="avatar" v-else>

            <el-upload class="upload" :action="updateUrl" :data="updateData" :headers="updateHeader"
                :on-success="uploadAvatarSuccess" :file-list="fileList" :show-file-list="false">
                <afp-button size="medium">点击上传</afp-button>
            </el-upload>
        </div>

        <div class="userInfoItemContainer">
            <div class="userInfoItemLabel">昵称</div>
            <div class="userInfoItemValue">{{ userInfo.nickName }}</div>
            <div class="userInfoItemOperate" @click="updateNickName">编辑资料</div>
        </div>
        <div class="userInfoItemContainer">
            <div class="userInfoItemLabel">手机号码</div>
            <div class="userInfoItemValue">{{ userInfo.phone }}</div>
            <div class="userInfoItemOperate" @click="phoneDialogVisible = true">更换绑定手机号</div>
        </div>
        <div class="userInfoItemContainer">
            <div class="userInfoItemLabel">登录密码</div>
            <div class="userInfoItemValue">******</div>
            <div class="userInfoItemOperate" @click="onChangePasswrod">更换密码</div>
        </div>

        <div class="packageContainer" v-if="userInfo && userInfo.parentId === '0'">
            <div class="gratisPackageContainer">
                <div class="userInfoItemLabel">兑换码：</div>
                <div style="color: var(--color-main);cursor: pointer;" @click="exchange">兑换权益</div>
            </div>

            <div class="gratisPackageContainer" style="margin-top: 26px;" v-if="userInfo.packageLevel == 0">
                <div class="userInfoItemLabel">当前版本</div>
                <div v-if="userInfo.logoImgAddress">
                    <img class="packageLogoImg" :src="userInfo.logoImgAddress" />
                </div>
                <div v-else>{{ userInfo.packageName }}</div>
                <afp-button size="small" plain type="primary" style="margin-left: 34px;" @click="showQr">立即升级</afp-button>
            </div>
            <div class="notGratisPackageContainer" v-else>
                <div class="gratisPackageContainer">
                    <div class="userInfoItemLabel">当前版本</div>
                    <div v-if="userInfo.logoImgAddress">
                        <img class="packageLogoImg" :src="userInfo.logoImgAddress" />
                    </div>
                    <div v-else>{{ userInfo.packageName }}</div>
                </div>
                <div style="display: flex; align-items: center;">
                    <div class="userInfoItemLabel">有效期到</div>
                    <div v-if="userInfo.expirationDate">{{ userInfo.expirationDate.substring(0, 10) }}</div>
                    <afp-button size="medium" :plain="false" type="primary" style="margin-left: 34px;"
                        @click="showQr">立即续费</afp-button>
                </div>
            </div>
            <div class="gratisPackageContainer" style="margin-top: 26px;">
                <div class="userInfoItemLabel" style="width: 140px;">资源下次更新时间</div>
                <div>{{ resourceUpdateTime }}</div>
            </div>
        </div>

        <el-dialog title="验证原手机号" :visible.sync="phoneDialogVisible" width="320px" :close-on-click-modal="false">
            <el-form :model="phoneDataForm" :rules="phoneDataRole" ref="dataForm">
                <el-form-item prop="phone">
                    <el-input style="width: 265px;" v-model="phoneDataForm.phone" placeholder="原手机号"></el-input>
                </el-form-item>
                <el-form-item prop="code">
                    <el-input style="width: 165px;" v-model="phoneDataForm.code" placeholder="验证码"></el-input>
                    <span class="getCodeText" v-if="getCodeCountdown <= 0" @click="getCode">获取验证码</span>
                    <span class="getCodeText" v-else style="cursor:not-allowed;">{{ this.getCodeCountdown }}s</span>
                </el-form-item>
                <div class="loginBtn" @click="check">
                    验证
                </div>
            </el-form>
        </el-dialog>

        <el-dialog title="设置新手机号" :visible.sync="newPhoneDialogVisible" width="320px" :close-on-click-modal="false">
            <el-form :model="phoneDataForm" :rules="phoneDataRole" ref="dataForm">
                <el-form-item prop="phone">
                    <el-input style="width: 265px;" v-model="phoneDataForm.phone" placeholder="新手机号"></el-input>
                </el-form-item>
                <el-form-item prop="code">
                    <el-input style="width: 165px;" v-model="phoneDataForm.code" placeholder="验证码"></el-input>
                    <span class="getCodeText" v-if="getCodeCountdown <= 0" @click="getCode">获取验证码</span>
                    <span class="getCodeText" v-else style="cursor:not-allowed;">{{ this.getCodeCountdown }}s</span>
                </el-form-item>
                <div class="loginBtn" @click="updatePhone">
                    提交
                </div>
            </el-form>
        </el-dialog>

        <editPassword ref="editPasswrod"></editPassword>
        

        <!-- 客服弹窗 -->
        <customer-service-qr-code v-if="kefuDialogVisible" ref="customerServiceQrCode"></customer-service-qr-code>


    </div>
</template>

<script>
import myUtils from '../../../utils/utils';
import customerServiceQrCode from '../../commonComponent/customerServiceQrCode.vue';
import editPassword from './component/editPassword.vue';
export default {
    name: 'ReplayClientAccountSetup',
    components: { customerServiceQrCode,editPassword },
    data() {
        return {
            kefuDialogVisible: false,
            phoneDataForm: {
                phone: "",
                code: "",
            },
            getCodeCountdown: 0,
            getCodeInterval: null,
            phoneDialogVisible: false,
            newPhoneDialogVisible: false,
            phoneDataRole: {
                code: [
                    {
                        required: true,
                        message: "验证码不能为空",
                        trigger: "blur",
                    }
                ],
                phone: [
                    {
                        required: true,
                        message: "手机号不能为空",
                        trigger: "blur",
                    }, {
                        pattern: /^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/,
                        message: "请填写正确的手机号码",
                        trigger: "blur",
                    }
                ],
            },
            fileList: [],

            userInfo: {
                id: "",
                avatar: "",
                avatarImgId: "",
                phone: "",
                nickName: "",
            },
        };
    },
    computed: {

        updateHeader() {
            return { token: this.$store.state.token }
        },
        updateUrl() {
            return this.$httpBack.uploadUrl;
        },
        updateData() {
            return { flag: 0 }
        },
        resourceUpdateTime(){
            if(this.userInfo.resourceUpdateTime){
                return this.userInfo.resourceUpdateTime?.split(' ')[0];
            }else{
                return '-'
            }
        }
    },

    mounted() {
        this.getUserInfo();
    },

    methods: {
        onChangePasswrod(){
            this.$refs.editPasswrod?.show({
                data: this.userInfo
            })
        },
        showQr() {
            this.kefuDialogVisible = true;
            this.$nextTick(() => {
                this.$refs.customerServiceQrCode.init()
            })
        },
        Renew(){
            this.$confirm('请在新打开的页面上进行支付，支付完成后再关闭此窗口', '支付结果', {
                confirmButtonText: '已完成支付',
                cancelButtonText: '未支付成功',
                customClass: 'confirm-common-box confirm-row-reverse',
                closeOnClickModal: false
            }).then(() => {
                //TODO...
                this.$message({
                    type: 'success',
                    message: '支付成功!'
                });
            }).catch(() => {
                this.$message({
                    type: 'info',
                    message: '已取消支付!'
                });
            });
        },
        // 兑换邀请码
        exchange() {
            this.$prompt('请输入邀请码', '兑换权益', {
                confirmButtonText: '确定',
                cancelButtonText: '取消'
            }).then(({ value }) => {
                this.$httpBack.invitationCode.exchange({ code: value }).then(res => {
                    if (res.code == 0) {
                        this.$message.success("兑换成功，权益将在一分钟内生效");
                        window.location.reload()
                    }
                })
            })
        },
        // 升级版本
        upgrade() {
            this.$emit("updateMenuIndex", 6);
        },
        // 修改手机号
        updatePhone() {
            this.phoneDataForm.isNewPhone = 1;
            this.$httpBack.user.checkPhoneCode(this.phoneDataForm).then(res => {
                if (res.code == 0) {

                    let requestData = { phone: this.phoneDataForm.phone };
                    this.updateUserInfo(requestData);

                    this.phoneDataForm = {
                        phone: "",
                        code: ""
                    }
                }
            });
        },
        // 校验验证码
        check() {
            this.$httpBack.user.checkPhoneCode(this.phoneDataForm).then(res => {
                if (res.code == 0) {
                    this.phoneDataForm = {
                        phone: "",
                        code: ""
                    }
                    this.getCodeCountdown = 0;
                    clearInterval(this.getCodeInterval);
                    this.getCodeInterval = null;
                    this.phoneDialogVisible = false;
                    this.newPhoneDialogVisible = true;
                }
            })
        },
        // 获取验证码
        getCode() {
            if (this.getCodeCountdown > 0) {
                return;
            }

            var reg_tel = /^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/;
            if (!this.phoneDataForm.phone || !reg_tel.test(this.phoneDataForm.phone)) {
                this.$message.error("请填写正确的手机号");
                return;
            }

            this.$httpBack.user.getPhoneCode({ phone: this.phoneDataForm.phone }).then(res => {
                if (res.code == 0) {
                    this.$message.success("获取成功，请留意短信");
                }
            });

            this.getCodeCountdown = 60;
            // 启动定时器
            this.getCodeInterval = setInterval(() => {
                // 创建定时器，每1秒执行一次
                this.getCodeCountdown -= 1;
                if (this.getCodeCountdown <= 0) {
                    clearInterval(this.getCodeInterval);
                    this.getCodeInterval = null;
                }
            }, 1000);
        },
        // 获取用户信息
        getUserInfo() {
            this.$httpBack.user.infoByClient({}).then(res => {
                if (res.code == 0 && res.data) {
                    this.userInfo = res.data;
                    this.$store.commit("saveUserInfo", res.data);
                }
            });
        },
        // 修改用户信息
        updateUserInfo(requestData) {
            requestData.id = this.userInfo.id;
            this.$httpBack.user.updateByClient(requestData).then(res => {
                if (res.code == 0 && res.data) {
                    this.userInfo = res.data;
                    this.$store.commit("saveUserInfo", res.data);
                    this.$emit("updateUserInfo", "");
                    this.newPhoneDialogVisible = false;
                }
            });
        },
        // 修改昵称
        updateNickName() {
            this.$prompt('请输入新的昵称', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                inputPattern: /^[\u4e00-\u9fa5A-Za-z0-9]{1,20}$/,
                inputErrorMessage: '昵称只能包含中文、英文、数字'
            }).then(({ value }) => {
                let requestData = { nickName: value };
                this.updateUserInfo(requestData);
            });
        },
        // 上传头像
        uploadAvatarSuccess(response, file, fileList) {
            if(response.code!==0){
                return this.$message.error(response.msg);
            }
            this.userInfo.avatar = response.data.url;
            this.userInfo.avatarImgId = response.data.id;
            let recordDate = { avatarImgId: this.userInfo.avatarImgId };
            this.updateUserInfo(recordDate);
        },
    },
};
</script>

<style lang="scss">

</style>

<style scoped>
.packageLogoImg {
    width: 80px;
}

.notGratisPackageContainer {
    font-size: 14px;
    color: #677583;
}

.gratisPackageContainer {
    display: flex;
    align-items: center;
    font-size: 14px;
    color: #677583;
    margin-bottom: 24px;
}

.packageContainer {
    margin-top: 30px;
    border-top: 1px solid #DCE0E7;
    width: 454px;
    padding-top: 23px;
}

.loginBtn {
    width: 265px;
    height: 44px;
    background: var(--color-main);
    border-radius: 4px;
    font-weight: 500;
    font-size: 14px;
    color: #FFFFFF;
    text-align: center;
    line-height: 44px;
    cursor: pointer;
}

.getCodeText {
    margin-left: 10px;
    width: 90px;
    font-size: 14px;
    color: #2E3742;
    cursor: pointer;
    height: 40px;
    border-radius: 4px;
    border: 1px solid #DCE0E7;
    text-align: center;
    line-height: 40px;
    display: inline-block;
    box-sizing: border-box;
}

.userInfoItemOperate {
    width: 102px;
    color: #0B7CFF;
    cursor: pointer;
}

.userInfoItemValue {
    width: 110px;
    color: #2E3742;
}

.userInfoItemLabel {
    width: 92px;
    color: #677583;
}

.userInfoItemContainer {
    margin-top: 26px;
    display: flex;
    align-items: center;
    font-size: 14px;
}

.upload {
    margin-left: 16px;
}

.avatar {
    width: 56px;
    height: 56px;
    border-radius: 50%;
}

.avatarUpdateContainer {
    display: flex;
    align-items: center;
}

</style>