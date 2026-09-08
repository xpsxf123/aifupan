export default {
    components: {},
    props:{
    },
    data() {
        return {
            formData: {},
            formInitData: null
        };
    },
    computed: {
        getFormVnode(){
            return this.$refs.formNode || {}
        }
    },
    watch: {},
    methods: {
        initData(data){
            if(data && !this.formInitData){
                this.formInitData = JSON.parse(JSON.stringify(data));
            };
            this.formData = data || JSON.parse(JSON.stringify(this.formInitData || {}));
        },
        setFormInitData(data){
            this.formInitData = data;
        },
        formSubmit(){
            return new Promise((res,rej)=>{
                this.getFormVnode?.validate(valid=>{
                    if(valid){
                        res()
                    }else{
                        rej()
                    }
                })
            })
        },
        validateField(...arg){
            this.getFormVnode?.validateField(...arg);
        },
        // 初始化数据
        resetFields() {
            this.getFormVnode?.resetFields();
            this.initData();
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