<template>
    <div style="display: flex;align-items: center;">
        <el-select v-model="recordMode" placeholder="录制格式"  :disabled="isDisabled || isDetection"
            style="width: 90px;margin-right: 10px;" @change="recordModeChange">
            <el-option v-for="(item, index) in recordModeList" :key="item" :label="item" :value="index">
            </el-option>
        </el-select>
        <el-select v-model="definition" placeholder="清晰度"  :disabled="isDisabled || isDetection"
            style="width: 90px;margin-right: 10px;" @change="definitionChange">
            <el-option v-for="(item, index) in definitionList" :key="item" :label="item" :value="index">
            </el-option>
        </el-select>
        <!-- 录制中 -->
        <template v-if="detection">
            <afp-button type="danger" size="small" :disabled="isStopDisabled" @click="stopClick">停止录制分析{{
                detectionBtnClickTime > 0 ? `${detectionBtnClickTime}s`: '' }}</afp-button>
        </template>
        <!-- 未录制 -->
        <template v-else>
            <afp-button type="primary" :disabled="isStartDisabled" size="small" @click="startClick">点我录制分析{{
                detectionBtnClickTime > 0 ? `${detectionBtnClickTime}s`: '' }}</afp-button>
        </template>
    </div>
</template>

<script>

export default {
    components: {},
    props: {
        detection: {
            type: Boolean,
            default: false
        },
        type: {
            type: String,
            default: 'all' // all-所有主播title, detection-录制中,
        },
        recordNum: {
            type: Number,
            default: 0
        }
    },
    data() {
        return {
            definitionList: ["标清", "高清", "超清", "蓝光"],
            recordModeList: ["ts", "flv"],
            // recordModeList: ["ts"],
            detectionBtnClickTime: -1,
            setTimeOutLock: false,
            timeOut: null,
            definition: 0,
            recordMode: 0
        };
    },
    computed: {
        isDisabled() {
            return this.detection || this.isTimeOut();
        },
        isStopDisabled() {
            return (!this.detection || this.isTimeOut());
        },
        isStartDisabled() {
            return this.isDisabled
        },
        isDetection() {
            return this.type === 'detection'
        }
    },
    watch: {},
    methods: {
        initDatas() {
            // 获取清晰度
            this.definition = localStorage.getItem("definition") ? parseInt(localStorage.getItem("definition")) : 0;
            this.recordMode = localStorage.getItem("recordMode") ? parseInt(localStorage.getItem("recordMode")) : 0;
            this.isTimeOut();
        },
        isTimeOut() {
            if (!this.setTimeOutLock) {
                // 同步定时倒计时
                this.setTime();
            }
            return this.detectionBtnClickTime > 0
        },
        stopClick() {
            this.stopDetection()
        },
        stopDetection() {
            // 限制30秒访问
            if (this.isTimeOut()) { return }
            this.$confirm('将停止所有直播间录制，是否继续？', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                this.runTimeOut(() => {
                    this.$emit('stop', false)
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
                this.$httpClient.compere.stopdecector({}, { load: false }).then((res) => {
                    if (res.code == 0) {
                        this.$store.commit("saveDetectionStatus", false);
                        this.$store.commit("saveDetectionTime", null);
                        this.$message.success("已停止录制");
                        this.$nextTick(() => {
                            this.$emit('stop')
                        })
                    } else {
                        this.$nextTick(() => {
                            this.$emit('stop', true)
                        })
                    }
                });
            });
        },
        startClick() {
            if (this.isTimeOut()) { return }
            if (!localStorage.getItem("diskTip")) {
                this.$httpClient.setup.getmaxdisk().then((res) => {
                    if (res.code == 0 && res.data) {
                        this.$confirm(res.data, '提示', {
                            confirmButtonText: '确定',
                            cancelButtonText: '取消',
                            type: 'warning'
                        }).then(() => {
                            this.$store.commit("saveSetupMenu", "basicSetup");
                            this.$emit('updateMenuIndex', 4);
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
        },
        startFn() {
            this.$confirm('本软件是一款智能复盘分析软件，其作用是从目标网站上获取使用者或使用者所在公司直播间的公开视频源进行下载，包含但不限于视频、图文，文字等，这些内容为使用者或使用者所在公司的直播间信息。本软件的录制功能是全免费和公益性质的，仅提供给用户使用软件以复盘和学习为目的，切勿用于商业用途或侵害他人权利，由此产生的一切后果由使用者承担。', '免责声明', {
                confirmButtonText: '我已知晓',
                cancelButtonText: '忽略',
                type: 'warning'
            }).then(() => {
                this.runTimeOut(() => {
                    this.$emit('start', true)
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
                this.$httpClient.compere.decector(requestData).then((res) => {
                    if (res.code == 0) {
                        this.$store.commit("saveDetectionStatus", true);
                        this.$store.commit("saveDetectionTime", Date.now());
                        this.$message.success("已开启检测");
                        this.$nextTick(() => {
                            this.$emit('start')
                        })
                    } else {
                        this.$nextTick(() => {
                            this.$emit('start', false)
                        })
                    }
                }).catch(err => {
                    this.$nextTick(() => {
                        this.$emit('start', false)
                    })
                })
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
            localStorage.setItem("detection", val);
        },
        runTimeOut(fn) {
            // 如果大于0持续执行程序
            if (this.detectionBtnClickTime > 0) {
                return;
            };
            // 设置30S
            this.setLocalStorageTime(30);
            fn && fn()
            this.setTime()
        },
        // 限制提交
        setTime() {
            let t = this.getLocalStorageTime();
            if (t <= 0) {
                return;
            };
            if (this.detectionBtnClickTime <= 0) {
                this.detectionBtnClickTime = t;
            }
            this.setTimeOutLock = true; //定时器加锁.
            // 递归调用
            clearTimeout(this.timeOut);
            this.timeOut = setTimeout(() => {
                this.detectionBtnClickTime -= 1; // 持续-1
                this.setLocalStorageTime(this.detectionBtnClickTime);
                if (this.detectionBtnClickTime > 0) {
                    this.setTime();
                } else {
                    this.setTimeOutLock = false;
                    this.detectionBtnClickTime = -1;
                    this.setLocalStorageTime(this.detectionBtnClickTime);
                }
            }, 1000)
        },
        // 设置限制时间
        setLocalStorageTime(time) {
            localStorage.setItem('detectionBtnClickTime', JSON.stringify({
                time,
                date: Date.now()
            }));
        },
        // 获取限制时间
        getLocalStorageTime() {
            if (localStorage.getItem('detectionBtnClickTime')) {
                let o = JSON.parse(localStorage.getItem('detectionBtnClickTime'));
                let d = Math.floor((Date.now() - o.date) / 1000);
                let t = o.time - d;
                return t > 30 ? 30 : t;
            } else {
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
<style lang='scss' scoped></style>
