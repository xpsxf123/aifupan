<template>
    <div class="lexicon-update main-bg brs-10">
        <CoreTable 
        ref="table"
        :searchData="selectData"
        :searchConfig="formConfig" 
        :menuConfig="menuConfig" 
        :getDataApi="getWordsList"
        :table-height="`calc(100vh - 208px)`"
        :apiProps="{data:'list',total: 'totalCount'}"
        class="word-table"
        notBuffer
        :column="column">
            <template #searchRight>
                <div>
                    <afp-button  type="primary" :plain="false" size="default" @click="addOrUpdate(0)">添加词语</afp-button>
                </div>
            </template>
            <!-- 空 -->
            <template #empty>
                <div v-if="(!wordList || wordList.length < 1) && getWordType == 2" class="pd-t40 pd-b40" style="text-align: center;color: #909399;font-size: 12px;line-height: 24px">
                    <span>当系统词库某个词，在您的直播间不属于敏感词的时候，您可以在本地词库中添加“敏感词白名单”，则在下一次直播分析中，该词不会再标记为敏感词。</span>
                    <br>
                    <span>敏感词白名单请谨慎标记，以免违规后，出现判断不准确的情况。</span>
                </div>
                <div v-else class="emptyContainer">
                    <img style="max-width: 267px;margin-bottom: 30px;" src="@/assets/imgs/1_9_30/ckEmpty.png" alt=""
                        srcset="">
                    <div class="emptyTipText" style="margin-top: 10px;">
                        <b>请添加词语</b>
                    </div>
                </div>
            </template>
        </CoreTable>

        <lexicon-word-add v-if="addOrUpdateVisible" ref="lexiconWordAdd" @refreshDataList="addGetList"></lexicon-word-add>
    </div>
</template>

<script>
import lexiconWordAdd from './lexicon-word-add.vue';
import CoreTable from '@/components/coreTable'
export default {
    components: {
        lexiconWordAdd,
        CoreTable,
    },
    data() {
        return {

            formConfig: {
                items: [
                    {
                        label: '', prop: 'wordType',
                        temp: 'RadioGroup',
                        config: {
                            options: [
                                {label: '全部',value: -1},
                                {label: '敏感词',value: 0},
                                {label: '关键词',value: 1},
                                {label: '敏感词白名单',value: 2},
                            ]
                        },
                        on: {
                            input:true
                        }
                    },
                    { label: '名称', prop: 'name', config:{style: {width: '150px'},}},
                    {
                        label: '添加时间', prop: 'dateRange',
                        temp: 'DatePicker',
                        config: {
                            style: {width: '200px'},
                            type:"daterange", 
                            format:"yyyy-MM-dd",
                            valueFormat:"yyyy-MM-dd",
                           
                        }
                    }
                ],
            },
            // 
            column: [
                {
                    label: '词语',
                    prop: 'name'
                },
                {
                    label: '类型',
                    prop: 'wordsType',
                    dicProp: 'wordType'
                },
                {
                    label: '备注',
                    prop: 'remarks',
                },
                {
                    label: '状态',
                    prop: 'status',
                    dicData: [
                        {label: '启用',value: 0},
                        {label: '禁用',value: 1}
                    ]
                },
                {
                    label: "添加/更新时间",
                    prop: 'updateDate'
                },
            ],
            // 
            menuConfig: {
                options: [
                    {
                        label:"编辑",
                        click:(item)=>{
                            this.addOrUpdate(item.id)
                        }
                    },
                    {
                        label:"删除",
                        type: 'danger',
                        click:(item)=>{
                            this.remove(item.id)
                        }
                    }
                ]
            },
            // 选中数据
            selectData:{
                lexiconId: this.lexiconId,
                wordType: this.wordType,
            },
            addOrUpdateVisible: false,
            selectDataForm: {},
            wordList: [],
            lexiconInfo: {}
        }
    },
    computed:{
        getWordType(){
            let t = this.selectDataForm.wordType;
            return t !=='undefined' ?t : this.wordType;
        }
    },
    props: {
        // 词库id
        lexiconId: {
            type: String,
            required: true
        },
        // 词语类型 0：敏感词 1：关键词 2：白名单
        wordType: {
            type: Number,
            required: true
        },
    },
    created() {
        this.getLexiconInfo();
        // this.getWordsList();
    },
    methods: {
        addGetList(){
            this.$refs.table.getList('add');
        },  
        // 删除词语
        remove(id) {
            this.$confirm(`确定要进行删除吗？`, "提示", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning",
            }).then(() => {
                this.$httpBack.sensitivewords
                    .delete({
                        id,
                    })
                    .then((res) => {
                        if (res && res.code === 0) {
                            this.$message({
                                message: res.msg,
                                type: "success",
                                duration: 1500,
                                onClose: () => {
                                    this.$refs.table.getList('del');
                                    // this.search();
                                },
                            });
                        } else {
                            this.$message.error(res.msg);
                        }
                    });
            });
        },
        // 添加或修改词语
        addOrUpdate(id) {
            this.addOrUpdateVisible = true;
            this.$nextTick(() => {
                this.$refs.lexiconWordAdd.init(id, this.selectDataForm.wordType, this.lexiconInfo.tradeId, this.lexiconInfo.id, this.lexiconInfo.tradeIdArr);
            });
        },
        // 获取词库信息
        getLexiconInfo() {
            this.$httpBack.lexicon.info({ id: this.lexiconId }).then(res => {
                if (res.code == 0) {
                    this.lexiconInfo = res.data;
                }
            });
        },
        // 获取词语列表
        getWordsList(param, type) {
            this.$set(this,'selectDataForm',{
                page: param.pageIndex,
                limit: param.pageSize,
                name: '',
                ...param,
                startTime: param.dateRange?.[0] || '',
                endTime: param.dateRange?.[1] || '',
            })
            let params = JSON.parse(JSON.stringify(this.selectDataForm));
            if(params.wordType === -1){
                delete params.wordType
            }
            delete params.dateRange;
            delete params.pageIndex;
            delete params.pageSize;
            return this.$httpBack.lexicon.getWordsList(params).then(res => {
                if (res && res.code === 0) {
                    this.wordList = res.data.list || [];
                } else {
                    this.wordList = [];
                }
                return res;
            });
        },
    }
}
</script>

<style lang="scss" scoped>
.word-table{
    ::v-deep(.el-table__empty-text){
        width: 100%;
    }
 }
</style>
