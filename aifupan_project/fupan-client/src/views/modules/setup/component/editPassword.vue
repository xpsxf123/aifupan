<template>
    <dialog-box title="更改密码" :visible.sync="dialogVisible" width="373px">
        <div>
            <el-form ref="formNode" :model="formData" label-width="80px" :rules="rules" size="default" >
                <el-form-item label="更改密码" prop="newPassword">
                    <el-input v-model="formData.newPassword" placeholder="更改密码" show-password></el-input>
                </el-form-item>
                <el-form-item label="确认密码" prop="checkPassword">
                    <el-input v-model="formData.checkPassword" placeholder="确认密码" show-password></el-input>
                </el-form-item>
                <el-form-item label="验证码" prop="code">
                    <div class="flex-jc-sb">
                        <el-input style="width: 165px;" v-model="formData.code" placeholder="验证码"></el-input>
                        <getCode :phone="formData.phone" @error="isGetCode" :isGetCode="isGetCode"></getCode>
                    </div>
                </el-form-item>
                <div class="text-right">
                    <afp-button @click="onCancel" >取消</afp-button>
                    <afp-button type="primary" :plain="false" @click="submit">确定</afp-button>
                </div>
            </el-form>
        </div>
    </dialog-box> 
</template>

<script>
import DialogBox from '@/components/dialog/index.vue'
import dialogMixin from '@/mixins/dialog.js'
import formMixin from '@/mixins/form.js'
import ruleConfig from '@/views/pages/login/component/ruleConfig.js';
import getCode from '@/views/pages/login/component/getCode.vue'
export default {
    components: {
        DialogBox,
        getCode
    },
    mixins: [dialogMixin, formMixin],
    props:{
        
    },
    data() {
        return {
            rules: {
                newPassword: ruleConfig.password,
                checkPassword: [
                    ...ruleConfig.password,
                    {
                        trigger: "blur",
                        validator:(rule, value, callback) => {
                            if(!value){
                                callback(new Error("请再次输入密码"))
                            }
                            if (this.formData.newPassword !== this.formData.checkPassword) {
                                callback(new Error('请确认修改密码一致'));
                            } else {
                                callback();
                            }
                        }
                    }
                    
                ],
                code: ruleConfig.code
            },
        };
    },
    computed: {},
    watch: {},
    methods: {
        async isGetCode(){
            let o = true;
            await this.validateField(['newPassword','checkPassword'],async (msg)=>{
                o = !msg;
            });
            return o;
        },
        showCallback(){
            this.formData.phone = this.dialogData.normalPhone
        },
        onCancel(){
            this.resetFields();
            this.hide();
        },
        submit(){
            this.formSubmit().then(()=>{
                this.$httpBack.user.updatePassword(this.formData).then(()=>{
                    this.onCancel();
                    this.delUserInfo();
                    this.$message.success("密码修改成功");
                })
            })
        },
        async delUserInfo(){
            let o = await this.$httpClient.setup.getUserObject().then(res=>res.data);
            // 如果有数据储存则清空密码
            if(o.saveStatus){
                localStorage.setItem("password", "");
                o = {userName: o.userName, password: '', saveStatus: o.saveStatus};
                this.$httpClient.setup.putUserObject(o);
            }
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