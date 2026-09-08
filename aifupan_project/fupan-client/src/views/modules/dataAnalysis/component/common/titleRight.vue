<template>
    <div class="flex items-center justify-between action-header">
        <div class="flex items-center justify-start">
           <slot name="search"></slot>
           <slot name="titleLeftBottom"></slot>
        </div>
        <div class="flex items-center">
            <!-- 录制中 -->
            <template v-if="detection">
                <afp-button type="danger" size="small" style="height: 42px;" :plain="false" :disabled="isStopDisabled" @click="stopClick">
                    停止录制{{versionType === VERSION_TYPE.AGENT ? '分析' : ' '}}
                    {{ detectionBtnClickTime>0?`${detectionBtnClickTime}s`: '' }}</afp-button>
            </template>
            <!-- 未录制 -->
            <template v-else>
                <!-- <polish class="mg-l24">
                    <afp-button type="primary" id="click-cecord" :disabled="isStartDisabled"  size="small" @click="startClick">点我录制分析{{ detectionBtnClickTime>0?`${detectionBtnClickTime}s`: '' }}</afp-button>
                </polish> -->


                <button class="btn-101 mg-r10" id="click-cecord" :disabled="isStartDisabled" @click="startClick" :key="versionType">
                <span>
                    <template v-if="detectionBtnClickTime <=0">
                        <i class="animation"></i>点我录制{{versionType === VERSION_TYPE.AGENT ? '分析' : ' '}}<i class="animation"></i>
                    </template>
                    <template v-else>
                        点我录制{{versionType === VERSION_TYPE.AGENT ? '分析' : ' '}}{{ detectionBtnClickTime>0?`${detectionBtnClickTime}s`: '' }}
                    </template>
                </span>
                    <svg>
                        <defs>
                            <filter id="glow">
                                <fegaussianblur result="coloredBlur" stddeviation="5"></fegaussianblur>
                                <femerge>
                                    <femergenode in="coloredBlur"></femergenode>
                                    <femergenode in="coloredBlur"></femergenode>
                                    <femergenode in="coloredBlur"></femergenode>
                                    <femergenode in="SourceGraphic"></femergenode>
                                </femerge>
                            </filter>
                        </defs>
                        <rect />
                    </svg>
                </button>

                <!-- <afp-button type="primary" class="detection-btn" id="click-cecord" :disabled="isStartDisabled"  size="small" @click="startClick">
                    <span  class="btn-61">
                        <template v-if="detectionBtnClickTime <=0">
                            <i class="animation"></i>点我录制分析<i class="animation"></i>
                        </template>
                        <template v-else>
                            点我录制分析{{ detectionBtnClickTime>0?`${detectionBtnClickTime}s`: '' }}
                        </template>
                    </span>
                </afp-button> -->
            </template>
            <el-select v-model="recordMode" placeholder="录制格式" size="default" :disabled="isDisabled || isDetection"
                       style="width: 90px;" class="mg-l10 mg-r10" @change="recordModeChange">
                <el-option v-for="(item, index) in recordModeList" :key="item" :label="item" :value="index">
                </el-option>
            </el-select>
            <el-select v-model="definition" placeholder="清晰度" size="default" :disabled="isDisabled || isDetection"
                       style="width: 90px;" @change="definitionChange">
                <el-option v-for="(item, index) in definitionList" :key="item" :label="item" :value="index">
                </el-option>
            </el-select>
            <warm-hint ref="WarmHint" @left-click="()=>{$refs.WarmHint?.hide()}" :rightBt="topUp?'充值完成':undefined" @right-click="onRightClick">
                <div class="expired-hint text-colorMain">
                    <div>监控数量超出套餐限制,目前为<span>{{ getPackageName }}</span></div>
                    <div class="flex items-center" style="padding-block: 12px">
                        <div> 录制数量为：</div>
                        <div>
                            <template v-for="item in recordCompere">
                                <div v-if="item.used>item.all" class="flex text-center">
                                    <div class="text-right" style="width: 56px">{{ item.title }}：</div>
                                    <span>总数</span>
                                    <div class="text-colorTheme" style="width: 56px">{{ item.all }}，</div>
                                    <span>已使用</span>
                                    <div style="color: rgb(40, 189, 108);width: 32px">{{ item.used }}</div>
                                </div>
                            </template>
                        </div>
                    </div>
                    <div>请删除超出限制的账号再开启自动录制</div>
                </div>
            </warm-hint>
            <Loading ref="loading"></Loading>
        </div>
    </div>
</template>

<script>
import WarmHint from '@/components/warmHint/index.vue';
import Loading from '/src/views/modules/compere/loading.vue';
import polish from '@/components/polish/index.vue'
import { PLATFORM_ENUM ,VERSION_TYPE } from '@/enum'
export default {
    components: {
        WarmHint,
        Loading,
        polish
    },
    inject: ['parent','appVnode'],
    props: {
        detection: {
            type: Boolean,
            default: false
        },
        type:{
            type:String,
            default: 'all' // all-所有主播title, detection-录制中,
        },
        recordNum:{
            type:Number,
            default: 0
        },
        compereMapList:{
            type: Object,
            default: () => {}
        }
    },
    data() {
        return {
            VERSION_TYPE,
            definitionList: ["标清", "高清", "超清", "蓝光"],
            recordModeList: ["ts","flv"],
            detectionBtnClickTime: -1,
            setTimeOutLock: false,
            timeOut: null,
            definition: 0,
            recordMode: 0,
            topUp: false,
            recordCompere: {
                dy: {},
                ks: {},
                sph: {}
            }
        };
    },
    computed: {
        isDisabled(){
            return this.detection || this.isTimeOut();
        },
        isStopDisabled(){
            return (!this.detection || this.isTimeOut()) ;
        },
        isStartDisabled(){
            return this.isDisabled
        }, 
        isDetection(){
            return this.type === 'detection'
        },
        getTotalMonitorNum() {
            return this.$store.getters?.getUserproperty?.totalAnchorNum || 0
        },
        getCompereListLen(){
            return this.parent.compereListLen || 0
        },
        getPackageName(){
            return this.$store.state.userInfo?.packageName
        },
        versionType(){
            return this.$store.getters.getVersionType
        },
    },
    watch: {},
    methods: {
        onRightClick(){
            if(this.topUp){
                this.appVnode.refresh();
            }else{
                this.topUp = true;
                this.appVnode.showQrCode();
            }
        },

        initDatas(){
            // 获取清晰度
            this.definition = localStorage.getItem("definition") ? parseInt(localStorage.getItem("definition")) : 0;
            this.recordMode = localStorage.getItem("recordMode") ? parseInt(localStorage.getItem("recordMode")) : 0;
            this.isTimeOut();
        },
        isTimeOut(){
            if (!this.setTimeOutLock) {
                // 同步定时倒计时
                this.setTime();
            }
            return this.detectionBtnClickTime > 0
        },
        stopClick(){
            this.stopDetection()
        },
         isWarmHint(data){
             const compereAllList = Object.values(this.compereMapList).flatMap(item => item.list)

             const douYinCompereList = compereAllList.filter(item => item.platform == PLATFORM_ENUM.douyin)
             const kuaiShouCompereList = compereAllList.filter(item => item.platform == PLATFORM_ENUM.kuaishou)
             const shiPingHaoCompereList = compereAllList.filter(item => item.platform == PLATFORM_ENUM.shipinhao)

             //判断主播位数是否小于主播列表数。如果小于弹出提示
             let dy = douYinCompereList.length > data?.totalAnchorNum;
             let ks = kuaiShouCompereList.length > data?.totalKuaishouMonitorNum;
             let sph = shiPingHaoCompereList.length > data?.totalChannelMonitorNum;
             this.recordCompere = {
                 dy: {
                     title: "抖音",
                     used: douYinCompereList.length,
                     all: data?.totalAnchorNum || 0
                 },
                 ks: {
                     title: "快手",
                     used: kuaiShouCompereList.length,
                     all: data?.totalKuaishouMonitorNum || 0
                 },
                 sph: {
                     title: "视频号",
                     used: shiPingHaoCompereList.length,
                     all: data?.totalChannelMonitorNum || 0
                 }
             }
             if (dy || ks || sph) {
                 this.$nextTick(() => {
                     this.$refs.WarmHint.show();
                 })
             }
             return dy || ks || sph;
        },
        stopDetection() {
            // 限制30秒访问
            if (this.isTimeOut()) {return}
            this.$confirm('将停止所有直播间录制，是否继续？', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                this.runTimeOut(()=>{
                    this.$emit('stop',false)
                }); // 运行定时锁,并且执行绑定在关闭按钮上的事件

                // setTimeout(()=>{
                //     this.$store.commit("saveDetectionStatus", false);
                //         this.$store.commit("saveDetectionTime", null);
                //         this.$message.success("已停止录制");
                //         this.$nextTick(()=>{
                //             this.$emit('stop')
                //         })
                // },4000)
                // return
                this.$refs?.loading?.show('停止录制中...');
                this.$httpClient.compere.stopdecector({},{load: false}).then((res) => {
                    if (res.code == 0) {
                        this.$store.commit("saveDetectionStatus", false);
                        this.$store.commit("saveDetectionTime", null);
                        this.$message.success("已停止录制");
                        this.$nextTick(()=>{
                            this.$emit('stop')
                        })
                    }else{
                        this.$nextTick(()=>{
                            this.$emit('stop')
                        })
                    }
                }).finally(()=>{
                    this.$refs?.loading?.hide();
                });
            });
        },
        async startClick(){
            // 每次点击之前刷新一次用户资产数据，判断版本是否过期
            await this.appVnode?.getUserproperty((data)=>{
                // 检查录制主播数量是否超出
                if(this.isWarmHint(data)){return}
                if (this.isTimeOut()) {return}
                if (!localStorage.getItem("diskTip")) {
                    this.$httpClient.setup.getmaxdisk().then((res) => {
                    if (res.code == 0 && res.data) {
                        this.$confirm(res.data, '提示', {
                            confirmButtonText: '确定',
                            cancelButtonText: '取消',
                            type: 'warning'
                        }).then(() => {
                            this.$store.commit("saveSetupMenu", "basicSetup");
                        return;
                        }).catch(() => {
                            this.startFn()
                        });
                        localStorage.setItem("diskTip", 1);
                    } else {
                        this.startFn()
                    }
                    });
                } else {
                    this.startFn()
                }
            });
        },
        startFn(){
            this.$confirm('本软件是一款智能复盘分析软件，其作用是从目标网站上获取使用者或使用者所在公司直播间的公开视频源进行下载，包含但不限于视频、图文，文字等，这些内容为使用者或使用者所在公司的直播间信息。本软件的录制功能是全免费和公益性质的，仅提供给用户使用软件以复盘和学习为目的，切勿用于商业用途或侵害他人权利，由此产生的一切后果由使用者承担。', '免责声明', {
                confirmButtonText: '我已知晓',
                cancelButtonText: '忽略',
                type: 'warning'
            }).then(() => {
                this.runTimeOut(()=>{
                    this.$emit('start',true)
                }); // 运行定时锁,并且执行绑定在开始按钮上的事件
                let requestData = {
                    VideoType: this.recordMode,
                    Definition: this.definition
                }
                // setTimeout(()=>{
                //     this.$store.commit("saveDetectionStatus", true);
                //     this.$store.commit("saveDetectionTime", Date.now());
                //     this.$message.success("已开启检测");
                //     this.$nextTick(()=>{
                //         this.$emit('start')
                //     })
                // },1000)
                // return 
                this.$refs?.loading?.show('开启录制中...');
                this.$httpClient.compere.decector(requestData,{load: false}).then((res) => {
                    if (res.code == 0) {
                        this.$store.commit("saveDetectionStatus", true);
                        this.$store.commit("saveDetectionTime", Date.now());
                        this.$message.success("已开启检测");
                        this.$nextTick(()=>{
                            this.$emit('start')
                        })
                    }
                }).catch(err=>{
                    if(err.code === 5701){
                        this.$nextTick(()=>{
                            this.$store.commit("saveDetectionStatus", false);
                            this.$store.commit("saveDetectionTime", null);
                            this.detectionBtnClickTime = -1;
                            this.setLocalStorageTime(this.detectionBtnClickTime);
                            this.$emit('stop');
                        })
                    }
                }).finally(()=>{
                    this.$refs?.loading?.hide();
                });
            });
        },
        recordModeChange(val) {
            localStorage.setItem("recordMode", val);
            if (val == 2) {
                this.$message.info("mp4模式录制可能会导致不稳定，建议选ts模式录制，录完之后再手动转mp4");
            }
        },
        // 清晰度值改变回调
        definitionChange(val) {
            localStorage.setItem("definition", val);
        },
        runTimeOut(fn){
            // 如果大于0持续执行程序
            if(this.detectionBtnClickTime>0){
                return;
            };
            // 设置30S
            this.setLocalStorageTime(30);
            fn&&fn()
            this.setTime()
        },
        // 限制提交
        setTime(){
            let t = this.getLocalStorageTime();
            if(t<=0){
                return;
            };
            if(this.detectionBtnClickTime <=0){
                this.detectionBtnClickTime = t;
            }
            this.setTimeOutLock = true; //定时器加锁.
            // 递归调用
            clearTimeout(this.timeOut);
            this.timeOut = setTimeout(()=>{
                this.detectionBtnClickTime -=1; // 持续-1
                this.setLocalStorageTime(this.detectionBtnClickTime);
                if(this.detectionBtnClickTime > 0){
                    this.setTime();
                }else{
                    this.setTimeOutLock = false;
                    this.detectionBtnClickTime = -1;
                    this.setLocalStorageTime(this.detectionBtnClickTime);
                }
            },1000)
        },
        // 设置限制时间
        setLocalStorageTime(time){
            localStorage.setItem('detectionBtnClickTime',JSON.stringify({
                time,
                date: Date.now()
            }));
        },
        // 获取限制时间
        getLocalStorageTime(){
            if(localStorage.getItem('detectionBtnClickTime')){
                let o = JSON.parse(localStorage.getItem('detectionBtnClickTime'));
                let d = Math.floor((Date.now() - o.date )/1000);
                return o.time  - d;
            }else{
                return 0;
            }
        }
    },
    created() {
        this.initDatas();
    },
    mounted() {
    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() {
        clearTimeout(this.timeOut);
    }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() {
        this.initDatas();
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.action-header{
    margin-top: 12px;
    background: #fff;
    padding: 10px 16px;
    border-radius: 10px;
}
.detection-btn{
    padding: 0 !important;
    border: none !important;
}
.btn {
    outline: 0;
    display: inline-flex;
    align-items: center;
    justify-content: space-between;
    background: #1787f0;
    min-width: 150px;
    border: 0;
    border-radius: 4px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, .1);
    box-sizing: border-box;
    padding: 10px 20px;
    color: #fff;
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 1.2px;
    text-transform: uppercase;
    overflow: hidden;
    cursor: pointer;
  }

  .btn:hover {
    opacity: .95;
  }

  .btn .animation {
    border-radius: 100%;
    animation: ripple 0.6s linear infinite;
  }

  @keyframes ripple {
    0% {
      box-shadow: 0 0 0 0 rgba(255, 255, 255, 0.1), 0 0 0 20px rgba(255, 255, 255, 0.1), 0 0 0 40px rgba(255, 255, 255, 0.1), 0 0 0 60px rgba(255, 255, 255, 0.1);
    }

    100% {
      box-shadow: 0 0 0 20px rgba(255, 255, 255, 0.1), 0 0 0 40px rgba(255, 255, 255, 0.1), 0 0 0 60px rgba(255, 255, 255, 0.1), 0 0 0 80px rgba(255, 255, 255, 0);
    }
  }

  .btn-101,
.btn-101 *,
.btn-101 :after,
.btn-101 :before,
.btn-101:after,
.btn-101:before {
  border: 0 solid;
  box-sizing: border-box;
}
.btn-101 {
  -webkit-tap-highlight-color: transparent;
  -webkit-appearance: button;
  background-color: #000;
  background-image: none;
  color: #000;
  font-size: 100%;
  font-weight: 900;
  line-height: 1.5;
  margin: 0;
  -webkit-mask-image: -webkit-radial-gradient(#000, #fff);
  padding: 0;
  text-transform: uppercase;
}
.btn-101:disabled {
  cursor: default;
}
.btn-101:-moz-focusring {
  outline: auto;
}
.btn-101 svg {
  vertical-align: middle;
}
.btn-101 [hidden] {
  display: none;
}
.btn-101 {
  --thickness: 5px;
  --roundness: 5px;
  --color: #fff;
  --opacity: 0.6;
  -webkit-backdrop-filter: blur(100px);
  backdrop-filter: blur(100px);
  background: none;
  background: hsla(0, 0%, 100%, 0.2);
  border: none;
  border-radius: var(--roundness);
  color: var(--color);
  cursor: pointer;
  display: block;
  font-family: Poppins, "sans-serif";
  font-size: 14px;
  font-weight: 500;
  padding: 10px 20px;
  position: relative;
  background: linear-gradient(90deg, blue 50%, red 50%);
// background: #1787f0;
}
.btn-101:hover {
//   background: hsla(0, 0%, 100%, 0.3);
//   filter: brightness(1.2);
}
.btn-101:active {
//   --opacity: 0;
//   background: hsla(0, 0%, 100%, 0.1);
}
.btn-101 svg {
  border-radius: var(--roundness);
  display: block;
  filter: url(#glow);
  height: 100%;
  left: 0;
  position: absolute;
  top: 0;
  width: 100%;
}
.btn-101 rect {
  fill: none;
  stroke: #fff;
  stroke-width: var(--thickness);
  rx: 0;
  stroke-linejoin: round;
  stroke-dasharray: 185%;
  stroke-dashoffset: 0;
  -webkit-animation: snake 2s linear infinite;
  animation: snake 2s linear infinite;
  -webkit-animation-play-state: paused;
  animation-play-state: paused;
  height: 100%;
  opacity: 0;
  transition: opacity 0.2s;
  width: 100%;
}
.btn-101 rect {
  -webkit-animation-play-state: running;
  animation-play-state: running;
  opacity: var(--opacity);
}
@-webkit-keyframes snake {
  to {
    stroke-dashoffset: 370%;
  }
}
@keyframes snake {
  to {
    stroke-dashoffset: 370%;
  }
}





</style>
