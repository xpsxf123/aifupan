export default {
    components: {},
    props:{
        title: {
            type:String,
            default: ''
        }
    },
    data() {
        return {
            drawerVisible: false,
            drawerTitle: '',
            drawerData: {}
        };
    },
    computed: {
        getTitle(){
            return this.drawerTitle || this.title
        }
    },
    watch: {},
    methods: {
        show(option = {}){
            this.drawerVisible = true
            const {data,title} = option || {}
            if(data){
                this.drawerData = data;
            }
            if(title){
                this.drawerTitle = title;
            }
            if(typeof this.showCallback === 'function'){
                this.showCallback(option)
            }
        },
        hide(option){
            this.drawerVisible = false
            if(typeof this.hideCallback === 'function'){
                this.hideCallback(option)
            }
            this.$nextTick(()=>{
                this.initDialog();
            })
        },
        initDialog(){
            this.drawerData = {}
            this.drawerTitle = '';
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