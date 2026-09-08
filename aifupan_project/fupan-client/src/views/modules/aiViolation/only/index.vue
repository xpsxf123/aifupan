<template>
    <div>
        <CoreTable 
            :searchConfig="formConfig" 
            :menuConfig="menuConfig"
            :getDataApi="getTableList"
            ref="table"
            :column="column"
            :table-select="true" 
            @deletes="deletes"
            buffer="aiAssistantOnly"
            notBuffer
            :table-height="`calc(100vh - 210px)`"
            :table-config="{
                'show-overflow-tooltip': false
            }"
        >
            <template #btns>
                <afp-button type="primary" size="default" @click="addAI">添加AI分析场次</afp-button>
            </template>
            <template #searchRight>
                <div class="flex-jc-e">
                    {{AiThokenNum}}
                </div>
            </template>
            <template #anchor="{row:item}">
                <Anchor :item="item?.videoInfo"></Anchor>
            </template>
            <template #startTime="{row:item}">
                <div class="fileSizeColContainer" style="flex-direction: column;">
                    <div>{{ item?.videoInfo?.startTime.substring(0, 16) }}</div>
                    <div>{{ item?.videoInfo?.endTime.substring(0, 16) }}</div>
                </div> 
            </template>
            <template #empty>
                <div class="emptyTipText" style="margin-top: 10px;">
                    {{ emptyMsg }}
                </div>
            </template>
        </CoreTable>
        <drawerList ref="drawerList" :column="column" :config="formConfig" :typeConfig="typeConfig"  type="0" @add="getList"></drawerList>
    </div>
</template>

<script>
import myUtils from '@/utils/utils.js';
import common from '@/views/modules/aiMixin/common.js'
export default {
    components: {
    },
    mixins: [common()],
    props:{
        
    },
    data() {
        return {
            typeConfig: {
                favType: 1,
                dataResourceType: 0
            },
            // 
            column: [
                {
                    label: '主播',
                    prop: 'anchor',
                },
                {
                    label: '录制时间',
                    prop: 'startTime'
                },
                {
                    label: '智能敏感词',
                    prop: 'sensitiveWordNum',
                    drawerHide: true,
                    formatter:(row)=>{
                        return this.formatTableValue(row?.videoInfo?.recordInfo?.sensitiveWordNum)
                    }
                },
                {
                    label: '运营关键词',
                    prop: 'cruxWordNum',
                    drawerHide: true,
                    formatter:(row)=>{
                        return this.formatTableValue(row?.videoInfo?.recordInfo?.cruxWordNum)
                    }
                },
                {
                    label: '全文字数',
                    prop: 'contentNum',
                    drawerHide: true,
                    formatter: (row)=>{
                        return this.formatTableValue(myUtils.toLocale(row?.videoInfo?.recordInfo?.contentNum));
                    }
                }
            ],
            // 
            
        };
    },
    computed: {},
    watch: {},
    methods: {
        tableHttp(param){
            return {
                http: this.httpList,
                param: {
                    ...param,
                    limit:param.pageSize,
                    page: param.pageIndex,
                    ...this.typeConfig
                }
            }
        },
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