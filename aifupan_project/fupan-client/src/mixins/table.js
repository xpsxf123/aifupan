import myUtils from '@/utils/utils.js';
import CoreTable from '@/components/coreTable/index.vue';
export default{
    mixins: [],
    components: {
        CoreTable,
    },
    props:{

    },
    data() {
        return {
            tableList: [],
        };
    },
    computed: {},
    watch: {},
    methods: {
        formatTableValue(val){
            return (val === -1 || val === '-1') ? '暂无': val
        },
        getList(){
            if(this.$refs?.table){
                this.$refs?.table.getList();
            }else{
                console.error('请设置table的ref属性')
            }
        },
        // 删除文件
        deletes(payload) {
            if(typeof this.deleteOpt === 'function'){
                const list = payload?.list || payload;
                const outerCallback = payload?.callback;
                const {title,http,idKey,callback} = this.deleteOpt(list);
                if(!http){return}
                this.$confirm(title || '将删除选中的数据, 是否继续?', '提示', {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning'
                }).then(() => {
                    let ids = []
                    try{
                        ids = list.map(d=>{
                            if(idKey){return d?.[idKey]}
                            return d?.videoId ?? d?.VideoId ?? d?.id
                        });
                    }catch(err){
                        console.error(err,'删除数据配置为空-----')
                    }
                    if(ids.length === 0){
                        return
                    }
                    http(ids).then(res => {
                        if (res.code === 0) {
                            this.$message.success("删除成功");
                            if(typeof outerCallback === 'function'){
                                outerCallback();
                            }
                            if(typeof callback === 'function'){
                                callback();
                            }
                            this.getList();
                        }
                    });
                })
            }
        },

        // 获取表格列表
        getTableList(param) {
            let params = {
                ...param
            };
            if(typeof this.tableHttp === 'function'){
                // 使用解构赋值从this.tableHttp获取http、param和isVideoFormat方法
                const {http,param,isVideoFormat,callback} = this.tableHttp(params);
                // 检查http是否为空，如果为空则直接返回一个已解析的Promise
                if(!http){
                    return Promise.resolve()
                }
                // 如果param存在，进一步处理http
                let httpFun = null
                if(param){
                    // 检查http是否具有then方法，如果没有则说明它不是一个Promise对象
                    if(typeof http.then === 'undefined'){
                        // 如果http不是Promise，则调用http函数并传入param作为参数
                        httpFun = http(param);
                    }
                }else{
                    httpFun = http;
                }

                return httpFun?.then(res=>{
                    if(res.code == 0){
                        if(isVideoFormat){
                            res.data.list?.forEach(item => {
                                item.durationStr = myUtils.formatSeconds(item.duration)
                                item.vedioSizie = myUtils.retainDecimals(item.vedioSizie / 1024 / 1024)
                                item.reportFileName = item.videoName.replace(/_[^_]+\.ts$/, '')
                                item.videoName = item.videoName.replaceAll('AF_', '')
                                item.videoName = item.videoName.replaceAll('.ts', '')
                                item.videoName = item.videoName.substring(item.videoName.indexOf('_') + 1);
                            })
                        }
                        this.tableList = res.data.list;
                        if(typeof callback === 'function'){
                            callback(res);
                        }
                    };
                    return res;
                })
            }else{
                console.error('未设置 tableHttp方法')
                return Promise.resolve()
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
    activated() {
    },
}
