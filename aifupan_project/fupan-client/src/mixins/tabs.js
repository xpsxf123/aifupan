import myUtils from "../utils/utils";
export default {
    data: ()=>{
        return {
            tabs: null,
            tabsName: '',
            tabsMixinDebounce:myUtils.debounce(100, true),
            notInitTabs: false
        }
    },
    computed: {
        getTabsName:{
            get(){
                return this.$store.getters?.getTabsName || this.tabsName ||'';
            },
            set(val){
                this.setTabsName(val,'set');
            }
        },
        // 获取tabsList
        getTabsList(){
            return this.$store.getters?.getTabsList|| this.tabs || [];
        },
    },
    methods: {
        // 设置tabs列表
        setTabs(list,type){
            this.tabs = list;
            this.$store.commit("setTabsList", list || this.tabs);
            if(type === 'init'){
                this.setTabsName(list[0]?.name,type)
            }else{
                this.setTabsName(this.getBufferData('tabs') || list[0]?.name,type)
            }
        },
        setTabsName(tabsName,type){
             // 判断点击事件。
             if(typeof this.$store.state?.tabsClickCallback === 'function' && type === 'click'){
                this.$store.state?.tabsClickCallback(tabsName);
                // 判断路径是否携带tabs参数。
                this.clearTabsQuery();
                return;
            }
            // undefined 则不进行设置
            if(typeof tabsName === 'undefined'){return};
            // 重复则不进行数据设置
            if(this.getTabsName === tabsName){
                return;
            }
            this.tabsName = tabsName;
            this.$store.commit('setTabsName',tabsName || this.tabsName);
            // 执行缓存数据储存
            this.$nextTick(()=>{
                this.setBufferData('tabs', tabsName);
            })
        },
        clearTabsQuery(){
            // 清空所有缓存数据
            this.clearPathBufferData();
            if(this.$route.query?.tabsName){
                this.clearQueryKey('tabsName')
            }
        },
        setTabsClickCallback(callback){
            this.$store.commit('setTabsClickCallback',callback);
        },
        ifTabsIndex(index){
            return this.tabs[index]?.name === this.getTabsName;
        },
        ifTabsName(name){
            return name === this.getTabsName;
        },
        setTabsNameCallback(callback){
            if(callback === 'init'){
                this.$store.commit('setTabsCallback',null);
                return;
            }
            if(typeof callback === 'function'){
                this.$store.commit('setTabsCallback',callback);
            }
        },
        initTabs(type){
            if(this.notInitTabs){return}
            this.$nextTick(()=>{
               // 默认设置tabs
                if(this.tabs && Array.isArray(this.tabs) && this.tabs.every(item=>item.name && item.label)){
                    this.tabsMixinDebounce(()=>{
                        this.setTabs(this.tabs, type);
                    })
                };
            })
        }
    },
    created() {
        
    },
    mounted() {
        this.initTabs('mounted')
    },
    activated(){
        this.initTabs('activated');
    },
    beforeDestroy(){
        
    },
    destroyed() {
        this.setTabs([]);
        this.setTabsName('','clear');
        this.setTabsNameCallback('init');
    }
}