
import myUtils from "../utils/utils";
const clearQueryDebounce = myUtils.debounce(100);
export default {
    methods: {
        // 设置路由参数
        setRouteQuery(key,data){
            this.setBufferData(key, data)
        },
        // 设置路由数据
        setRouteQuerys(data){
            this.setBufferData('RouteQuery', data || '{}');
        },
        // 获取路由数据
        getRouteQuery(keyOrOBjcet,defaultValue){
            if(typeof keyOrOBjcet === 'string'){
                return this.$route.query?.[keyOrOBjcet] || this.getBufferData(keyOrOBjcet) || defaultValue;
            }else{
                return {
                    ...this.getBufferData('RouteQuery'),
                    ...this.$route.query || {},
                } || keyOrOBjcet || {};
            }
        },
        // 清理路由参数
        clearQueryKey(keyOrKeys){
            clearQueryDebounce(()=>{
                // 储存当前页面所有需要删除的query的key
                let query = this.$route.query;
                let keys = Array.isArray(keyOrKeys)?keyOrKeys:[keyOrKeys];
                keys?.forEach((k)=>{
                    delete query[k];
                });
                // 判断是否有参数
                if(Object.keys(query).length === 0){return};
                // 清空参数
                this.$router.push({
                    path: this.$route.path,
                    query
                });
            })
        },
        // 清楚当前路径下所有缓存数据
        clearPathBufferData(path){
            Object.keys(sessionStorage)?.forEach(key=>{
                if(key.indexOf(path || this.$route.path) !== -1){
                    sessionStorage.removeItem(key);
                }
            })
        },
        deleteBufferData(name,type){
            // 执行模糊删除
            if(type === 'indexOf'){
                Object.keys(sessionStorage).filter(k=>{
                    return k.indexOf(this.$route.path+ '_' + name) !== -1
                }).forEach(k=>{
                    sessionStorage.removeItem(k);
                });
                return
            }
            // 执行删除
            sessionStorage.removeItem(this.$route.path+ '_' + name);
        },
        // 设置缓存
        setBufferData(name,data){
            if(!name){return}
            sessionStorage.setItem(this.$route.path+'_'+name, JSON.stringify(data));
        },
        // 获取缓存
        getBufferData(name){
            if(!name){return null;};
            try{
                return JSON.parse(sessionStorage.getItem(this.$route.path+'_'+name))
            }catch(err){
                return  sessionStorage.getItem(this.$route.path+'_' + name)
            }
        }
    }
}