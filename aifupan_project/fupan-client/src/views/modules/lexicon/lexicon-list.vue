<template>

    <!-- 本地词库 -->
    <div class="lexicon">
        <!-- 本地词库顶部 -->

        <CoreTable 
            :menuConfig="menuConfig"
            ref="table"
            :column="column" 
            :notSearch="true" 
            :apiProps="{data:'list',total: 'totalCount'}" 
            :getDataApi="getDataList"
            :table-height="`calc(100vh - 220px)`"
        >
            <template #tableTop>
                 <!-- 本地词库添加按钮 -->
                <div class="main-bg pd-16 mg-b10">
                    <afp-button size="medium" @click="showAddLexiconDialog('')" >+ 添加词库</afp-button>
                </div>
            </template>
            <template #name="{row}">
                <div style="display: flex; align-items: center;">
                    <img src="@/assets/imgs/lexicon.png" class="videoImg">
                    <div style="margin-left: 10px;">
                        <div class="lexicon-type-name">{{ row.name }}</div>
                        <div class="lexicon-type-info">{{ row.tradeName }}</div>
                    </div>
                </div>
            </template>
            <template #lexicons="{row}">
                <div class="lexicon-btns" style="flex: 2; text-align: center;">
                    <el-button type='text' @click="toWordList(row.id,0)">
                        敏感词{{`(${row.sensitiveNum})`}}
                    </el-button>
                    <el-button type='text' @click="toWordList(row.id,1)">
                        关键词{{`(${row.cruxNum})`}}
                    </el-button>
                    <el-button type='text' @click="toWordList(row.id,2)">
                        敏感词白名单{{`(${row.whiteNum})`}}
                    </el-button>
                </div>
            </template>
            <template #empty>
                <div class="emptyContainer">
                    <img style="max-width: 267px;margin-bottom: 30px;" src="@/assets/imgs/1_9_30/ckEmpty.png" alt=""
                        srcset="">
                    <div class="emptyTipText" style="margin-top: 10px;">
                        <b>请建立您的本地词库</b>
                    </div>
                </div>
            </template>
        </CoreTable>
       

        <!-- 添加/修改词库表单 -->
        <div>
            <el-dialog :title="!addForm.id ? '添加词库' : '编辑词库'" :visible.sync="dialogFormVisible" width="400px"
                :close-on-click-modal="false">
                <div class="dialogClass">
                    <el-form :model="addForm" :rules="dataRule" ref="addForm" label-width="100px" size="default">
                        <div style="margin-top: 24px;">
                            <el-form-item class="add-form-item" label="名称" prop="name">
                                <el-input v-model="addForm.name" placeholder="请输入词库名称" style="width: 220px;"></el-input>
                            </el-form-item>
                            <el-form-item label="行业" prop="tradeIdArr">
                                <el-cascader v-model="addForm.tradeIdArr" :options="tradeTreeList"
                                    :props="{ checkStrictly: true, expandTrigger: 'click', value: 'id', label: 'name' }"
                                    filterable placeholder="请选择行业" @change="changeTradeHandle" ref="tradeCascader"
                                    style="width: 220px;">
                                </el-cascader>
                            </el-form-item>
                            <el-form-item class="add-form-item" label="备注" prop="remarks">
                                <el-input v-model="addForm.remarks" placeholder="请输入备注" style="width: 220px;"></el-input>
                            </el-form-item>
                        </div>

                        <div class="add-form-button">
                            <afp-button @click="dialogFormVisible = false" size="default" style="width: 110px;">取消</afp-button>
                            <afp-button @click="submit" size="default" type="primary" :plain="false"
                                style="margin-left: 16px;width: 110px;">确认</afp-button>
                        </div>
                    </el-form>
                </div>

            </el-dialog>
        </div>
    </div>
</template>

<script>
import CoreTable from '@/components/coreTable'
export default {
    components:{
        CoreTable
    },
    data() {
        return {

            column: [
                {
                    label: '名称',
                    prop: 'name',
                    option: {
                        'min-width': 200,
                    }
                },
                {
                    label: '备注',
                    prop: 'remarks',
                    option: {
                        'min-width': 300,
                    }
                },
                {
                    label: '词库',
                    prop: 'lexicons',
                    option: {
                        'min-width': 200,
                    }
                }
            ],
            menuConfig: {
                options: [
                    {
                        label:"编辑",
                        // show(row){
                        // },
                        click:(item)=>{
                            this.showAddLexiconDialog(item.id);
                            // this.createAnalysis(item.fileId)
                        }
                    }
                ]
            },

            dataForm: {
                keyword: ''
            },
            tradeTreeList: [],
            dataList: [],
            pageIndex: 1,
            pageSize: 10,
            totalCount: 0,
            dialogFormVisible: false,
            addForm: {
                id: "",
                tradeIdArr: [],
                name: "",
                tradeId: "",
                remarks: "",
                oldTradeId: "",
            },
            dataRule: {
                name: [
                    { required: true, message: "词库名称不能为空", trigger: "blur" },
                ],
                tradeIdArr: [
                    { required: true, message: "行业不能为空", trigger: "blur" },
                ]
            },
        }
    },
    created() {
        this.getTradeTreeList();
        // this.getDataList();
    },
    methods: {
        toWordList(lexiconId, wordType) {
            this.$emit("child-event", lexiconId, wordType);
        },
        // 显示
        showAddLexiconDialog(id) {
            this.dialogFormVisible = true;
            this.addForm.tradeIdArr = [];
            this.$nextTick(() => {
                this.$refs["addForm"].resetFields();
                this.addForm.id = id;
                if (this.addForm.id) {
                    this.$httpBack.lexicon.info({ id }).then(res => {
                        if (res.code == 0) {
                            this.addForm = res.data;
                            this.addForm.tradeIdArr = JSON.parse(this.addForm.tradeIdArr);
                            this.addForm.oldTradeId = this.addForm.tradeId;
                        }
                    })
                }
            });
        },
        // 选择行业回调
        changeTradeHandle() {
            // 关闭级联列表下拉
            this.$refs.tradeCascader.dropDownVisible = false;
        },
        // 获取行业列表树形
        getTradeTreeList() {
            this.tradeTreeList = [];
            this.$httpBack.trade.listTree({}).then((res) => {
                if (res && res.code === 0) {
                    this.tradeTreeList = res.data;
                }
            });
        },
        // 添加/编辑词库
        submit() {
            this.$refs["addForm"].validate((valid) => {
                if (valid) {
                    let requestData = JSON.parse(JSON.stringify(this.addForm));
                    if (requestData.tradeIdArr instanceof Array) {
                        requestData.tradeId = requestData.tradeIdArr[requestData.tradeIdArr.length - 1];

                    } else {
                        requestData.tradeId = requestData.tradeIdArr;

                    }

                    requestData.tradeIdArr = JSON.stringify(requestData.tradeIdArr);

                    if (requestData.id) {
                        // 修改
                        this.$httpBack.lexicon.update(requestData).then(res => {
                            if (res.code == 0) {
                                this.$message.success(res.msg);
                                this.dialogFormVisible = false;
                                this.$refs.table.getList();
                            }
                        });
                    } else {
                        // 新增
                        this.$httpBack.lexicon.save(requestData).then(res => {
                            if (res.code == 0) {
                                this.$message.success(res.msg);
                                this.dialogFormVisible = false;
                                this.$refs.table.getList();
                            }
                        });
                    }
                }
            })
        },

        // 获取数据列表
        getDataList(params) {
            this.dataListLoading = true;
            // this.dataForm.page = this.pageIndex;
            // this.dataForm.limit = this.pageSize;
            let param = {
                page:params?.pageIndex,
                limit:params?.pageSize,
                ...this.dataForm
            }
            return this.$httpBack.lexicon.list(param).then((res) => {
                if (res && res.code === 0) {
                    this.dataList = res.data.list;
                    this.totalCount = res.data.totalCount;
                } else {
                    this.dataList = [];
                    this.totalCount = 0;
                }
                this.dataListLoading = false;
                return res;
            });
        },
        // 删除
        deleteHandle(id) {
            this.$confirm(`确定要进行删除吗？`, "提示", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning",
            }).then(() => {
                this.$http.lexicon.delete({
                    id,
                }).then((res) => {
                    if (res && res.code === 0) {
                        this.$message({
                            message: res.msg,
                            type: "success",
                            duration: 1500,
                            onClose: () => {
                                this.getDataList();
                            },
                        });
                    } else {
                        this.$message.error(res.msg);
                    }
                });
            });
        },

    }
}
</script>

<style lang="scss" scoped>
.lexicon-btns{
   .el-button{
        margin: 0 5px !important;
   }
}


.lexiconWordTypeBtn {
    cursor: pointer;
    color: var(--color-main);
}

.dialogClass {
    padding-bottom: 27px;
}

/* 顶部内容 */
.lexicon-top {
    width: calc(100% - 28px);
    border-bottom: 1px solid #DCE0E7;
    margin-left: 14px;
}

.lexicon-top-text {
    color: #2E3742;
    font-size: 16px;
    margin-bottom: 13px;
}

/* 本地词库添加按钮 */
.lexicon-add-button {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 94px;
    height: 32px;
    background-color: var(--color-main);
    margin: 15px 0px 0px 14px;
}

.lexicon-add-button:hover {
    cursor: pointer;
}

.lexicon-add-logo {
    font-size: 18px;
    color: #FFFFFF;
    margin-right: 7px;
}

.lexicon-add-text {
    color: #FFFFFF;
    font-size: 13px;
}

/* #region 本地词库内容列表 */
.lexicon-content {
    margin: 16px 14px 0px 14px;
}

/* 条目 */
.lexicon-entry {
    display: flex;
    align-items: center;
    height: 28px;
    background-color: #F5F7F9;
    color: #2E3742;
    font-size: 12px;
    border-radius: 4px;
}


/* 列表 */
.lexicon-list {
    display: flex;
    height: 63px;
    align-items: center;
    border-bottom: 1px solid #DCE0E7;
}

.videoImg {
    width: 40px;
    height: 40px;
}

.lexicon-item-img {
    width: 48px;
    height: 38px;
    background-color: skyblue;
}

.lexicon-type {
    display: flex;
    flex-direction: column;
    margin-left: 16px;
}

.lexicon-type-name {
    color: #2E3742;
    font-size: 14px;
}

.lexicon-type-info {
    color: #677583;
    font-size: 13px;
}

.lexicon-ciku {
    display: flex;
    justify-content: space-between;
    margin-top: 12px;
    color: #2E3742;
    font-size: 14px;
}

.lexicon-update-button {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 37px;
    height: 24px;
    font-size: 13px;
    color: #2E3742;
    border-radius: 4px;
    border: 1px solid #B4BCCA;
}

.lexicon-update-button:hover {
    cursor: pointer;
}

/* #region 添加词库表单 */
/* .add-form-item {
    display: flex;
    align-items: center;
} */

.add-form-button {
    display: flex;
    justify-content: center;
    margin-top: 30px;

}

.cancel {
    width: 110px;
    height: 40px;
    color: #2E3742;
    font-size: 14px;
    border-radius: 4px;
    border: 1px solid #B4BCCA;
    margin-right: 20px;
}

.addYes {
    width: 110px;
    height: 40px;
    color: #FFFFFF;
    background-color: var(--color-main);
    font-size: 14px;
    border-radius: 4px;
    border: 1px solid #B4BCCA;
}

/* #endregion */

/* 修改element-ui默认样式 */
::v-deep .el-dialog__body {
    padding: 0;
}

::v-deep .el-dialog__header {
    padding: 0;
    display: flex;
    justify-content: center;
    align-items: center;
}

::v-deep .el-dialog__headerbtn {
    top: 14px;
    right: 13px;
}

::v-deep .el-dialog__title {
    font-size: 16px;
    color: #2E3742;
    margin-top: 25px;
}

::v-deep .el-button+.el-button {
    margin: 0;
}

/* ::v-deep .el-form-item {
    margin: 0px;
}

::v-deep .el-form-item__label {
    color: #2E3742;
    font-size: 14px;
    margin: 0px 13px 0px 42px;
    padding: 0;
}

::v-deep .el-input__inner {
    width: 240px;
    height: 32px;
    border-radius: 4px;
    border: 1px solid #CFD4DB;
} */
</style>