<template>
    <div class="loginContainer bg-c" :class="{'overflow_hidden': isMobile}" @contextmenu="preventRightClick">
        <!-- 左区域 -->
        <div v-if="!isMobile" class="loginLeftContainer flex-ai-c" :style="!isShowLoginBox?{width: '100%'}:{}">
            <div class="w100" style="display: inline-block;">
                <img class="logoImg" src="@/assets/imgs/theme/logo.png" />
                <div class="loginBack">
                    <img src="@/assets/imgs/aiImg.png" style="max-width: 715px;">
                </div>
                <!-- <div v-if="!isIframe"> -->
                <div class="pd-t20">
                    <div class="pd-l40 pd-r40" v-loading="slidesLoading">
                        <SwiperGroupLoop ref="swiperGroupLoop" :slides="slidesData" :group-size="5"></SwiperGroupLoop>
                        <div class="pd-t20">
                            <span class="font-s14 text-color2">*以上为部分重要合作伙伴</span>
                        </div>
                        <div class="pd-t20" v-if="!isShowLoginBox">
                            <span class="font-s14 text-color2">浏览器暂不支持登录页面，请</span>
                            <span class="font-s14 text-colorTheme cs-p" @click="goToOfficialWebsite">前往官网</span>
                            <span class="font-s14 text-color2">查看云空间，或分享查看云空间</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <!-- 右区域 -->
        <div v-if="isShowLoginBox" class="loginRightContainer" :style="isMobile?{width: '100%'}:{}">
            <!-- 工具栏 -->
            <!--      <div v-if="!isIframe" class="navContainer">-->
            <!--        <img class="navImg" src="@/assets/imgs/min.png" @click="minsize" />-->
            <!--        <img class="navImg" v-if="togglemaxsizeFlag == 'normal'" src="@/assets/imgs/max.png" @click="togglemaxsize" />-->
            <!--        <img class="navImg" v-else src="@/assets/imgs/normal.png" @click="togglemaxsize" />-->
            <!--        <img class="navImg" src="@/assets/imgs/close.png" @click="close" />-->
            <!--      </div>-->
            <div class="loginRightBacg" :style="isMobile?{boxShadow: 'none'}:{}">

                <!-- 登录 -->
                <div class="bodyContainer" v-if="loginOrRegister == 'login'">
                    <!-- 登录界面宣传语 -->
                    <div class="logintext">
                        <div class="font-s28">
                            <span class="font-s26">比<span class="text-colorTheme">claude</span>更懂直播<br></span>
                            <span style="display: block;margin-top: 8px;color: #5E81F4;">
                  <span class="onePlus">3万</span>
                  <span style="padding-left: 18px">头部直播间在用</span>
              </span>
                        </div>
                        <!-- <div>
                          <span>数据只是</span><span style="color: #5E81F4;margin-right:14px;">结果</span>
                          <span>内容才是</span><span style="color: #5E81F4;">核心</span>
                        </div>
                        <div style="margin-top: 8px;">
                          <span>复盘解决</span><span style="color: #5E81F4;margin-right:14px;">根本</span>
                          <span><span class="font-s40" style="color: #5E81F4;">AI</span>成就</span><span style="color: #5E81F4;">未来</span>
                        </div> -->
                    </div>
                    <!-- 登录类型 -->
                    <div class="loginTypeContainer">
                        <div  :class="loginType == 'password' ? 'loginTypeItemSelect' : 'loginTypeItem'"
                              @click="loginTypeChange('password')">账号密码登录</div>
                        <div :class="loginType == 'code' ? 'loginTypeItemSelect' : 'loginTypeItem'"
                             @click="loginTypeChange('code')">
                            验证码登录</div>
                    </div>
                    <!-- 账号密码登录 -->
                    <div class="formContainer" v-if="loginType == 'password'">
                        <el-form :model="dataForm" :rules="dataRule" ref="dataForm" @keyup.enter.native="login">
                            <el-form-item prop="username">
                                <el-input style="width: 265px;" v-model="dataForm.username" placeholder="手机号码" size="default"></el-input>
                            </el-form-item>
                            <el-form-item prop="password">
                                <el-input style="width: 265px;" v-model="dataForm.password" placeholder="密码" show-password size="default"></el-input>
                            </el-form-item>
                             <el-button class="loginBtn" @click="login" type="primary" :loading="clientLoading">{{ clientLoading? '加载中' : '登录' }} </el-button>
                            <!-- <div class="loginBtn" @click="login" >登录</div> -->
                            <!-- <div class="loginBtn" @click="syncData">同步数据</div> -->
                            <div  class="formBottomContainer">
                                <el-checkbox v-model="rememberPwd">记住账号密码</el-checkbox>
                                <div class="registerText" @click="loginOrRegisterChange('register')">立即注册</div>
                            </div>
                        </el-form>
                    </div>
                    <!-- 验证码登录 -->
                    <div class="formContainer" v-if="loginType == 'code'">
                        <el-form :model="dataForm" :rules="dataRule" ref="dataForm">
                            <el-form-item prop="phone">
                                <el-input style="width: 265px;" v-model="dataForm.phone" placeholder="手机号码" size="default"></el-input>
                            </el-form-item>
                            <el-form-item prop="code">
                                <el-input style="width: 165px;" v-model="dataForm.code" placeholder="验证码"  size="default"></el-input>
                                <span class="getCodeText" v-if="getCodeCountdown <= 0" @click="getCode">获取验证码</span>
                                <span class="getCodeText" v-else style="cursor:not-allowed;">{{ this.getCodeCountdown }}s</span>
                            </el-form-item>
                            <el-button class="loginBtn" @click="login" type="primary" :loading="clientLoading">{{ clientLoading? '加载中' : '登录' }} </el-button>
                            <!-- <div class="loginBtn" @click="login" v-loading="clientLoading">
                               {{ clientLoading? '加载中' : '登录' }} 
                            </div> -->
                            <div  class="formBottomContainer">
                                <div></div>
                                <div class="registerText" @click="loginOrRegisterChange('register')">立即注册</div>
                            </div>
                        </el-form>
                    </div>
                </div>
                <!-- 注册 -->
                <div class="bodyContainer" v-if="loginOrRegister == 'register'">
                    <div style="width: 265px;">
                        <div class="registerTitle">
                            欢迎注册爱复盘
                        </div>
                        <div class="registerSubTitleContainer">
                            <span>已有账号？</span>
                            <span style="color: var(--color-main); cursor: pointer;" @click="loginOrRegisterChange('login')">登录</span>
                        </div>
                        <el-form :model="dataForm" :rules="dataRule" ref="dataForm" style="margin-top: 24px;" size="default">
                            <el-form-item prop="nickName">
                                <el-input style="width: 265px;" v-model="dataForm.nickName" placeholder="昵称"></el-input>
                            </el-form-item>
                            <el-form-item prop="phone">
                                <el-input style="width: 265px;" v-model="dataForm.phone" placeholder="手机号"></el-input>
                            </el-form-item>
                            <el-form-item prop="password">
                                <el-input style="width: 265px;" v-model="dataForm.password" placeholder="密码" show-password></el-input>
                            </el-form-item>
                            <!-- <el-form-item prop="invitationCode">
                              <el-input style="width: 265px;" v-model="dataForm.invitationCode" placeholder="邀请码（非必填）"></el-input>
                            </el-form-item> -->
                            <el-form-item prop="code">
                                <el-input style="width: 165px;" v-model="dataForm.code" placeholder="验证码"></el-input>
                                <span class="getCodeText" v-if="getCodeCountdown <= 0" @click="getCode">获取验证码</span>
                                <span class="getCodeText" v-else style="cursor:not-allowed;">{{ this.getCodeCountdown }}s</span>
                            </el-form-item>
                            <el-button class="loginBtn" @click="register" type="primary">注册</el-button>
                            <!-- <div class="loginBtn" @click="register">
                                注册
                            </div> -->
                        </el-form>
                    </div>
                </div>
            </div>
            <div v-if="!isIframe" class="version">
                <div style="margin-right: 4px;">版本号：</div>
                <div>{{ configInfo.SerialNumber }}</div>
                <div v-if="updateVersionVisible" class="mg-l6 text-colorTheme cs-p" @click="handUpdateVersion">点击更新</div>
            </div>
        </div>
        <!-- <update-version v-if="updateVersionVisible" ref="updateVersion"></update-version> -->
    </div>
</template>

<script>
import UpdateVersion from '../../commonComponent/updateVersion.vue';
import SwiperGroupLoop from './swiper.vue';
import env from '@/config/env/index';
import getPlatform from '@/utils/platformSource';
import Vue from "vue";
import {VERSION_TYPE} from "@/enum";
export default {
    components: {
        UpdateVersion,
        SwiperGroupLoop
    },
    inject:["APP"],
    data() {
        return {
            clientLoading: false,
            updateVersionVisible: false,
            loginOrRegister: "login",
            loginType: "password",
            getCodeCountdown: 0,
            rememberPwd: false,
            configInfo: {
                SerialNumber: "",
            },
            errorMsg: "",
            getCodeInterval: null,
            dataForm: {
                username: "",
                password: "",
                phone: "",
                code: "",
                invitationCode: "",
            },
            dataRule: {
                nickName: [
                    {
                        required: true,
                        message: "昵称不能为空",
                        trigger: "blur",
                    },
                    {
                        min: 2,
                        max: 20,
                        message: "长度在 2 到 20 个字符",
                        trigger: "blur",
                    }
                ],
                username: [
                    {
                        required: true,
                        message: "用户名不能为空",
                        trigger: "blur",
                    },
                    {
                        min: 4,
                        max: 20,
                        message: "长度在 4 到 20 个字符",
                        trigger: "blur",
                    }
                ],
                password: [
                    {
                        required: true,
                        message: "密码不能为空",
                        trigger: "blur",
                    },
                    {
                        min: 6,
                        message: "长度在 6 个字符以上",
                        trigger: "blur",
                    },
                    {
                        validator: (rule, value, callback) => {
                            if (this.errorMsg) {
                                callback(new Error(this.errorMsg));
                            } else {
                                callback();
                            }
                        },
                        trigger: 'manual'
                    }
                ],
                code: [
                    {
                        required: true,
                        message: "验证码不能为空",
                        trigger: "blur",
                    },
                    {
                        validator: (rule, value, callback) => {
                            if (this.errorMsg) {
                                callback(new Error(this.errorMsg));
                            } else {
                                callback();
                            }
                        },
                        trigger: 'manual'
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
            togglemaxsizeFlag: 'normal',
            versionInfo: {},
            updateVersionTimeout: null,
            isIframe: false,
            slidesData: [],
            isMobile: false,
            inviteUrlCode: '',
            slidesLoading: false
        };
    },
    computed:{
        isShowLoginBox(){
            if(env.isWeb){
                return this.isIframe;
            }else{
                return true
            }
        },
        getInviteUrlCode(){
            // 防止删除掉code，导致注册失败。
            return this.$route.query.code || this.inviteUrlCode
        }
    },
    created() {
        this.inviteUrlCode = this.getInviteUrlCode;
        localStorage.setItem("notCloseLoading", "");
        this.getBasinSetupInfo();
        this.getVersionUpdate();
        this.setClientVersion();

        // 禁止用户按F5和F12
        // window.addEventListener("keydown", (event) => {
        //   if (event.key === 'F12' || event.key === 'F5') {
        //     event.preventDefault()
        //   }
        // });
        this.getUserInfoData();
        // 用户被冻结跳到的登录页
        // if (localStorage.getItem("stopRecord")) {
        //   localStorage.setItem("stopRecord", "");
        //   this.$message.error("账号已被冻结，将停止录制和分析。");
        //   this.$httpClient.video.stopAutoAnalysis().then((res) => {
        //     this.$httpClient.compere.stopdecector({}).then((res) => {
        //       if (res.code == 0) {
        //         this.$store.commit("saveDetectionStatus", false);
        //         this.$store.commit("saveDetectionTime", null);
        //       }
        //     });
        //   });
        // }

        
        // 清空定时器
        // if (this.updateVersionTimeout) {
        //   clearInterval(this.updateVersionTimeout)
        // }
        // this.updateVersionTimeout = setInterval(() => {
        //   this.getVersionUpdate2();
        // }, 5000);
        this.$nextTick(()=>{
            setTimeout(()=>{
                this.getVersionUpdate2();
            },5000)
        })

    },
    async mounted() {
        this.getIframe();
        await this.getBannerList();
    },
    beforeDestroy() {
        // 清空定时器
        if (this.updateVersionTimeout) {
            clearInterval(this.updateVersionTimeout)
        }
        this.updateVersionTimeout = null;
    },
    methods: {

        goToOfficialWebsite(){
            this.APP?.toIfupanWebsite();
        },
        async getBannerList(){
            this.slidesLoading = true;
            await this.$httpBack.login.getBannerList().then((res)=>{
                if(res.code == 0){
                    this.slidesData = res.data;
                    this.$refs.swiperGroupLoop.initSwiper(200);
                }
            }).finally(()=>{
                this.slidesLoading = false;
            })
        },
        getIframe(){
            this.$nextTick(()=>{
                this.isIframe = !!sessionStorage.getItem('iframe') || this.$route.query?.iframe;
                this.isMobile = this.$route?.query?.isMobile
                if(this.isIframe){
                    this.loginType = 'code';
                }

            })
        },
        // 获取基本设置信息
        getBasinSetupInfo() {
            if(this.isIframe){return;}
            this.$httpClient.setup.getmodel({}).then((res) => {
                if (res.code == 0) {
                    this.configInfo = res.data;
                }
            });
        },
        async getUserInfoData(){
            let isIframeName = this.isIframe ? 'iframe_' : ''
            let username = localStorage.getItem(isIframeName + "username");
            if (username) {
                this.dataForm.username = username;
                this.dataForm.password = localStorage.getItem(isIframeName + "password");
                this.rememberPwd = true;
            }else{
                if(this.isIframe){
                    return;
                }
                let o = await this.$httpClient.setup.getUserObject().then(res=>res.data);
                if(o?.saveStatus){
                    this.rememberPwd = true;
                    this.dataForm.username = o.userName;
                    this.dataForm.password = o.password;
                }
            }
        },
        setUserInfoData(){
            let o = {}
            let isIframeName = this.isIframe ? 'iframe_' : ''
            if (this.rememberPwd) {
                localStorage.setItem(isIframeName + "username", this.dataForm.username);
                localStorage.setItem(isIframeName +"password", this.dataForm.password);
                o = {
                    userName: this.dataForm.username,
                    password: this.dataForm.password,
                    saveStatus: this.rememberPwd
                }
            } else {
                localStorage.setItem(isIframeName +"username", "");
                localStorage.setItem(isIframeName +"password", "");
                o = {userName: '', password: '', saveStatus: this.rememberPwd};
            }
            if(this.isIframe){
                return;
            }
            this.$httpClient.setup.putUserObject(o);
        },
        // 获取版本更新信息
        getVersionUpdate() {
            if(this.isIframe){return;}
            this.$httpClient.setup.getCreateTime({});
        },
        // 获取版本更新信息
        getVersionUpdate2() {
          this.$httpClient.setup.getVersionUpdate({}).then(res => {
            this.versionInfo = res.data;
            if (this.versionInfo && this.versionInfo.VersionNum) {
              if (!localStorage.getItem(this.versionInfo.VersionNum)) {
                this.updateVersionVisible = true;
                // this.$nextTick(() => {
                //   this.$refs['updateVersion'].init();
                // });
              }
            }
          });
        },
        handUpdateVersion(){
            this.$nextTick(() => {
                this.$httpClient.setup.handUpdateVersion();
            })
        },
        setClientVersion(){
            if(this.isIframe){return;}
            this.$httpClient.setup.setClientVersion({}).then(res => { })
        },
        // 禁止鼠标右键
        preventRightClick(event) {
            event.preventDefault();
        },
        // 最大化/恢复正常
        togglemaxsize() {
            this.$httpClient.form.togglemaxsize().then((res) => {
                if (res.code == 0) {
                    if (this.togglemaxsizeFlag == 'normal') {
                        this.togglemaxsizeFlag = 'max';
                    } else {
                        this.togglemaxsizeFlag = 'normal';
                    }
                }
            })
        },
        // 最小化
        minsize() {
            this.$httpClient.form.minsize().then((res) => {
            })
        },
        // 关闭
        close() {
            if(this.isIframe){return;}
            this.$httpClient.form.close().then((res) => {
            })
        },
        // 同步数据
        syncData() {
            this.$refs["dataForm"].validate((valid) => {
                if (valid) {
                    this.$httpBack.user.login(this.dataForm).then(res => {
                        if(this.isIframe){return;}
                        this.$httpClient.syncData.syncData({ token: res.data.token }).then((res) => {
                            if (res.code == 0) {
                            }
                        })
                    })
                }
            });
        },
        // 登录
        login() {
            this.errorMsg = '';
            this.$refs.dataForm.clearValidate('password');

            this.$refs["dataForm"].validate((valid) => {
                if (valid) {
                    // 嵌套登录
                    if(this.isIframe){
                        this.$httpBack.onlineUser.login(this.dataForm).then(res => {
                            if (res.code == 0) {
                                // this.$message.success(res.msg);
                                this.$store.commit("saveLoginResultData", res.data);
                                window.parent.postMessage({
                                    type: 'login',
                                    data: {
                                        status: true,
                                        msg: '登录成功'
                                    }
                                }, '*');
                            } else {
                                this.errorMsg = res.msg;
                                this.$refs.dataForm.validateField('password');
                                this.$refs.dataForm.validateField('code');
                                // this.$message.error(res.msg);
                            }
                        }).finally(()=>{
                            this.setUserInfoData();
                        });
                        return;
                    }
                    this.$httpBack.user.login(this.dataForm).then(res => {
                        if (res.code == 0) {
                            this.$message.success(res.msg);
                            this.$store.commit("saveLoginResultData", res.data);
                            // 正常登录
                            this.clientLoading = true;
                            this.$httpClient.setup.settoken({ token: res.data.token, userId: res.data.id,activeTenantId: res.data.activeTenantId }).then((res) => {
                                if (res.code == 0) {
                                    const {clientVersion,isVersionSelect} = res.data;
                                    const versionType = clientVersion === 'replay' ? VERSION_TYPE.AGENT : VERSION_TYPE.PURE;
                                    if(clientVersion) this.$store.commit("setVersionType", versionType);
                                    if (isVersionSelect===0) {
                                        this.$router.push({
                                            path: "dataAnalysis", query: {
                                                login: '1'
                                            }
                                        });
                                        window.open('https://www.douyin.com/jingxuan/');
                                    } else {
                                        this.$store.commit("setVersionType", '');
                                        this.$router.push({path: "versionSelection"});
                                    }

                                    this.clientLoading = false;
                                }
                            })
                            // 每次登录成功将该值设为ture，打开每天首次弹窗的组件
                            localStorage.setItem("currentPrompt",String(true))
                            localStorage.removeItem('clickCount')
                        } else {
                            this.errorMsg = res.msg;
                                this.$refs.dataForm.validateField('password');
                                this.$refs.dataForm.validateField('code');
                            // this.$message.error(res.msg);
                        }
                    }).finally(()=>{
                        this.setUserInfoData()
                    });
                }
            });
        },
        // 注册
        register() {
            this.errorMsg = '';
            this.$refs["dataForm"].validate((valid) => {
                if (valid) {
                    this.dataForm.inviteUrlCode = this.getInviteUrlCode || getPlatform({isCode: true}); // 如果没有邀请码，则使用当前环境邀请码
                    this.$httpBack.user.register(this.dataForm).then(res => {
                        if (res.code == 0) {
                            this.dataForm.username = this.dataForm.phone
                            this.$message.success(res.msg);
                            this.loginOrRegister = "login";
                        } else {
                            this.errorMsg = res.msg;
                            this.$refs.dataForm.validateField('password');
                            this.$refs.dataForm.validateField('code');
                            // this.$message.error(res.msg);
                        }
                    });
                }
            });
        },
        // 登录注册切换回调
        loginOrRegisterChange(loginOrRegister) {
            this.$nextTick(() => {
                this.initDataForm();
                this.loginOrRegister = loginOrRegister;
                if (this.loginOrRegister == "login") {
                    this.loginType = "password";
                    this.getUserInfoData()
                }
            });
        },
        // 登录类型切换回调
        loginTypeChange(loginType) {

            this.initDataForm();

            this.$nextTick(() => {

                this.loginType = loginType;
                if (this.loginType == "password") {
                    this.getUserInfoData()
                } else {
                    this.initDataForm();
                }

            });
        },
        initDataForm() {
            this.$refs["dataForm"].resetFields();
            this.dataForm = {
                username: "",
                password: "",
                phone: "",
                code: "",
            }
        },
        // 获取验证码
        getCode() {
            if (this.getCodeCountdown > 0) {
                return;
            }

            var reg_tel = /^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/;
            if (!this.dataForm.phone || !reg_tel.test(this.dataForm.phone)) {
                this.$message.error("请填写正确的手机号");
                return;
            }

            this.$httpBack.user.getPhoneCode({ phone: this.dataForm.phone }).then(res => {
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
    },
};
</script>

<style scoped>
.getCodeText {
    margin-left: 10px;
    width: 90px;
    font-size: 14px;
    color: #2E3742;
    cursor: pointer;
    height: var(--height-default);
    border-radius: var(--height-default);
    border: 1px solid #DCE0E7;
    text-align: center;
    line-height: var(--height-default);
    display: inline-block;
    box-sizing: border-box;
}

.registerSubTitleContainer {
    font-size: 14px;
    color: #2E3742;
}

.registerTitle {
    font-weight: 600;
    font-size: 20px;
    color: #2E3742;
}

/* .getCodeText {
  color: #555;
  cursor: pointer;
  border: 0.5px solid #ccc;
  padding: 2px 6px;
  border-radius: 4px;
  background: #FFF;
} */
.registerText {
    font-size: 14px;
    color: var(--color-main);
    cursor: pointer;
}

.formBottomContainer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 24px;
}

.loginBtn {
    width: 265px;
    height: var(--height-default);
    background: var(--color-main);
    border-radius: var(--height-default);
    font-weight: 500;
    font-size: 16px;
    color: #FFFFFF;
    text-align: center;
    line-height: var(--height-default);
    padding: 0;
    cursor: pointer;
}

.formContainer {
    margin-top: 27px;
}

.loginTypeItemSelect {
    font-weight: 500;
    font-size: 16px;
    color: rgb(0, 119, 255);
    padding: 7px 0;
    height: 36px;
    border-bottom: 3px solid rgb(0, 119, 255);
;
    box-sizing: border-box;
    cursor: pointer;
}

.loginTypeItem {
    font-weight: 500;
    font-size: 16px;
    height: 36px;
    color: #2E3742;
    padding: 7px 0;
    box-sizing: border-box;
    cursor: pointer;
}

.loginTypeContainer {
    width: 222px;
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.bodyContainer {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    margin-top: -30px;
}

.navImg {
    margin-right: 26px;
    width: 12px;
    height: 12px;
    cursor: pointer;
}

.navContainer {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    margin-top: 20px;
    z-index: 50;
    position: absolute;
    top: 0px;
    right: 0px;
}

.logoImg {
    margin-top: 40px;
    margin-left: 60px;
    width: 210px;
}

.loginBack {
    display: flex;
    justify-content: center;
    margin-top: 36px;
}

.loginRightContainer {
    background: #FFFFFF;
    width: 40%;
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
}

.loginRightBacg {
    display: flex;
    flex-direction: column;
    background-color: #FFFFFF;
    box-shadow: 0px 7px 15px 0px rgba(107, 176, 255, 0.5);
    border-radius: 8px;
    padding: 80px 70px;
}

.logintext {
    display: flex;
    flex-direction: column;
    font-size: 22px;
    color: #4D4D4D;
    margin-bottom: 30px;

    .onePlus{
        position: relative;
        &::after{
            content: '+';
            position: absolute;
            top: -10px;
        }
    }
}

.loginLeftContainer {
    /* background: #E0EFFF; */
    width: 60%;
    height: 100%;
    /* background-color: gray; */
    /* background-size: cover;
    background-repeat: no-repeat;
    background-position: center center; */
}

.loginContainer {
    display: flex;
    border: 1px solid #ccc;
    height: calc(100vh);
    -webkit-user-select: none;
    -moz-user-select: none;
    -ms-user-select: none;
    user-select: none;
    background-size: 100% 100%;
    background-image: url('~@/assets/imgs/loginBg.png');
}

.version {
    display: flex;
    position: absolute;
    bottom: 0px;
    right: 0px;
    font-size: 14px;
    color: #4D4D4D;
    margin: 0px 43px 43px 0px;
}
.formContainer{
    ::v-deep(.el-form-item__error){
        padding-top: 8px;
    }
    .loginBtn{
        margin-top: 20px;
    }
}
</style>
