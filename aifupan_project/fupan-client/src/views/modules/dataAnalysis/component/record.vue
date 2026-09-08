<template>
    <div class="data-analysis-tab-box">
        <Title
            @leftClick="rechargeTime"
            :detection="detection"
            @stop="stopHandler"
            :detectionTime="detectionTime"
            @start="startHanlder"
            type="detection"
        >
            <template #titleLeft>
<!--                <div class="common-card flex flex-col justify-around items-start">-->
<!--                    <div>录制中</div>-->
<!--                    <span class="text-2xl font-bold">{{ compereInfo.CurrentRecordNum || 0 }}</span>-->
<!--                </div>-->
<!--                <div class="common-card flex flex-col justify-around items-start">-->
<!--                    <div>还可以录制</div>-->
<!--                    <span class="text-2xl font-bold">{{ (getTotalMonitorNum || 0) - (compereInfo.CurrentRecordNum || 0) }}</span>-->
<!--                </div>-->
            </template>

            <template #search>
                <slot name="search"></slot>
            </template>
        </Title>
        <Table style="padding-top: 12px;" class="home-table" :data="compereList" :column="getColumnConfig" :menuConfig="{options: options ,width: '245px'}">
            <template #anchor="{row}">
            <Anchor :item="row"></Anchor>
            </template>
            <template #knowledgeBase="{ row }">
                <span class="cursor-pointer" style="color:var(--color-main)"
                      @click="parent.openKnowledgeBaseDrawer(row)">
                    {{ parent.getKnowledgeBaseButtonText(row) }}
                </span>
            </template>
            <template #IsAutoRecord="{row}">
                <el-tag effect="dark" :type="row.IsAutoRecord?'success': 'danger'" >
                    {{ row.IsAutoRecord ? '是' : '否' }}
                </el-tag>
                <!-- <el-switch
                    v-model="row.IsAutoRecord"
                    active-color="#0077FF"
                    inactive-color="#DCE0E7"
                    :active-value="1"
                    :inactive-value="0"
                    @change="(value) => autoRecordChange(value, row.SecUid)"
                >
                </el-switch> -->
            </template>
            <template #RecordStatus="{row,$index}">
            <div v-if="row.RecordStatus == 0">未录制</div>
            <div v-if="row.RecordStatus == 1">
                <div style="color: #FC4F52;">
                <span class="dot-span s-0"></span>录制中
                </div>
                <div style="margin-top: 4px;">{{ row.StartTime }}</div>
            </div>
            <div v-if="row.RecordStatus == 2">手动停止</div>
            <div v-if="row.RecordStatus == 3">录制完成</div>
            <div v-if="row.RecordStatus == 4">手动开启中</div>
            </template>
            <template #VedioSizie="{row}">
            <div  v-if="row.RecordStatus === 1">
                <!-- 第几段 -->
                <div class="teble-Paragraph">{{ '第' + (row.Paragraph + 1) + '段' }}</div>
                <div class="teble-VedioSizie" style="margin-top: 1px;" >
                <span v-if="configInfo.HideSize != 1">
                    {{ row.VedioSizie ? row.VedioSizie + "M" : "" }}
                </span>
                <span v-if="configInfo.HideDuration != 1 && configInfo.HideSize != 1" style="font-size: 14px;color: #909499;">|</span>
                <span  v-if="configInfo.HideDuration != 1">
                    {{ row.Duration }}
                </span>
                </div>
            </div>
            <div v-else>-</div>
            </template>
            <template #OnlineNumber="{row}">
                <template v-if="versionType===VERSION_TYPE.PURE">
                    <span v-if="detection && row?.LiveStatus == 2 && configInfo?.OnlineNumber == 1&&row.pureRecordOnlineNum==1">
                        {{ row.OnlineNumber ? row.OnlineNumber : 0 }}</span>
                    <span v-else>-</span>
                </template>
                <template v-else>
                    <span v-if="detection && row?.LiveStatus == 2 && configInfo?.OnlineNumber == 1">
                        {{ row.OnlineNumber ? row.OnlineNumber : 0 }}</span>
                    <span v-else>-</span>
                </template>
            </template>
            <template #empty>
            <div class="emptyContainer">
                <img style="max-width: 350px;margin-bottom: 40px;" src="@/assets/imgs/1_9_30/hlz.png" alt="" srcset="">
                <div class="emptyTipText">暂无录制</div>
                <el-button type="text" @click="$emit('tabsName','first')">返回录制</el-button>
                </div>
            </template>
        </Table>
    </div>
</template>

<script>
import Mixin from './mixin'
import { VERSION_TYPE } from '@/enum'
export default {
    components: {},
    mixins: [Mixin],
    props:{

    },
    data() {
        return {
            VERSION_TYPE
        };
    },
    computed: {
        getColumnConfig() {
            return this.columnConfig.filter(item => item.prop !== 'buyIn')
        }
    },
    watch: {},
    methods: {
        // 运行单个主播录制定时锁
        runClickTimerLock() {
            let timeKeys = Object.keys(this.itemBtnClickData);
            if (timeKeys.length <= 0) { return }
            timeKeys.forEach(key => {
                this.itemBtnClickData[key] -= 1; //减少1s时间锁
                if (this.itemBtnClickData[key] <= 0) {
                    // 小于等于0删除时间锁
                    delete this.itemBtnClickData[key];
                }
            })
        },

        // 停止主播录制
        stopRecord(secUid) {
            if (this.getClickTimerLock(secUid)) {
                this.$message.warning("请等待倒计时结束后再操作");
                return;
            };
            sessionStorage.setItem("notLoading", "");
            this.clickTimerLock(secUid)
            this.$httpClient.compere.stoprecord({ secUid }).then((res) => {
                if (res.code == 0) {
                    this.parent.getDataList();
                    this.$message.success("停止录制成功，请等待程序执行停止！");
                    sessionStorage.setItem("notLoading", "1");
                }
            });
        },
        getCompereList(param){
            return this.getDataList(1,param);
        }
    },
    created() {

    },
    mounted() {
    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.common-card{
    height: 99px;
    width: 176px;
    border-radius: 10px;
    background: radial-gradient(318.59% 138.89% at 78.4% 108.5%, rgba(159, 196, 255, 0.50) 0%, rgba(179, 232, 255, 0.50) 100%);
    box-shadow: 0 2px 16px 0 rgba(143, 161, 226, 0.15);
}
</style>
