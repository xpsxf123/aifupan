export default {
    components: {},
    props:{
    },
    data() {
        return {
            
        };
    },
    computed: {
    },
    watch: {},
    methods: {
        // 设置表单配置字典
        setFormConfigDic(KeysOrIndexs,formConfig,option={}) {
            const {callback} = option;
            Object.keys(KeysOrIndexs).map(k=>{
                let item = formConfig?.items[k] || formConfig?.items?.find(d=>d.prop === k);
                let opts = KeysOrIndexs[k];
                if(item && item?.config?.options){
                    let o = opts;
                    let prop = item?.prop;
                    if(typeof option[prop] === 'function'){
                        o = option[prop](opts,item) || o;
                    }else if(typeof callback === 'function'){
                        o = callback(prop,opts,item) || o;
                    }
                    this.$set(item?.config,'options', o);
                }
            })
        },
        // 获取主播列表
        getCompereList(callback) {
            let requestData = {
                pageIndex: 1,
                pageSize: 9999,
                anchorName: "",
                recordStatus: null,
                isRemoveRecord: null,
                tradeId: ''
            }
            return this.$httpClient.compere.getpageanchor(requestData).then((res) => {
                if (res.code == 0) {
                    let compereList = res.data.DataList;
                    if(typeof callback === 'function'){
                        callback(compereList);
                    }
                    return compereList;
                }
                return res.data;
            })
        },
        getCompereBackList(callback){
            let requestData = {
                page: 1,
                limit: 999999,
                anchorName: "",
                tradeId: ''
            }

           return this.$httpBack.compere.clientAnchorRecordList(requestData).then((res) => {
                if (res.code == 0) {
                    let compereList = res.data.list;
                    if(typeof callback === 'function'){
                        callback(compereList);
                    }
                    return compereList;
                }
                return res.data;
            })
        },
        // 根据用户添加的主播返回行业列表
        getListByAnchor(callback){
            return this.$httpBack.trade.listByAnchor({}).then((res) => {
                if (res.code == 0) {
                    if(typeof callback === 'function'){
                        callback(res.data);
                    }
                    return res.data
                }
                return res.data;
            })
        },
        // 云空间行业列表
        listByTenantAnchor(callback){
            return this.$httpBack.trade.listByTenantAnchor({}).then((res) => {
                if (res.code == 0) {
                    if(typeof callback === 'function'){
                        callback(res.data);
                    }
                    return res.data
                }
                return res.data;
            })
        },
        // 云空间主播列表
        clientTenantAnchorList(callback){
            return this.$httpBack.compere.clientTenantAnchorList({}).then((res) => {
                if (res.code == 0) {
                    let compereList = res.data;
                    if(typeof callback === 'function'){
                        callback(compereList);
                    }
                    return compereList;
                }
                return res.data;
            })
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