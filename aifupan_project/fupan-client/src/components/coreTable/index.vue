<template>
    <div>
        <!-- 搜索 -->
        <div v-if="!notSearch" class="core-search brs-10" :style="{zoom:getZoom>1?1:getZoom}" :class="getClass('search')">
            <Search ref="search"  :searchData="formData" @updatedSearch="updatedSearch" :size="size" :searchConfig="getSearchConfig"
                @search="search">
                <template v-if="$slots.form">
                    <slot name="form"></slot>
                </template>
                <template #btns>
<!--                    <afp-button type="primary" plain @click="refresh" v-if="!getTableConfig.notRefresh"  size="default">刷新</afp-button>-->
                    <slot v-if="isBtnsCenter" name="btns"></slot>
                    <afp-button :type="searchDelType" plain @click="deletes" class="core-search-del" v-if="getTableConfig.select" size="default" >批量删除</afp-button>
                    <slot v-if="!isBtnsCenter" name="btns"></slot>
                </template>
                <template v-if="$slots.searchRight" #searchRight>
                    <slot name="searchRight" v-bind="{...config}"></slot>
                </template>
                <template #ref-btn>
                    <slot name="ref-btn">
                        <el-button type="text" @click="refresh" style="padding: 0;height: 22px" v-if="!getTableConfig.notRefresh">刷新</el-button>
                    </slot>
                </template>
            </Search>
        </div>
        <div v-if="$slots.tableTop">
            <slot name="tableTop"></slot>
        </div>
        <!-- 表格 -->
        <div class="core-table" :class="getClass('table')">
            <slot name="table" :data="getTableData" :menuConfig="menuConfig">
                <Table ref="tableDom" v-bind="{ ...getTableConfig }" :optionsMap="getSearchConfigOptionsMap"
                    :menuConfig="menuConfig" :data="getTableData" :rowKey="rowKey" :column="getTableColumn"
                    @select-list="selectList" :is-flex="isFlex">
                    <template v-for="slot in getSlotList()" slot-scope="scope" :slot="slot">
                        <slot v-bind="scope" :name="slot"></slot>
                    </template>
                </Table>
            </slot>
        </div>
        <div v-if="$slots.tableBottom">
            <slot name="tableBottom"></slot>
        </div>
        <!-- 分页 -->
        <div class="core-page" :class="[getClass('page')]">
            <div class="mg-t10" :class="[($slots['page-before'] || $slots['page-after'])?'flex-jc-sb flex-ai-c':'']">
                <slot name="page-before"></slot>
                <page v-if="!notPage" v-bind="getPageConfig" :size="size" @current-change="currentChange" @size-change="sizeChange" @size-input="sizeInput" @current-input="currentInput"></page>
                <slot name="page-after"></slot>
            </div>
        </div>
    </div>
</template>

<script>
import slot from './../../mixins/slot'
import Table from './../Table/index.vue';
import Page from './../page/index.vue'
// src\utils\utils.js
import myUtils from '/src/utils/utils.js';
import Search from './../search/index.vue'
export default {
    components: { Table, Page, Search },
    mixins: [slot],
    props: {
        mainBg: {
            type: Array,
            default: () => {
                return ['search', 'page', 'table']
            }
        },
        page: {
            type: Object,
            default: null
        },
        searchConfig: {
            type: Object,
            default: () => { return {} }
        },
        searchData: {
            type: Object,
            default: () => { return {} }
        },
        getDataApi: {
            type: [Function, null],
            default: null
        },
        dataCallback: { 
            type: [Function, null],
            default: null
        },
        apiParams: {
            type: [Function, null, Object],
            default: null
        },
        resData: {
            type: [Function, null],
            default: null
        },
        tableList: {
            type: Array,
            default: () => { return [] }
        },
        codes: {
            type: Array,
            default: () => { return [] }
        },
        column: {
            type: Array,
            default: () => { return [] }
        },
        notPage: {
            type: Boolean,
            default: false
        },
        apiProps: {
            type: Object,
            default: () => {
                return {}
            }
        },
        // 是否自动发起请求，用于分页数据改变搜索等操作发起请求
        isAutoApi: {
            type: Boolean,
            default: true
        },
        notFirstGet: {
            type: Boolean,
            default: false
        },
        rowKey: {
            type: String,
            default: ""
        },
        menuConfig: {
            type: Object,
            default: () => {
                return {}
            }
        },
        notSearch: {
            type: Boolean,
            default: false
        },
        notActivated: {
            type: Boolean,
            default: false
        },
        size: {
            type:String,
            default: 'mini'
        },
        isFlex: {
            type:Boolean,
            default: false
        },
        buffer: {
            type:String,
            default: ''
        },
        notBuffer: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            pageData: {
                pageSize: 10,
                pageIndex: 1,
                total: 0
            },
            formData: {

            },
            tableData: [],
            selectListData: {},
            // d:myUtils.d
            debounce: myUtils.debounce(500),
            initDebounce: myUtils.debounce(100,true),
            errNum: 0
        };
    },
    computed: {
        config(){
            return {
                size: this.size
            }
        },
        // 搜索按钮插槽是否剧中.(在删除按钮之前插入内容)
        isBtnsCenter(){
            return this.$CONFIG?.coreTable?.isBtnsCenter
        },
        searchDelType(){
            return this.$CONFIG?.coreTable?.searchDelType || 'danger'
        },
        getZoom () {
            return this.$store.getters.getDomZoom || 1
        },
        isSelectLen() {
            return this.selectListData?.list?.length
        },
        // 获取表格配置Config，注入v-bind;
        getTableConfig() {
            let o = {
                size: this.size,
            };
            // 提取table配置
            Object.keys({ ...this.$attrs, ...this.$props }).forEach(key => {
                if (key.indexOf('table') >= 0) {
                    if (key.split('-')?.[1]) {
                        o[key.split('-')?.[1]] = this.$attrs[key]
                    }
                }
            })
            if (!o.config) {
                o.config = {
                    ...o,
                    ...o.config,
                }
            }
            return o
        },
        // 获取表格数据
        getTableData() {
            let list = [];
            if (typeof this.getDataApi === 'function') {
                list = this.tableData;
            } else {
                list = this.tableList || this.getTableConfig.data;0
            }
            return this.dataCallback?.(list) || list
        },
        getSearchConfig(){
            return {
                ...this.searchConfig,
                items: this.searchConfig?.items?.filter(d=>{
                    if(d.hide){
                        return typeof d.hide === 'function'? !d.hide(d) : !d.hide;
                    }
                    if(d.hidden){
                        return typeof d.hidden === 'function'? d.hidden(d) : d.hidden;
                    }else{
                        return true;
                    }
                })
            };
        },
        // 获取配置
        getTableColumn() {
            return (this.column || this.getTableConfig.column || [])
        },
        // 搜索数据的options
        getSearchConfigOptionsMap() {
            let o = {}
            this.getSearchConfig?.items?.forEach(d => {
                if (d?.config?.options) {
                    o[d.prop] = d?.config?.options || [];
                }
            })
            return o
        },
        getSearchData(){
            return this.formData;
        },
        // 获取分页
        getPageConfig() {
            if(this.page){
                return {
                    ...this.pageData,
                    ...this.page
                }
            }else{
                return this.pageData
            }
        },
        getPageBufferName(){
            if(!this.buffer){return ''};
            return `${this.buffer ? this.buffer + '-' : ''}table-page`
        },
        getFormBufferName(){
            if(!this.buffer){return ''};
            return `${this.buffer ? this.buffer + '-': ''}table-form`
        }
    },
    watch: {
        searchData: {
            handler(data){
                if(Object.keys(data)?.length){
                    this.$set(this,'formData',{
                        ...this.formData,
                        ...data
                    });
                }
            },
            deep: true,
            immediate: true
        }
    },
    methods: {
        refresh(){
            this.getTableList({}, 'refresh');
        },
        // 清空缓存
        clearBufferData(){
            this.deleteBufferData(this.getPageBufferName);
            this.deleteBufferData(this.getFormBufferName);
        },
        setPageData(pageDataAndPageKey,value){
            if(typeof pageDataAndPageKey === 'object'){
                this.pageData = pageDataAndPageKey;
            }else if(typeof pageDataAndPageKey === 'string'){
                this.$set(this.pageData,pageDataAndPageKey,value);
            }
            if(this.notBuffer){return}
            this.setBufferData(this.getPageBufferName, this.pageData);
        },
        getPageData(){
            if(this.notBuffer){return}
            let o = this.getBufferData(this.getPageBufferName);
            if(o){
                this.pageData = o;
            }
        },
        setFormData(formData){
            if(formData){
                this.formData = formData;
            }
            if(this.notBuffer){return}
            this.setBufferData(this.getFormBufferName, this.formData);
        },
        getFormData(){
            if(this.notBuffer){return}
            let o = this.getBufferData(this.getFormBufferName);
            if(o){
                this.$set(this,'formData',{
                    ...o,
                    ...this.searchData
                })
                this.$refs.search?.updatedData(this.formData || {})
            }
        },
        // 跟新数据接口，用于更新内部数据
        updateTtablList(fn) {
            if (typeof fn === 'function') {
                if (typeof this.getDataApi == 'function') {
                    fn(this.tableData)
                } else {
                    fn(this.tableList || this.getTableConfig.data);
                }
            }
        },
        getClass(type) {
            return this.mainBg.includes(type) ? 'main-bg' : ''
        },
        search(formData) {
            // 节流锁定
            this.setFormData(formData);
            this.pageData.pageIndex = 1;
            this.debounce(() => {
                this.getTableList({}, 'search');
            })
            this.$emit('search');
        },
        updatedSearch(key, data) {
            if (typeof key === 'object') {
                this.$set(this, 'formData', key)
            } else {
                this.$set(this.formData, key, data)
            }
        },
        deletes() {
            if(this.isSelectLen){
                this.$emit('deletes', {
                    ...this.selectListData,
                    callback: () => {
                        this.clearSelection();
                        this.$nextTick(() => {
                            this.getTableList({}, 'deletes');
                        })
                    }
                })
            }
            
        },
        clearSelection() {
            this.selectListData = {}
            this.$refs.tableDom?.clearSelection();
        },
        updateTable(){
            this.$refs.tableDom?.updateTable?.();
        },
        // 获取表格数据
        async getTableList(param = {}, type) {
            if (!this.getDataApi) { return }
            // 如果type不等于notClearSelection 并且开启选择，每次请求都会刷新选中状态
            if(type !== 'notClearSelection'){
                console.log('刷新选中状态');
                this.clearSelection();
            }
            // this.clearSelection();
            let params = {
                pageIndex: this.getPageConfig.pageIndex,
                pageSize: this.getPageConfig.pageSize,
                ...this.formData,
                ...param,
            }
            // 获取请求数据
            let requestParams = typeof this.apiParams === 'function' ? this.apiParams(params) || params : typeof apiParams === 'object' ? this.apiParams : params;
            // 发起请求
            await this.getDataApi(requestParams || {}, type).then(res => {
                if (res.code === 0 || this.codes.includes(res.code)) {
                    let data = typeof this.resData === 'function' ? this.resData(res.data) || res.data : res.data;
                    // 设置翻页数据
                    let total = this.$fetchValue(data,this.apiProps.total,'Total','totalCount');
                    if (!this.notPage) {
                        this.setPageData('total',total);
                    }
                    // 如果总页数大于当前页，则将当前页退回第一页
                    if(total && this.getPageConfig.pageIndex > Math.ceil(total/this.getPageConfig.pageSize)){
                        return Promise.reject('pageIndex');
                    }
                    // 设置表格数据
                    let list = this.$fetchValue(data, this.apiProps.data,'DataList','list');
                    this.tableData = list || [];
                    this.$emit('api-succees', res);
                    // 重置错误收集器
                    this.errNum = 0;
                }
                this.$emit('api-finally', res);
                return res;
            }).catch(rej => {
                this.$emit('api-error', rej);
                this.$emit('api-finally', rej);
                // 当前页错误回溯。
                if(rej === 'pageIndex' && this.errNum<5){
                    this.errNum++;
                    this.setPageData('pageIndex',1);
                    this.getTableList({},'err');
                }
                return rej
            })
        },
        selectList(listData) {
            this.selectListData = listData;
            this.$emit('table-select', listData);
        },
        // 选中
        toggleRowSelection(tableList) {
            this.$nextTick(() => {
                this.$refs.tableDom.toggleRowSelection(tableList)
            })
        },
        // 是否自定获取表格数据
        autoGetList(param, type) {
            if (this.isAutoApi) {
                this.getTableList(param || {}, type);
            }
        },
        // 设置页码
        setCurrent(pageIndex) {
            if (typeof this.page?.pageIndex !== 'undefined') {
                this.page.pageIndex = pageIndex
            }
            this.setPageData('pageIndex',pageIndex)
        },
        // 页码改变
        currentChange(pageIndex) {
            if (this.notPage) { return }
            this.setCurrent(pageIndex);
            this.autoGetList({}, 'pageIndex');
            this.$emit('current-change');
        },
        currentInput(pageIndex){
            this.setCurrent(pageIndex);
        },
        // 设置页数
        setPsize(pageSize) {
            if (typeof this.page?.pageSize !== 'undefined') {
                this.page.pageSize = pageSize
            }
            this.setPageData('pageSize',pageSize);
        },
        // 页数改变
        sizeChange(pageSize) {
            if (this.notPage) { return }
            this.setPsize(pageSize);
            this.autoGetList({}, 'pageSize');
            this.$emit('size-change');
        },
        sizeInput(pageSize){
            this.setPsize(pageSize);
        },
        getList(type, param) {
            this.$nextTick(() => {
                this.getTableList(param || {}, type || 'init')
            })
        },
        resetList(){
            this.selectListData = {};
            this.formData = {};
            this.pageData = {
                pageSize: 10,
                pageIndex: 1,
                total: 0
            };
            this.getList('init');
        },
        initTabeList(){
            this.initDebounce(()=>{
                this.$nextTick(() => {
                    // 获取当前页面分页数据
                    this.getPageData();
                    // 获取当前页面查询数据
                    this.getFormData();
                    // 是否首次发起
                    if (this.notFirstGet) { return }
                    // 首次发起请求
                    this.getList();
                })
            })
        }
    },
    created() {

    },
    mounted() {
        this.initTabeList()
    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() {
        this.formData = {}
    }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() {
        // 是否启动缓存不加载
        // if (!this.notActivated) {
        //     this.getList()
        // }
        this.initTabeList();
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.core-search {
    padding: 16px;
    margin-bottom: 12px;

    // ::v-deep(.el-form-item) {
    //     margin-bottom: 0 !important;
    // }
}

.core-table {
    border-radius: 10px 10px 0 0;

    ::v-deep(.el-table::before) {
        height: 0px !important;
    }
}

.searchRight {
    display: flex;
    flex-direction: row-reverse;
}

.core-page {
    border-radius: 0 0 10px 10px;
    padding-bottom: 10px;
    padding-top: 10px;
    margin-top: 0px;
}
</style>
