<!--
@description 系统基础设置页：用于维护客户端录制基础配置、全局自动删除规则与版本切换能力。
注意：
1) 全局自动删除删除内容目前优先走接口字段，接口缺失时会回退到本地缓存；
2) 该页的自动删除配置已抽为复用组件，直播间级配置需要保持相同交互体验。
-->
<template>
    <div class="">
        <el-form :model="dataForm" class="el-form-box" :rules="dataRule" size="default" ref="dataForm" label-width="100px"
            label-position="left">
            <el-row>
                <el-col :span="12">
                    <el-tooltip class="item" effect="dark" content="开播检测频率最小值为2秒，挂机建议10秒以上"  placement="top-start">
                        <el-form-item label="检测频率" prop="detectionFrequency">
<!--                            <el-input style="width: 250px;" v-model.number="dataForm.detectionFrequency/1000"-->
<!--                                placeholder="请输入"></el-input>-->
                            <el-select style="width: 250px;" v-model="dataForm.detectionFrequency" placeholder="请选择检测频率">
                                <el-option v-for="(item, index) in [0.5, 1,1.5,2,5,8,10,20,30]" :key="item" :label="item"
                                           :value="item">
                                </el-option>
                            </el-select>
                            <span style="margin-left: 15px;" @click="openDevMode">秒</span>
                        </el-form-item>
                    </el-tooltip>
                </el-col>
                <el-col :span="12">
                    <el-tooltip class="item" effect="dark" :content="dataForm.saveLocation"  placement="top-start">
                        <el-form-item label="保存位置" prop="saveLocation">
                            <el-input style="width: 250px;" v-model="dataForm.saveLocation" placeholder="请输入保存位置"
                                @focus="checkfilebox" ref="checkfilebox"></el-input>
                        </el-form-item>
                    </el-tooltip>
                </el-col>
                <el-col :span="13">
                    <el-tooltip class="item" effect="dark" content="默认使用flv即可，录不上再切换"  placement="top-start">
                        <el-form-item label="直播源" prop="streamingSource">
                            <el-select style="width: 250px;" v-model="dataForm.streamingSource" placeholder="请选择直播源">
                                <el-option v-for="(item, index) in ['m3u8源', 'flv源']" :key="item" :label="item"
                                    :value="index">
                                </el-option>
                            </el-select>
                        </el-form-item>
                    </el-tooltip>
                </el-col>
                <el-col :span="24">
                    <AutoDeleteConfig
                        v-model="globalAutoDeleteConfig"
                        class="mg-t30"
                        label="全局自动删除"
                        label-width="100px"
                        time-dict-code="global_auto_delete_time"
                        content-layout="block"
                        :content-required="false"
                        tips="智能分析或者提取文案完成后的视频才能自动删除" />
                </el-col>
<!--                <el-col :span="12">-->
<!--                    <el-tooltip class="item" effect="dark" content="录制到指定时长后，将停止录制" placement="top-start">-->
<!--                        <el-form-item label="限制时长" prop="limitTime">-->
<!--                            <div style="display: flex; margin-left: 10px;">-->
<!--                                <el-slider v-model="dataForm.limitTime" :marks="marks" :max="600" style="width: 250px;"-->
<!--                                    :show-tooltip="false" @change="val => sliderChange(val, 'limitTime')">-->
<!--                                </el-slider>-->
<!--                                <div style="margin-left: 10px;color: red;">{{ dataForm.limitTime ?-->
<!--                                    (dataForm.limitTime % 60 == 0 ? dataForm.limitTime / 60 + '小时' :-->
<!--                                        parseInt(dataForm.limitTime / 60) + '小时' + dataForm.limitTime % 60 + '分') :-->
<!--                                    '无限制' }}</div>-->
<!--                            </div>-->
<!--                        </el-form-item>-->
<!--                    </el-tooltip>-->
<!--                </el-col>-->
                <el-col :span="24">
                    <el-tooltip class="item" effect="dark" content="按照指定的时长给视频分段，达到设置值后重新录制" placement="top-start">
                        <el-form-item label="全局时长分段" prop="subsectionTime">
                            <template #label>
                                <div class="pd-t4">
                                    <span>全局时长分段</span><br>
                                    <!-- <span>(按班次排)</span> -->
                                </div>
                            </template>
                            <div class="flex items-center mg-l10">
                                <el-slider v-model="dataForm.subsectionTime" :min="30" :marks="marks" :max="600"
                                    style="width: 250px;" :show-tooltip="false"
                                    @change="val => sliderChange(val, 'subsectionTime')">
                                </el-slider>
                                <el-input-number step-strictly controls-position="right" size="default" class="subsectionTime" v-model="dataForm.subsectionTime" @change="val => sliderChange(val, 'subsectionTime')" :min="30" :max="600"
                                          placeholder="请输入分钟时长"></el-input-number>
                                <span style="padding-inline: 12px">/分钟每段（不低于30分钟）</span>
<!--                                <div style="margin-left: 10px;color: red;">{{ dataForm.subsectionTime ?-->
<!--                                    (dataForm.subsectionTime % 60 == 0 ? dataForm.subsectionTime / 60 + '小时' :-->
<!--                                        parseInt(dataForm.subsectionTime / 60) + '小时' + dataForm.subsectionTime % 60 + '分')-->
<!--                                    :-->
<!--                                    '无限制' }}</div>-->
                            </div>
                        </el-form-item>
                    </el-tooltip>
                </el-col>
                <!-- <el-col :span="13" class="mg-t40">
                    <el-tooltip class="item" effect="dark" content="11111111" placement="top-start">
                        <el-form-item id="optRestScreen" label="屏幕保护" prop="restScreen">
                            <el-radio-group v-model="dataForm.restScreen">
                                <el-radio v-for="(item,index) in restScreens" :label="item.value" :key="index" >{{ item.label }}</el-radio>
                            </el-radio-group>
                        </el-form-item>
                    </el-tooltip>
                </el-col> -->
                <el-col :span="24">
                    <el-form-item label="隐藏时长" prop="hiddenTime" style="margin-top: 6px;">
                        <el-tooltip class="item" effect="dark" content="录制时不显示已录制时长" placement="top-start">
                            <el-switch v-model="dataForm.hiddenTime" active-color="#13ce66" inactive-color="#DCE0E7">
                            </el-switch>
                        </el-tooltip>
                    </el-form-item>
                </el-col>
                <el-col :span="12">
                    <el-form-item label="隐藏大小" prop="hiddenSize" style="margin-top: 6px;">
                        <el-tooltip class="item" effect="dark" content="录制时不显示文件大小" placement="top-start">
                            <el-switch v-model="dataForm.hiddenSize" active-color="#13ce66" inactive-color="#DCE0E7">
                            </el-switch>
                        </el-tooltip>
                    </el-form-item>
                </el-col>
                <el-col :span="24">
                    <div class="flex-form">
                        <el-form-item label="版本选择" style="margin-top: 6px;">
                            <el-switch
                                class="themeAsSwitch themeAsSwitchVersion"
                                v-model="versionType"
                                inactive-text="AI全能版"
                                :active-value="VERSION_TYPE.PURE"
                                @change="versionTypeChange"
                                :inactive-value="VERSION_TYPE.AGENT"
                                active-text="纯录制版"
                                active-color="#13ce66"
                                inactive-color="#ff4949">
                            </el-switch>
                        </el-form-item>
                        <span class="tips">AI全能版，在是免费版或者激活版的时候，才能切换成纯录制版，纯录制版客户端，有激活版和纯录制版两种套餐</span>
                    </div>
                </el-col>
                <!-- <el-col :span="12">
                    <el-form-item label="在线人数" prop="hiddenSize" style="margin-top: 16px;">
                        <el-tooltip class="item" effect="dark" content="是否采集在线人数" placement="top">
                            <el-switch v-model="dataForm.zxrs" active-color="#13ce66" inactive-color="#DCE0E7">
                            </el-switch>
                        </el-tooltip>
                    </el-form-item>
                </el-col> -->
                <el-col :span="24">
                    <el-form-item>
                        <div style="text-align: right;margin-top: 100px;">
                            <afp-button style="margin-right: 50px;" size="default" v-if="mode === 0"
                                @click="closeDevMode">关闭开发模式</afp-button>
                            <afp-button style="margin-right: 50px;" type="primary" size="default" v-if="mode === 0"
                                @click="showDevTools">F12</afp-button>
                            <afp-button style="margin-right: 50px;" type="primary" size="default" @click="submit" :plain="false">点我生效</afp-button>
                        </div>
                    </el-form-item>
                </el-col>
            </el-row>

            <!-- <div class="leftContainer"> -->
                <!-- <el-form-item label="自动分段" prop="autoSubsection" style="margin-top: 36px;">
                    <el-tooltip class="item" effect="dark" content="主播进入PK模式的时候将自动分段" placement="top">
                        <el-switch v-model="dataForm.autoSubsection" active-color="#13ce66" inactive-color="#DCE0E7">
                        </el-switch>
                    </el-tooltip>
                </el-form-item> -->

                <!--                <el-form-item label="在线人数" prop="showOnlineNum">-->
                <!--                    <el-tooltip class="item" effect="dark" content="录制时不显示在线人数" placement="top">-->
                <!--                        <el-switch v-model="dataForm.showOnlineNum" active-color="#13ce66" inactive-color="#DCE0E7"-->
                <!--                            @change="onlineNumChange">-->
                <!--                        </el-switch>-->
                <!--                    </el-tooltip>-->
                <!--                </el-form-item>-->
                <!-- <el-form-item label="开播提示" prop="startTip">
                    <el-tooltip class="item" effect="dark" content="主播开播时，播放语音提示，可替换kaibo.mp3为您喜欢的语音" placement="top">
                        <el-switch v-model="dataForm.startTip" active-color="#13ce66" inactive-color="#DCE0E7">
                        </el-switch>
                    </el-tooltip>
                </el-form-item> -->
            <!-- </div>
            <div class="rightContainer"> -->
                <!-- <el-tooltip class="item" effect="dark" content="录制到指定大小后，将停止录制" placement="top">
                    <el-form-item label="限制大小" prop="limitSize" style="margin-top: 84px;">
                        <div style="display: flex; margin-left: 10px;">
                            <el-slider v-model="dataForm.limitSize" :marks="sizeMarks" :max="10240"
                                style="width: 250px;" @change="val => sliderChange(val, 'limitSize')">
                            </el-slider>
                            <div style="margin-left: 10px;color: red;">{{ dataForm.limitSize ?
                                keepTwoDecimalPlaces(dataForm.limitSize / 1024) + 'G' : '无限制' }}</div>
                        </div>
                    </el-form-item>
                </el-tooltip> -->
                <!-- <el-tooltip class="item" effect="dark" content="按照指定的大小给视频分段，达到设置值后重新录制" placement="top">
                    <el-form-item label="分段大小" prop="subsectionSize" style="margin-top: 36px;">
                        <div style="display: flex; margin-left: 10px;">
                            <el-slider v-model="dataForm.subsectionSize" :marks="sizeMarks" :max="10240"
                                style="width: 250px;" @change="val => sliderChange(val, 'subsectionSize')">
                            </el-slider>
                            <div style="margin-left: 10px;color: red;">{{ dataForm.subsectionSize ?
                                keepTwoDecimalPlaces(dataForm.subsectionSize / 1024) + 'G' : '无限制' }}</div>
                        </div>
                    </el-form-item>
                </el-tooltip> -->


            <!-- </div> -->


        </el-form>

    </div>
</template>

<script>
/**
 * @description 系统基础设置脚本：负责客户端基础配置查询、回填、提交与全局自动删除缓存兜底。
 */
import myUtils from '../../../utils/utils';
import Shepherd from 'shepherd.js';
import {VERSION_TYPE} from "@/enum";
import { shift,offset } from '@floating-ui/vue';
import AutoDeleteConfig from '@/components/autoDeleteConfig/index.vue'
export default {
    components: {
        AutoDeleteConfig
    },
    data() {
        var checkDetectionFrequency = (rule, value, callback) => {
            if (!value) {
                return callback(new Error('检测频率不能为空'));
            }
            setTimeout(() => {
                if (!Number.isFinite(value)) {
                    callback(new Error('请输入数字值'));
                } else {
                    if (value < 0) {
                        callback(new Error('必须大于0'));
                    } else {
                        callback();
                    }
                }
            }, 500);
        };
        return {
            VERSION_TYPE,
            dataForm: {
                detectionFrequency: 30, // 检测频率
                streamingSource: 0, // 直播源
                limitTime: 0, // 限制时长,0表示无限制，单位：分钟
                subsectionTime: 0, // 分段时长,0表示无限制，单位：分钟
                limitSize: 0, // 限制大小,0表示无限制，单位：G
                subsectionSize: 0, // 分段大小,0表示无限制，单位：G
                autoDeleteTime: '-1',
                deleteContent: '',
                autoSubsection: false, // 自动分段
                hiddenTime: false, // 隐藏时长
                showOnlineNum: false, // 显示在线人数
                saveLocation: "", // 保存的路径
                hiddenSize: false, // 隐藏大小
                startTip: false, // 开播提示
                id: "", // ID
                serialNumber: "", // 客户端序列号
                isRocord: 0,
                restScreen: 5
            },
            marks: {
                0: '无限制',
                120: '2h',
                240: '4h',
                360: '6h',
                480: '8h',
                600: '10h',
            },
            sizeMarks: {
                0: '无限制',
                2048: '2G',
                4096: '4G',
                6144: '6G',
                8192: '8G',
                10240: '10G',
            },
            dataRule: {
                detectionFrequency: [
                    { required: true, validator: checkDetectionFrequency, trigger: 'blur' }
                ],
                saveLocation: [
                    { required: true, message: '保存路径不能为空', trigger: 'blur' },
                ]
            },
            isEnter: false,
            timestamps: [],
            mode: 1,
            localVersionType: '',
            restScreens: [
                {label: '5min',value: 5},
                {label: '10min',value: 10},
                {label: '15min',value: 15},
                {label: '30min',value: 30},
                {label: '用不弹出',value: 99999},
            ]
        };
    },
    inject: ['appVnode'],
    computed: {
        versionType: {
            get() {
                return this.$store.getters.getVersionType;
            },
            set(val) {
                this.localVersionType = val
            }
        },
        isPackageFree() {
            return this.$store.getters.isFree || this.$store.getters.isActivated|| this.$store.getters.isPure;
        },
        globalAutoDeleteConfig: {
            get() {
                return {
                    autoDeleteTime: String(this.dataForm.autoDeleteTime ?? '-1'),
                    deleteContent: String(this.dataForm.deleteContent ?? '')
                }
            },
            set(val) {
                this.dataForm.autoDeleteTime = String(val?.autoDeleteTime ?? '-1')
                this.dataForm.deleteContent = String(val?.deleteContent ?? '')
            }
        }
    },
    mounted() {
        let _this = this
        this.getBasinSetupInfo();
        // 监听键盘按下事件
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Enter') {
                _this.isEnter = true;
            }
        });

        // 监听键盘松开事件
        document.addEventListener('keyup', function (event) {
            if (event.key === 'Enter') {
                _this.isEnter = false;
            }
        });

        this.getMode();
        this.addCheckfilebox();

        this.isRestScreen();
    },

    methods: {
        isRestScreen(){
            let type = this.$route.query.type;
            if(type === 'restScreen'){
                    // this.$tour
                    this.tour = new Shepherd.Tour({
                    useModalOverlay: true,
                    defaultStepOptions: {
                        modalOverlayOpeningPadding: 10,
                        canClickTarget:false,
                        floatingUIOptions: {
                            middleware: [shift({ padding: 32 }), offset(40)]
                        },
                    }
                });

                this.tour.addSteps([
                    {
                        text: `<div class="shepherd-step-box">
                            <p class="step-text">可以设置爱复盘录制时进入休眠模式时间！</p>
                        </div>`,
                        classes: 'example-step-extra-class',
                        attachTo: {
                            element: '#optRestScreen', // 目标元素
                            on: "auto", // 指导窗的位置，auto 会自动计算
                        },
                        buttons: [
                            {
                                text: '开始设置',
                                action: this.tour.cancel
                            }
                        ]
                    },
                ]);
                this.tour.start();
            }
        },
        onBan(){
            // douyinWarnStopRecord
            this.$httpClient.test.tesTdouyinWarnStopRecord();
        },
        getMode() {
            this.$httpClient.setup.getClientMode({}).then(res => {
                this.mode = res.data;
            })
        },
        openDevMode() {
            if (this.isEnter) {
                // 获取当前时间戳
                const currentTime = Date.now();
                // 记录当前时间戳
                this.timestamps.push(currentTime);
                // 清理掉超过1秒钟的时间戳
                this.timestamps = this.timestamps.filter(timestamp => currentTime - timestamp <= 1000);
                // 判断是否1秒内触发了6次
                if (this.timestamps.length >= 6) {
                    this.$httpClient.setup.openDevelopmentMode({}).then(res => {
                        this.mode = res.data;
                        this.$store.commit('setMode',res.data)
                        this.$message.success("进入开发模式")
                    })
                } else {
                }
            }
        },
        showDevTools() {
            if (this.mode === 0) {
                this.$httpClient.setup.openDevTools({}).then()
            }
        },
        closeDevMode() {
            this.$httpClient.setup.closeDevMode({}).then(res => {
                this.mode = res.data;
                this.$store.commit('setMode',res.data);
                this.$message.success("关闭开发模式")
            })
        },
        onlineNumChange(value) {
            if (!value) {
                this.$message.info("关闭后将不展示在线人数");
            }
        },
        checkfilebox() {
            this.$httpClient.setup.checkfilebox({}).then(res=>{
                if(res.code == 0 && res.data){
                    this.dataForm.saveLocation = res.data;
                }
            });
            this.$refs["checkfilebox"].blur();
        },
        /* 
        {
            "code": "0",
            "status": "200",
            "action": "selectedSavePath",
            "data": 'xxxxxxxxx'
        }*/
        addCheckfilebox(){
            // this.$CSharpNotify.addTask('selectedSavePath',(res,resolve)=>{
            //     this.$nextTick(()=>{
            //         this.dataForm.saveLocation = res.data;
            //     })
            // });
        },
        // 获取基本设置信息
        getBasinSetupInfo() {
            this.$nextTick(() => {
                this.$refs["dataForm"]?.resetFields();

                this.$httpClient.setup.getmodel({}).then((res) => {
                    if (res.code == 0) {
                        this.dataForm.id = res.data.Id;
                        this.dataForm.serialNumber = res.data.SerialNumber;
                        this.dataForm.detectionFrequency = res.data.DetectionFre/1000;
                        this.dataForm.streamingSource = res.data.LiveSource;
                        this.dataForm.autoSubsection = res.data.IsSubection == 0 ? false : true;
                        this.dataForm.hiddenTime = res.data.HideDuration == 0 ? false : true;
                        this.dataForm.showOnlineNum = res.data.OnlineNumber == 0 ? false : true;
                        this.dataForm.saveLocation = res.data.SavePath;
                        this.dataForm.hiddenSize = res.data.HideSize == 0 ? false : true;
                        this.dataForm.startTip = res.data.LiveNotice == 0 ? false : true;
                        this.dataForm.isRocord = res.data.IsRocord;
                        this.dataForm.autoDeleteTime = String(res.data?.autoDeleteTime ?? '-1')
                        this.dataForm.deleteContent = String(res.data?.deleteContent ?? '')

                        if (res.data.LimitType == 1) {
                            this.dataForm.limitTime = parseInt(res.data.LimitValue);
                        } else if (res.data.LimitType == 2) {
                            this.dataForm.subsectionTime = parseInt(res.data.LimitValue);
                        } else if (res.data.LimitType == 3) {
                            this.dataForm.limitSize = parseInt(res.data.LimitValue);
                        } else if (res.data.LimitType == 4) {
                            this.dataForm.subsectionSize = parseInt(res.data.LimitValue);
                        }


                    }

                });
            })
        },
        // 提交
        submit() {
            this.$refs["dataForm"].validate((valid) => {
                if (valid) {
                    let requestData = {};
                    requestData.Id = this.dataForm.id;
                    requestData.SerialNumber = this.dataForm.serialNumber;
                    requestData.DetectionFre = this.dataForm.detectionFrequency*1000;
                    requestData.LiveSource = this.dataForm.streamingSource;
                    requestData.IsRocord = this.dataForm.isRocord;
                    requestData.IsSubection = this.dataForm.autoSubsection ? 1 : 0;
                    requestData.HideDuration = this.dataForm.hiddenTime ? 1 : 0;
                    requestData.OnlineNumber = this.dataForm.showOnlineNum ? 1 : 0;
                    requestData.SavePath = encodeURI(this.dataForm.saveLocation);
                    requestData.HideSize = this.dataForm.hiddenSize ? 1 : 0;
                    requestData.LiveNotice = this.dataForm.startTip ? 1 : 0;
                    requestData.autoDeleteTime = this.dataForm.autoDeleteTime;
                    requestData.deleteContent = this.dataForm.deleteContent;
                    requestData.LimitType = 0;
                    requestData.LimitValue = 0;
                    if (this.dataForm.limitTime > 0) {
                        requestData.LimitType = 1;
                        requestData.LimitValue = this.dataForm.limitTime;
                    } else if (this.dataForm.subsectionTime > 0) {
                        requestData.LimitType = 2;
                        requestData.LimitValue = this.dataForm.subsectionTime;
                    } else if (this.dataForm.limitSize > 0) {
                        requestData.LimitType = 3;
                        requestData.LimitValue = this.dataForm.limitSize;
                    } else if (this.dataForm.subsectionSize > 0) {
                        requestData.LimitType = 4;
                        requestData.LimitValue = this.dataForm.subsectionSize;
                    }

                    this.$httpClient.setup.updatemodel(requestData).then((res) => {
                        if (res && res.code == 0) {
                            if (this.dataForm.isRocord == 1) {
                                this.$confirm('当前正在录制中，等待下次重新录制后修改的配置才会生效。', '提示', {
                                    confirmButtonText: '确定',
                                    showCancelButton: false
                                });
                            } else {
                                this.$message.success("修改成功");
                                this.appVnode?.getDisk()
                            }
                        } else {
                            this.$message.error("系统异常，请联系客服");
                        }
                    })
                }
            })
        },
        // 精确两位小数
        keepTwoDecimalPlaces(data) {
            return myUtils.retainDecimals(data);
        },
        // 滑块值改变回调
        sliderChange(value, type) {
            this.dataForm.limitTime = 0;
            this.dataForm.subsectionTime = 0;
            this.dataForm.limitSize = 0;
            this.dataForm.subsectionSize = 0;
            const val = Number(value)
            if (type == 'limitTime') {
                this.dataForm.limitTime = val;
            } else if (type == 'subsectionTime') {
                this.dataForm.subsectionTime = val;
            } else if (type == 'limitSize') {
                this.dataForm.limitSize = val;
            } else if (type == 'subsectionSize') {
                this.dataForm.subsectionSize = val;
            }
        },
        async versionChange(value) {
            try {
                const result = await this.$httpClient.setup.setClientVersionConfig({
                    clientVersion: value === VERSION_TYPE.AGENT ? 'replay' : 'record',
                    pageType: 0
                })
                if (result.code === 0) {
                    this.$store.commit("setVersionType", value);
                    setTimeout(() => {
                        this.$router.go(0);
                    }, 300)
                }
            }catch (e) {

            }
        },
        versionTypeChange(value) {
            if (this.$store.state.detectionStatus) return this.$message.info('录制中暂时无法切换版本');
            if (this.versionType === VERSION_TYPE.AGENT) {
                if (this.isPackageFree) {
                    this.versionChange(value)
                } else {
                    this.$confirm(`
                    <div style="text-align: center;height: 120px;" class="flex items-center justify-center">
                        <div>只有 <span style="color:#444DFF;">免费版</span> 或 <span style="color:#444DFF;">激活版</span> 才能切换成纯录制版客户端</div>
                    </div>`, '温馨提示', {
                        showConfirmButton: false,
                        showCancelButton: false,
                        customClass: 'edit-file-name',
                        showClose: true,
                        closeOnClickModal: false,
                        closeOnPressEscape: false,
                        dangerouslyUseHTMLString: true,
                        center: true
                    }).then(async () => {
                    }).catch(async () => {
                    });
                }
            } else {
                this.$confirm(`
                    <div style="text-align: center;height: 120px;" class="flex items-center justify-center">
                        <div>
                            <div>是否切换成<span style="color:#444DFF;">AI全能版 </span>？</div>
                            <div>切换后，可以在系统设置里修改</div>
                        </div>
                    </div>`, '温馨提示', {
                    confirmButtonText: '切换版本',
                    cancelButtonText: '不切换',
                    customClass: 'edit-file-name',
                    showClose: false,
                    closeOnClickModal: false,
                    closeOnPressEscape: false,
                    dangerouslyUseHTMLString: true,
                    center: true
                }).then(async () => {
                    await this.versionChange(value)
                }).catch(async () => {
                });
            }
        }
    },
};
</script>
<style scoped lang="scss">
.submitBtn {
    position: absolute;
    right: 60px;
}

.rightContainer {
    min-width: 450px;
}

.leftContainer {
    min-width: 420px;
}

.formContainer {
    display: flex;
}

.subsectionTime {
    width: 80px;
    margin-left: 20px;

    ::v-deep(.el-input-number__decrease) {
        display: none;
    }

    ::v-deep(.el-input-number__increase) {
        display: none;
    }

    ::v-deep(.el-input) {
        .el-input__inner {
            padding: 0;
        }
    }
}

.flex-form {
    .el-form-item {
        margin-bottom: 0;
    }
    .tips {
        font-weight: 400;
        font-size: 12px;
        color: #F4BE34;
    }
}
.el-form-box{
    ::v-deep(.el-slider__bar){
        background-color: #8ED06D;
    }
    ::v-deep(.el-slider__button){
        background-image: url('~@/assets/imgs/1_9_30/slider_bt.png');
        border: none;
        background-size: cover;
    }
}
/* el-slider__stop el-slider__marks-stop
el-slider__stop el-slider__marks-stop */
</style>
