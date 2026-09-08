<template>
    <drawer ref="drawer" class="drawer-table-box" title="添加AI分析场次" :visible.sync="drawerVisible" width="900px">
        <CoreTable :table-height="`calc(100vh - 190px)`" notBuffer :searchConfig="getFormConfig"
                   :menuConfig="menuConfig" :getDataApi="getTableList" ref="tableDialog" :column="getColumn" :table-config="{
                'show-overflow-tooltip': false
            }">
            <template #anchor="{row:item}">
                <Anchor :item="item"></Anchor>
            </template>
            <template #fileName="{row:item}">
                <Anchor :item="item"></Anchor>
            </template>
            <template #startTime="{row:item}">
                <div class="fileSizeColContainer" style="flex-direction: column;">
                    <div>{{ item?.startTime.substring(0, 16) }}</div>
                    <div>{{ item?.endTime.substring(0, 16) }}</div>
                </div>
            </template>
            <template #anchorVideoOne="{ row: item }">
                <Anchor :item="{...item.videoOneInfo,...item?.fileOneInfo,anchorInfo: item?.anchorOneInfo}"></Anchor>
            </template>
            <template #anchorVideoTwo="{ row: item }">
                <Anchor :item="{...item.videoTwoInfo,...item?.fileTwoInfo,anchorInfo: item?.anchorTwoInfo}"></Anchor>
            </template>
            <template #updateDate="{ row: item }">
                <div class="analysisDateColContainer">
                    <div>{{ item.updateDate?.substring(0, 10) }}</div>
                    <div>{{ item.updateDate?.substring(10) }}</div>
                </div>
            </template>
        </CoreTable>
    </drawer>
</template>

<script>
import drawerMixin from '@/mixins/drawer.js';
import drawer from '@/components/drawer/index.vue';
import table from '@/mixins/table.js';
import Anchor from '@/components/anchor/index.vue';
export default {
    components: { drawer, Anchor},
    mixins: [drawerMixin, table],
    props:{
        column:{
            type:Array,
            default: ()=>{return[]}
        },
        config: {
            type: Object,
            default: () => {
            }
        },
        // 数据来源类型 0：录制视频 1：文件上传 2：对比
        type: {
            type: [Number,String],
            default: 0
        },
        typeConfig: {
            type: Object,
            default: ()=>{
                return {}
            }
        }
    },
    data() {
        return {
            // 
            menuConfig: {
                width: '110px',
                options: [
                    {
                        label: "加入分析",
                        click: (row) => {
                            this.addAiList(row)
                        }
                    },
                ]
            },
        };
    },
    computed: {
        getColumn(){
            return JSON.parse(JSON.stringify(this.column)).map(item => {
                if(item.drawerHide){
                    item.hidden = true;
                }
                return item
            });
        },
        getFormConfig(){
            return this.config || {}
        },
        getHttp(){
            let https = {
                0: this.$httpBack.video.clientVideoList,
                1: this.$httpBack.fileAnalysis.clientFileList,
                2: this.$httpBack.contrast.clientContrastList,
            }
            return https[this.type];
        }
    },
    watch: {},
    methods: {
        tableHttp(param){
            let o = {};
            if(typeof this.typeConfig?.contrastType !=='undefined'){
                o = {
                    contrastType: this.typeConfig.contrastType,
                }
            }else{
                o = {
                    analysisStatus: 2
                }
            }

            return {
                http: this.getHttp,
                param: {
                    ...param,
                    ...o,
                    page: param.pageIndex,
                    limit: param.pageSize,
                    secUid: param.secUid || '',
                }
            }
        },
        addAiList(row){
            let id = {
                0: row?.videoId,
                1: row?.fileId,
                2: row?.contrastId,
            };
            this.$httpBack.clientaifav.saveOrUpdate({
                ...this.typeConfig,
                dataResourceUuid: id[this.type]
            }).then((res)=>{
                this.$message({
                    type: 'success',
                    message:'加入分析列表成功'
                });
                this.$refs.tableDialog.getList();
                this.$emit('add')
            })
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
    activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
</style>