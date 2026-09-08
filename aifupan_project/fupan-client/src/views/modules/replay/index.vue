<template>
    <div>
        <template v-if="replayType !== 'replayShort'">
            <!-- 全部 -->
            <record-list ref="allRecordList" :key="getTabsName" v-if="ifTabsIndex(0)" :isSelectTabs="ifTabsIndex(0)"
                         @contrast="contrast" :replayType="replayType"></record-list>
            <!-- 已完成分析 -->
            <analysis-finish-list :key="getTabsName" v-if="ifTabsIndex(1)" @contrast="contrast" :replayType="replayType"></analysis-finish-list>
            <!-- 对比分析列表 -->
            <!-- <analysis-contrast-list v-if="ifTabsIndex(2)" ref="contrastList"></analysis-contrast-list> -->
            <record-list ref="aiFinishRecordList" :key="getTabsName" v-if="ifTabsIndex(2)" :isSelectTabs="ifTabsIndex(2)" @contrast="contrast"
                         :aIFinish="true" :replayType="replayType"></record-list>
        </template>
        <template v-if="replayType === 'replayShort'">
            <record-list ref="shortRecordList" :key="getTabsName" v-if="ifTabsIndex(0)" :isSelectTabs="ifTabsIndex(2)" @contrast="contrast"
                         :aIFinish="true" :replayType="replayType"></record-list>
        </template>
    </div>
</template>

<script>
import Tabs from '@/mixins/tabs.js'
import recordList from "./record-list.vue";
import analysisFinishList from "./analysis/analysis-finish-list.vue";
import {VERSION_TYPE} from "@/enum";
// import analysisContrastList from "./contrast/analysis-contrast-list.vue";
export default {
    mixins: [Tabs],
    components: {
        recordList,
        // recordCopyList,
        analysisFinishList
        // analysisContrastList
    },
    props: {
        replayType: {
            type: String,
            default: 'replayAll'
        }
    },
    data() {
        return {
            tabs:[]
        };
    },
    computed: {
        versionType() {
            return this.$store.getters.getVersionType
        }
    },
    watch: {
        '$route.path': {
            handler() {
                this.getTabs()
            },
            immediate: true,
            deep: true
        },
        versionType: {
            handler() {
                this.getTabs()
            },
            immediate: true,
            deep: true
        }
    },
    methods: {
        isToTabs(){
            if(this.getRouteQuery('tabsName')){
                this.$nextTick(()=>{
                    this.setTabsName(this.getRouteQuery('tabsName'),'getRouteQuery');
                    this.clearTabsQuery();
                })
            }
        },
        contrast(){
            // 清楚掉表格数据
            // this.deleteBufferData('_analysisContrastList','indexOf');
            // this.replayType === 'replayAll'
            this.clearPathBufferData('/contrastReplay');
            this.clearPathBufferData('/contrastSection');
            this.$router.push({
                path: this.replayType === 'replayAll' ? '/contrastReplay' : '/contrastSection',
            })
            // this.setTabsName('compare','toContrastTabs');
        },
        getTabs() {
            if (this.replayType === 'replayAll') {
                if (this.versionType === VERSION_TYPE.PURE) {
                    this.tabs = [{label: "全部", name: 'all'}]
                } else {
                    this.tabs = [
                        {label: "全部", name: 'all'},
                        // { label: "播前分析", name: 'file' },
                        {label: "已完成分析", name: 'finish'},
                        // { label: "智能对比分析", name: 'compare' },
                        // { label: "分享复盘", name: 'online' },
                        {label: "已完成AI诊断", name: 'aIFinish'}
                    ]
                }
            } else if (this.replayType === 'replaySection') {
                this.tabs = [{label: "切片复盘", name: 'slice'}]
            } else if (this.replayType === 'replayShort') {
                this.tabs = [{label: "短视频切片", name: 'short'}]
            }
            this.initTabs('init')
            this.isToTabs()
        }
    },
    created() {
        this.isToTabs()
    },
    mounted() {
        /**
         * @description 监听客户端推送的视频分析进度通知，分析完成/失败时自动刷新列表
         */
        this.$CSharpNotify.addTask('videoAnalysisProgress', (res, resolve) => {
            const status = res?.data?.progressStatus
            if (status === 'completed' || status === 'failed') {
                this.$refs.allRecordList?.getRecordList?.()
                this.$refs.aiFinishRecordList?.getRecordList?.()
                this.$refs.shortRecordList?.getRecordList?.()
            }
            resolve?.()
        })
    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {
        this.isToTabs()
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>

</style>