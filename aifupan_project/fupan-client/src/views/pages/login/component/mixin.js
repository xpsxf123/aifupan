import ruleConfig from "./ruleConfig";
import GetCode from "./getCode.vue";
export default {
    components:{
        GetCode
    },
    props: {

    },
    data() {
        return {
            dataForm: {
                username: "",
                password: "",
                phone: "",
                code: "",
                invitationCode: "",
            },
            ruleConfig
        };
    },
    computed: {},
    watch: {},
    methods: {
        // 登录类型切换回调
        loginTypeChange(loginType) {
            this.initDataForm();
            this.$nextTick(() => {
                this.loginType = loginType;
                if (this.loginType == "password") {
                    let username = localStorage.getItem("username");
                    if (username) {
                        this.dataForm.username = username;
                        this.dataForm.password = localStorage.getItem("password");
                        this.rememberPwd = true;
                    }
                } else {
                    this.initDataForm();
                }
            });
        },
        // 初始化数据
        initDataForm() {
            this.$refs["dataForm"].resetFields();
            this.dataForm = {
                username: "",
                password: "",
                phone: "",
                code: "",
            }
        },
        // 获取本地储存数据
        getStorageData(){
            let username = localStorage.getItem("username");
            if (username) {
                this.dataForm.username = username;
                this.dataForm.password = localStorage.getItem("password");
                if(typeof this.rememberPwd !== 'undefined'){
                    this.rememberPwd = true;
                }
            }
        },
        codeError(){
            this.$refs.dataForm.validateField('phone');
        }
    },
    created() {

    },
    mounted() {

    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() { }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() { }, //如果页面有keep-alive缓存功能，这个函数会触发
}