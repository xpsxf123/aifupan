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
            buffer="aiAssistantCompare"
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
            <template #anchorVideoOne="{ row: item }">
                <Anchor :item="{...item?.contrastInfo?.videoOneInfo,...item?.contrastInfo?.fileOneInfo,anchorInfo: item?.contrastInfo?.anchorOneInfo}"></Anchor>
            </template>
            <template #anchorVideoTwo="{ row: item }">
                <Anchor :item="{...item?.contrastInfo?.videoTwoInfo,...item?.contrastInfo?.fileTwoInfo,anchorInfo: item?.contrastInfo?.anchorTwoInfo}"></Anchor>
            </template>
            <template #updateDate="{ row: item }">
                <div class="analysisDateColContainer">
                    <div>{{ item?.contrastInfo.updateDate?.substring(0, 10) }}</div>
                    <div>{{ item?.contrastInfo.updateDate?.substring(10) }}</div>
                </div>
            </template>
            <template #empty>
                <div class="emptyTipText" style="margin-top: 10px;">
                    {{ emptyMsg }}
                </div>
            </template>
        </CoreTable>
        <drawerList ref="drawerList" :column="column" :config="formConfig" :typeConfig="typeConfig" type="2" @add="getList"></drawerList>
    </div>
</template>

<script>
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
                dataResourceType: 2,
                contrastType: 0
            },
            // 
            column: [
            {
                    label: '对比1',
                    prop: 'anchorVideoOne',
                },
                {
                    label: '对比2',
                    prop: 'anchorVideoTwo',
                    align: 'left',
                },
                {
                    label: '对比时间',
                    prop: 'updateDate',
                },
            ],
            
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
                    limit: param.pageSize,
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