<template>
    <div>
        <el-dialog :visible.sync="visible" width="440px" :close-on-click-modal="false" >
            <div style="padding:16px; padding-top: 0;">
                <div style="display: flex;align-items: center;justify-content: space-between;">
                    <span style="color: #151917;font-size: 16px;font-weight: 500;">视频申诉</span>
                    <!-- <img src="../../assets/imgs/close.png" style="width: 24px;height: 24px;cursor: pointer;" @click="visible = false"> -->
                </div>
                <div style="color: #909499;font-size: 12px;">如此视频有异议，请留下您的联系方式以便我们核实相关信息</div>
            </div>
            <div style="width: 100%;height: 1px;background-color: #E3E3E5;"></div>

            <div style="display: flex;justify-content: center;margin: 24px 0px 20px 0px;">
                <el-form :model="dataForm" :rules="dataRule" ref="dataForm" label-width="107px">
                    <el-form-item label="直播账号昵称" prop="liveUserName">
                        <el-input v-model="dataForm.liveUserName" style="width: 230px;" maxlength="100" show-word-limit size="small"></el-input>
                    </el-form-item>
                    <el-form-item label="联系人名称" prop="contacts" >
                        <el-input v-model="dataForm.contacts" style="width: 230px;" maxlength="50" show-word-limit size="small"></el-input>
                    </el-form-item>
                    <el-form-item label="手机号码" prop="phone" >
                        <el-input v-model="dataForm.phone" style="width: 230px;" maxlength="20" show-word-limit size="small"></el-input>
                    </el-form-item>
                    <el-form-item label="公司名称" prop="companyName" >
                        <el-input v-model="dataForm.companyName" style="width: 230px;" maxlength="100" show-word-limit size="small"></el-input>
                    </el-form-item>
                    <el-form-item label="申诉原因" prop="appealReason">
                        <el-input v-model="dataForm.appealReason" type="textarea" maxlength="500" placeholder="申诉原因最多输入500字"></el-input>
                        <div style="margin-top: -10px;font-size: 12px;">当前字数：<span style="color: lightseagreen;">{{ currentLength }}</span> / 500</div>
                    </el-form-item> 
                </el-form>
            </div>
            
            <div style="display: flex;justify-content: center;padding-bottom: 60px;">
                <afp-button type="primary" style="border-radius: 2px;" @click="submit">确认提交</afp-button>
            </div>
        </el-dialog>

        <el-dialog :visible.sync="successVisible" width="384px">
            <div style="display: flex;justify-content: end;padding: 16px;">
                <img src="../../assets/imgs/close.png" style="width: 24px;height: 24px;cursor: pointer;" @click="successVisible = false">
            </div>
            <div style="display: flex;justify-content: center;align-items: center;flex-direction: column;">
                <img src="../../assets/imgs/complainSuccess.png">
                <div style="margin-top: 24px;padding-bottom: 71px;color: #151917;font-size: 16px;font-weight: bold;">已提交申诉，请等待运营联系</div>
            </div>
        </el-dialog>
    </div>
</template>

<script>
    export default{
        data(){
            return{
                visible:false,
                successVisible:false,

                dataForm:{
                    appealReason:'',
                },
                dataRule:{
                    liveUserName:[{required:true,message:'请输入直播账号昵称',trigger:'blur'}],
                    contacts:[{required:true,message:'请输入联系人名称',trigger:'blur'}],
                    phone:[
                        {required:true,message:'请输入手机号码',trigger:'blur'},
                        { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码', trigger: 'blur' }
                    ],
                    companyName:[{required:true,message:'请输入公司名称',trigger:'blur'}],
                }
            }
        },
        computed:{
            currentLength(){
                return this.dataForm.appealReason.length;
            }
        },
        methods:{

            init(secUid,videoId){
                this.visible = true;
                this.dataForm = {
                    appealReason:'',
                };
                this.dataForm.anchorUrlId = secUid;
                this.dataForm.anchorVideoId = videoId;
            },

            submit(){
                this.$refs["dataForm"].validate((valid) =>{
                    if(valid){
                        let requestData = JSON.parse(JSON.stringify(this.dataForm));
                        requestData.id = "";
                        this.$httpBack.videoAppeal.uploadVideoAppeal(requestData).then((res) =>{
                            if(res && res.code === 0){
                                    this.visible = false
                                    this.successVisible = true
                                    this.$emit("refreshDataList");
                                    this.$refs['dataForm'].resetFields();
                            }else{
                                this.$message.error(res.msg);
                            }
                        })
                    }
                })
            }
        }
    }
</script>

<style scoped>
 ::v-deep .el-form-item{
    margin-bottom: 10px;
 }
 ::v-deep .el-dialog{
    border-radius: 4px;
 }
</style>