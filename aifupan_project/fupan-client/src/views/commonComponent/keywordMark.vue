<template>
    <div>
        <el-dialog :visible.sync="visible" width="546px" :close-on-click-modal="false">
            <div class="keywordMark-dialog-close"><img src="../../assets/imgs/close.png" @click="dialogClose()" style="margin:14px 13px 0px 0px;width: 16px; height: 16px;cursor: pointer;"></div>
            <div class="keywordMark-dialog-title">标记关键词</div>
            <el-form 
                :model="keywordMarkForm"
                label-width="100px"
                :rules="dataRule"
                ref="keywordMarkForm">
                <el-form-item label="关键词" prop="keywordText">
                    <el-input v-model="keywordMarkForm.keywordText" size="small" style="width: 358px;"></el-input>
                </el-form-item>
                
                <el-form-item label="关键词类型" prop="keywordType">
                    <el-select v-model="keywordMarkForm.keywordType" size="small" style="width: 100px;">
                        <el-option
                            v-for="item in keywordTypeList"
                            :label="item.label"
                            :value="item.value"
                            :key="item.value">
                        </el-option>
                    </el-select>
                </el-form-item>
            </el-form>

            <div style="display: flex;justify-content: center;padding-bottom: 32px;">
                <afp-button class="keywordMark-dialog-button" size="small" style="margin-right: 10px;" @click="dialogClose()">取消</afp-button>
                <afp-button class="keywordMark-dialog-button" size="small" type="primary">确认标记</afp-button>
            </div>

        </el-dialog>
    </div>
</template>

<script>
export default {
    data() {
        return {
            visible: false,
            keywordMarkForm:{
                keywordText:'',
                keywordType:'',
            },
            keywordTypeList:[
                {value:'value1',label:'敏感词'},
                {value:'value2',label:'关键词'},
            ],
            dataRule:{
                keywordText:[
                    {required:true,message:'请输入关键词',trigger:'blur'}
                ],
                keywordType:[
                    {required:true,message:'请选择关键词类型',trigger:'blur'}
                ]
            },
        }
    },
    methods: {
        init(){
            this.visible = true;
        },
        // 关闭表单并清空表单数据
        dialogClose(){
            this.visible = false;
            this.keywordMarkForm = {};
            this.$refs["keywordMarkForm"].resetFields();
        }
    }
}
</script>

<style scoped>
.keywordMark-dialog-close{
    display: flex;
    justify-content: end;
}
.keywordMark-dialog-title{
    display: flex;
    justify-content: center;
    color: #2E3742;
    font-size: 16px;
    margin-bottom: 19px;
}

.keywordMark-dialog-button{
    border-radius: 4px;
    font-size: 14px;
    width: 110px;
    height: 40px;
}
</style>