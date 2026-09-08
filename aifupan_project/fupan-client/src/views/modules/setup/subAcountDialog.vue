<template>
    <div>
        <el-dialog :title="!dataForm.id ? '添加子账号' : '编辑子账号'" :visible.sync="visible" width="456px"
            :close-on-click-modal="false">
            <el-form :model="dataForm" :rules="dataRule" label-width="100px" style="margin-top: 10px;" ref="dataForm">
<!--                <el-form-item label="登录账号" prop="subUserName">-->
<!--                    <el-input style="width: 280px;" v-model="dataForm.subUserName" placeholder="请输入账号"></el-input>-->
<!--                </el-form-item>-->
                <el-form-item label="账号昵称" prop="nickName">
                    <el-input style="width: 280px;" v-model="dataForm.nickName" placeholder="请输入昵称"></el-input>
                </el-form-item>
                <el-form-item label="手机号码" prop="subPhone">
                    <el-input style="width: 280px;" v-model="dataForm.subPhone" placeholder="请输入手机号码"></el-input>
                </el-form-item>
                <el-form-item label="验证码" prop="code">
                    <el-input style="width: 180px;" v-model="dataForm.code" placeholder="验证码"></el-input>
                    <span class="getCodeText" v-if="getCodeCountdown <= 0" @click="getCode">获取验证码</span>
                    <span class="getCodeText" v-else style="cursor:not-allowed;">{{ getCodeCountdown }}s</span>
                </el-form-item>
            </el-form>

            <div style="display: flex;justify-content: center;padding-bottom: 20px;">
                <afp-button style="width: 100px;margin-right: 10px;" @click="visible = false">取消</afp-button>
                <afp-button type="primary" style="width: 100px;" @click="dataFormSubmit()">确定</afp-button>
            </div>
        </el-dialog>

        <el-dialog :visible.sync="lackVisible" width="384px" :close-on-click-modal="false">
            <div class="lack-dialog">
                <img src="../../../assets/imgs/Frame.png">
                <div style="color: #151917 ;font-size: 16px;margin: 30px 0px 70px 0px;">您的子账号授权不足</div>
            </div>
        </el-dialog>

    </div>
</template>

<script>

export default {
    data() {
        return {
            visible: false,
            lackVisible: false,
            getCodeCountdown: 0,
            dataForm: {
                id: '',
                subUserName: '',
                subPhone: '',
                nickName:'',
                code: '',
            },
            dataRule: {
                subUserName: [
                    { required: true, message: '请输入账号', trigger: 'blur' }
                ],
                subPhone: [
                    { required: true, message: '请输入手机号', trigger: 'blur' }
                ],
                code: [
                    { required: true, message: '请输入验证码', trigger: 'blur' }
                ]
            }
        }
    },
    methods: {
        // 获取验证码
        getCode() {
            if (this.getCodeCountdown > 0) {
                return;
            }

            var reg_tel = /^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/;
            if (!this.dataForm.subPhone || !reg_tel.test(this.dataForm.subPhone)) {
                this.$message.error("请填写正确的手机号");
                return;
            }

            this.$httpBack.user.getPhoneCode({ phone: this.dataForm.subPhone }).then(res => {
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
        init(id) {
            this.dataForm.id = id || 0;
            this.visible = true;
            this.$nextTick(() => {
                this.$refs["dataForm"].resetFields();
            });

        },
        dataFormSubmit() {
            this.$refs["dataForm"].validate((valid) => {
                if (valid) {
                    this.$cMsg.customConfirm({
                        message: '绑定的时候，如果子账号在录制中，为避免数据错误，会强制将子账号退出软件，确定绑定为子账号吗？',
                        confirmButtonText: '确认',
                        cancelButtonText: '取消',
                        customClass: 'sub-account-confirm confirm-btns-center',
                    }).then(() => {
                        let requestData = JSON.parse(JSON.stringify(this.dataForm));
                        this.$httpBack.subAccount.bind(requestData).then((res) => {
                            if (res && res.code == 0) {
                                this.$message({
                                    message: "绑定成功",
                                    type: 'success',
                                    duration: 1500,
                                    onClose: () => {
                                        this.visible = false
                                        this.$emit("refreshDataList");
                                    }
                                });
                            }
                        });
                    })
                }
            });
            
        },
        lack() {
            this.lackVisible = true
        },
    }
}
</script>

<style scoped>
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

::v-deep .el-dialog__body {
    padding: 0;
}

.lack-dialog {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    margin-top: 40px;
}
</style>