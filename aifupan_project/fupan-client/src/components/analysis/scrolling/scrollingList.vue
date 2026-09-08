<template>
    <div style="overflow: hidden;" class="h100 p-r">
        <div v-if="search">
            <el-form class="scrolling-form" label-width="40px"  :model="dataForm" size="medium">
                <el-row>
                    <el-col v-if="!notScrollingType" :span="24" class="mg-b10 flex-jc-sb">
                        <div>
                            <el-radio-group v-model="dataForm.scrollingType" :disabled="tabLaoding" size="small" @change="scrollingChange">
                                <el-radio-button label="all">所有弹幕</el-radio-button>
                                <el-radio-button label="core">重要弹幕</el-radio-button>
                            </el-radio-group>
                        </div>
                        <div>
                            <afp-button type="primary" size="small" :plain="false" class="ml-5" @click="exportData">导出</afp-button>
                        </div>
                    </el-col>
                    <el-col v-if="importantBarrageStatus === 2" :span="getInline ? 14 : 24">
                        <el-form-item label="搜索">
                            <el-input v-model="dataForm.search" size="medium" class="select-search" placeholder="请输入搜索内容" clearable
                                @input="onSearchNameOrContent">
                                <el-select v-model="dataForm.select" slot="prepend" placeholder="请选择"
                                    @change="clearSearch">
                                    <el-option label="昵称" value="0"></el-option>
                                    <el-option label="内容" value="1"></el-option>
                                </el-select>
                            </el-input>
                        </el-form-item>
                    </el-col>
                    <el-col  v-if="importantBarrageStatus === 2" :span="getInline ? 10 : 24">
                        <el-form-item label="筛选">
                            <el-select v-model="dataForm.filter" class="filter-input" style="width: 100%;" size="medium" clearable placeholder="请选择筛选条件"
                                @change="filterChange">
                                <el-option-group v-for="group in options" class="font-s12" style="width: 100%"
                                    :key="group.label" :label="group.label">
                                    <el-option v-for="item in group.options" class="font-s12" :key="item.value"
                                        :label="item.label" :value="item.value">
                                        {{ item.label }}
                                    </el-option>
                                </el-option-group>
                            </el-select>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
        </div>
<!--        <div class="scrolling-box" v-if="saveData.important===1&&scrollingDatas?.length===0"> 获取重要弹幕 </div>-->
        <div v-if="scrollingDatas?.length && importantBarrageStatus === 2" class="scrolling-box"
        :class="getScrollingBoxClass"
            @mousewheel="wheelHander" @scroll="handleScroll" @contextmenu.prevent="rightContextMenuHanlder">
            <div v-if="scrollingMap.previousHash && search" class="top-more">
                <el-button class="text-center" style="width:100%" type="text" @click="moreList(0)">加载更多...</el-button>
            </div>
            <scrolling v-for="(item, index) in scrollingDatas" :key="index" :item="item"
                :class="`scrolling-${item.recordDate}-${item.index}`" :deductionTime="getInitStartTime" :dataDisplay="dataDisplay" :ai="!!ai" :isShowTime="ai || search">
                <template #name="item">
                    <span class="font-s12 text-color3 cs-p" @click.stop="nickNameClick(item)">
                        {{ item.nickName }}
                    </span>
                </template>
            </scrolling>
            <div v-if="scrollingMap.nextHash && search" class="down-more">
                <el-button class="text-center" style="width:100%" type="text" @click="moreList(1)">加载更多...</el-button>
            </div>
        </div>
        <div v-else class="h100" :class="{ 'scrolling-box-search': search }">
            <div v-if="(importantBarrageStatus === 0 || importantBarrageStatus === 3) && isCore" class="flex-ji-c flex-ai-c h100 w100">
                <el-button type="primary"  size="mini" plain @click="startImportantBarrage">点我获取重要弹幕</el-button>
            </div>
            <div v-else-if="importantBarrageStatus === 1  && isCore" class="flex-ji-c flex-ai-c h100 w100">
                 <letterSpacing text="重要弹幕正在获取中,请五分钟后再查看..." :position="-3"></letterSpacing>
            </div>
            <div v-else-if="!tabLaoding" class="flex-ai-c flex-jc-c font-s12 text-color3 h100">
                <div v-if="ai"><img src="@/assets/imgs/chartEmpty.png" style="max-height: 160px" alt="" srcset=""></div>
                <div v-else>
                    <div>直播时间段暂无弹幕...</div>
                    <div>（弹幕仅保存60天）</div>
                </div>
            </div>
            <div v-else  class="flex-ai-c flex-jc-c font-s12 text-color3 h100">
                弹幕加载中...
            </div>
        </div>
        <namePop ref="namePop" v-model="visible" :startTime="getStartTime" @hide="hide" :isAi="!!ai" @getAll="getNickNameAllList"
            @getOther="getOtherAllList">
            <span></span>
        </namePop>
        <exportDialog ref="exportDialog" ></exportDialog>
    </div>
</template>

<script>
import scrolling from '/src/components/analysis/scrolling/index.vue';
import namePop from './namePop.vue';
import myUtils from './../../../utils/utils';
import letterSpacing from '/src/components/letterShake/index.vue';
import exportDialog from './export.vue';
// import setTimeOutMixin from '@/mixins/setTimeOut'
export default {
    components: {
        scrolling,
        letterSpacing,
        namePop,
        exportDialog
    },
    // mixins: [setTimeOutMixin],
    props: {
        search: {
            type: Boolean,
            default: false
        },
        sentenceMarkData: {
            type: Object,
            default: () => { return {} }
        },
        targetType: {
            type: String,
            default: ''
        },
        startTime: {
            type: [Number, String],
            default: ''
        },
        endTime: {
            type: [Number, String],
            default: ''
        },
        inline: {
            type: Boolean,
            default: false
        },
        ai:{
            type: Boolean,
            default: false
        },
        notScrollingType: {
            type: Boolean,
            default: false
        },
        scrollAutoLoad: {
            type: Boolean,
            default: false
        },
        dataDisplay: {
            type: Object,
            default: () => {return{}}
        },
        tabScrollingType:{
            type: String,
            default: ''
        }
    },
    data() {
        return {
            dataForm: {
                select: '0',
                scrollingType: 'all',
            },
            scrollingDatas: [
                // {name: '青青小绵羊',text: '哈哈哈哈哈！！' ,new: true, dydj: 5, fstdj: '4-5'},
                // {name: '青青小绵羊',text: '哈哈哈哈哈！！' ,new: true, dydj: 5, fstdj: '4-5'},
                // {name: '小小飞',text: '老板更赚钱~！运营更省心~！主播更省力~!' ,new: false, dydj: 35, fstdj: '5-7'},
                // {name: '小小飞',text: '老板更赚钱~！运营更省心~！主播更省力~!' ,new: false, dydj: 35, fstdj: '5-7'},
            ],
            exportParams: {},
            scrollingMap: {},
            lock: false,
            tabLaoding: true,
            saveData: {},
            getListDebounce: myUtils.debounce(1000),
            visible: false,
            clickPopIndex: -1,
            searchData: {},
            options: [
                {
                    label: '抖音等级',
                    options: [
                        { value: 'user_0', label: '1级≤ 抖音等级 <15级' },
                        { value: 'user_1', label: '15≤级 抖音等级 <20级' },
                        { value: 'user_2', label: '20≤级 抖音等级 <25级' },
                        { value: 'user_3', label: '25≤级 抖音等级 <30级' },
                        { value: 'user_4', label: '抖音等级 30级以上' },
                    ]
                },
                {
                    label: '初始粉丝牌等级',
                    options: [
                        { value: 'init_00', label: '0级≤ 粉丝等级 <1级' },
                        { value: 'init_0', label: '1级≤ 粉丝等级 <5级' },
                        { value: 'init_1', label: '5级≤ 粉丝等级 <10级' },
                        { value: 'init_2', label: '粉丝等级 10级以上' },
                    ]
                },
                {
                    label: '最终粉丝牌等级',
                    options: [
                        { value: 'fans_00', label: '0级≤ 粉丝等级 <1级' },
                        { value: 'fans_0', label: '1级≤ 粉丝等级 <5级' },
                        { value: 'fans_1', label: '5级≤ 粉丝等级 <10级' },
                        { value: 'fans_2', label: '粉丝等级 10级以上' },
                    ]
                },
                {
                    label: '直播间新发言',
                    options: [
                        { value: 'isNew_1', label: '新发言：是' },
                        { value: 'isNew_0', label: '新发言：否' },
                    ]
                },
            ],
            optionValueMap: {
                isNew_1: { isNew: 1 },
                isNew_0: { isNew: 0 },
                init_00: { minInitFansLevel: 0, maxInitFansLevel: 1 },
                init_0: { minInitFansLevel: 1, maxInitFansLevel: 5 },
                init_1: { minInitFansLevel: 5, maxInitFansLevel: 10 },
                init_2: { minInitFansLevel: 10 },
                fans_00: { minFinallyFansLevel: 0, maxFinallyFansLevel: 1 },
                fans_0: { minFinallyFansLevel: 1, maxFinallyFansLevel: 5 },
                fans_1: { minFinallyFansLevel: 5, maxFinallyFansLevel: 10 },
                fans_2: { minFinallyFansLevel: 10 },
                user_0: { minLevel: 1, maxLevel: 15 },
                user_1: { minLevel: 15, maxLevel: 20 },
                user_2: { minLevel: 20, maxLevel: 25 },
                user_3: { minLevel: 25, maxLevel: 30 },
                user_4: { minLevel: 30 },
            },
            searchDebounce: myUtils.debounce(1500),
            searchLock: false,
            popItem: null,
            oldTime: 0,
            _loadingPrevious: false,
            _loadingNext: false,
            // importantBarrageStatus 0未获取，1获取中，2获取成功，3获取失败
            importantBarrageStatus: 0,
            getImportantBarrageStatusTimeout: null
        };
    },
    watch: {
        search: {
            handler(val) {
                if (!val) {
                    this.initFormData('search')
                }
            },
            immediate: true
        },
        tabLaoding:{
            handler(val){
                this.$emit('tabLoading', val)
            }
        }
    },
    computed: {
        isCore(){
            return (this.tabScrollingType || this.dataForm.scrollingType) === 'core'
        },
        getStartTime() {
            return this.startTime !== '' ? this.startTime : myUtils.toSecondByDate(this.sentenceMarkData?.videoInfo?.StartTime)
        },
        getEndTime() {
            return this.endTime !== '' ? this.endTime : myUtils.toSecondByDate(this.sentenceMarkData?.videoInfo?.EndTime)
        },
        getBatchNumber() {
            return this.sentenceMarkData?.videoInfo?.BatchNumber
        },
        getInitStartTime(){
            return (new Date(this.sentenceMarkData?.videoInfo?.StartTime)).getTime()
        },
        getVideoId() {
            return this.sentenceMarkData?.videoInfo?.VideoId
        },
        getSecUid() {
            return this.sentenceMarkData?.anchorInfo?.SecUid
        },
        isWebOnline() {
            return this.targetType === 'webOnline';
        },
        getInline(){
            return this.search || this.inline
        },
        
        getScrollingBoxClass() { 
            if(this.getInline && this.notScrollingType){
                return 'scrolling-inline-notScrollingType-box'
            }else if(this.notScrollingType || this.getInline){
                return 'notScrollingType-box'
            }else if(this.search){
                return 'scrolling-box-search'
            }
        }, 

    },
    methods: {
        /**
         * 导出弹幕
         * @description 点击按钮时触发，显示导出弹窗
         */
        exportData(){
            this.$refs.exportDialog.show({
                data: this.exportParams
            });
        },
        /**
         * 开始获取重要弹幕
         * @description 点击按钮时触发，设置状态为获取中并调用接口
         */
        startImportantBarrage(){
            this.importantBarrageStatus = 1;
            this.$httpBack.words.startImportantBarrage({
                videoId: this.getVideoId,
            }).then(res=>{
                // 接口调用成功后立即查询一次状态
                this.ifImportantBarrageStatus();
            }).catch(err=>{
                this.importantBarrageStatus = 3;
                // this.$message.error('启动重要弹幕获取失败');
            })
        },
        /**
         * 轮询查询重要弹幕获取状态
         * @description 每5秒查询一次，直到状态变为成功或失败
         */
        ifImportantBarrageStatus(){
            // 先清理之前的定时器，防止重复执行
            if(this.getImportantBarrageStatusTimeout){
                clearTimeout(this.getImportantBarrageStatusTimeout);
                this.getImportantBarrageStatusTimeout = null;
            }
            
            this.$httpBack.words.getImportantBarrageStatus({
                videoId: this.getVideoId,
            }).then(res=>{
                if(res.data.importantBarrageStatus === 1){
                    // 获取中，继续轮询
                    this.getImportantBarrageStatusTimeout = setTimeout(()=>{
                        this.ifImportantBarrageStatus();
                    },5000);
                }else if(res.data.importantBarrageStatus === 2){
                    // 获取成功，直接调用滚动切换，无需检查当前类型
                    if(this.dataForm.scrollingType === 'core'){
                        this.scrollingChange('core');
                    }
                }else if(res.data.importantBarrageStatus === 3){
                    // 获取失败，显示错误信息
                    this.$message.error(res.data.errReason || '获取重要弹幕失败');
                }
                
                // 更新状态
                this.importantBarrageStatus = res.data.importantBarrageStatus;
            }).catch(err=>{
                // 请求失败，设置状态为失败
                this.importantBarrageStatus = 3;
                this.$message.error('查询重要弹幕状态失败');
            })
        },
        rightContextMenuHanlder(event){
            this.$emit('rightContext', event)
        },
        scrollingChange(type){
            this.scrollingDatas = [];
            if(typeof type !== 'undefined'){
                this.setScrollingType(type);
            }
            this.$nextTick(()=>{
                this.saveData.important = type === 'all'? 0: 1;
                this.initLoadScrolling({},'change');
            })
        },
        nickNameClick(item) {
            this.visible = true;
            this.$refs.namePop.setTiem(item);
        },
        hide() {
            this.visible = false;
        },
        setScrollingType(type){
            this.$set(this.dataForm, 'scrollingType', type);
        },
        // 初始化表单数据和搜索数据
        initFormData(type) {
            this.dataForm = {
                select: '0',
                scrollingType: this.dataForm.scrollingType,
            };
            this.searchData = {};
        },
        // 清楚搜索数据
        clearSearch() {
            this.dataForm.search = '';
            this.$delete(this.searchData, 'nickNameLike');
            this.$delete(this.searchData, 'contentLike');
        },
        // 清除过滤数据
        clearFiler() {
            this.$delete(this.searchData, 'isNew');
            this.$delete(this.searchData, 'minInitFansLevel');
            this.$delete(this.searchData, 'maxInitFansLevel');
            this.$delete(this.searchData, 'minFinallyFansLevel');
            this.$delete(this.searchData, 'maxFinallyFansLevel');
            this.$delete(this.searchData, 'minLevel');
            this.$delete(this.searchData, 'maxLevel');
        },
        // 设置搜索数据分页参数
        setSearchDataPage() {
            if (this.dataForm.search || this.dataForm.filter) {
                this.searchData.queryType = 4;
                this.searchData.limit = 500;
                this.searchData.page = 1;
            }
        },
        // 判断是否清翻页数据
        clearPage() {
            if (this.dataForm.search || this.dataForm.filter) { return }
            this.$delete(this.searchData, 'queryType');
            this.$delete(this.searchData, 'limit');
            this.$delete(this.searchData, 'page');
        },
        // 搜索文本改变自动触发搜索/清理
        onSearchNameOrContent() {
            // 防抖处理
            this.searchDebounce(() => {
                this.searchChange();
            })
        },
        searchChange() {
            let key = ['nickNameLike', 'contentLike'];
            // 清空数据再次重新赋值。
            this.clearPage();
            if (this.dataForm.search) {
                // 每次有数据改变将会刷新分页数据，以及搜索参数
                this.searchData[key[this.dataForm.select]] = this.dataForm.search;
                this.setSearchDataPage();
            } else {
                // 如果数据为空则判断是否清理掉数据
                this.clearSearch();
            }
            // 执行搜索。
            this.getScrollingDatas(this.searchData)
        },
        // 过滤条件变更自动触发搜索/清理
        filterChange() {
            let o = this.optionValueMap[this.dataForm.filter] || false;
            //清空数据再次重新赋值。
            this.clearFiler();
            if (o) {
                // 每次过滤数据改变将会刷新分页数据，以及过滤数据参数
                this.searchData = {
                    ...this.searchData,
                    ...o
                }
                this.setSearchDataPage();
            } else {
                // 如果数据为空则判断是否清理掉数据
                this.clearPage();
            }
            this.getScrollingDatas(this.searchData);
        },
        // 上一页/下一页
        moreList(type) {
            // this.searchData.queryType = 4 表示当前正在执行搜索任务
            let o = {}
            if (this.searchData.queryType === 4) {
                this.searchData.page += 1;
            } else {
                // 一般来说type只有0/1，queryType = 4 得时候执行搜索逻辑在上面。
                // 如果是queryType=2 的时候则是不会出现更多加载.
                // 点击更多加载得情况，要么queryType = 4，要么为0/1
                // 判断上一页还是下一页，进行页数计算。
                if (type === 1) {
                    this.scrollingMap.nextPage += 1;
                } else {
                    this.scrollingMap.previousPage += 1;
                }
                // page是加载完成直接进入弹幕操作页面时，记录页数用。type: 表示上一页，下一页
                let page = type ? this.scrollingMap.nextPage : this.scrollingMap.previousPage;
                // 储存需要翻页得数据
                o = {
                    queryType: type,
                    page,
                }
            }
            
            this.getScrollingDatas({ ...o, ...this.searchData });
        },
        // 鼠标滚动锁定运行视频时加载。
        wheelHander() {
            this.lock = true;
            this.visible = false;
        },
        // 处理滚动事件，监听滚动到顶部或底部时自动加载更多
        handleScroll(e) {
            // 如果不在搜索模式下，或者没有previousHash和nextHash，则不处理
            if (!this.scrollAutoLoad) return;
            // 获取滚动容器元素
            const scrollElement = e.target;
            
            
            // 滚动到顶部时加载上一页 - 当滚动距离顶部小于20px且previousHash存在
            if (scrollElement.scrollTop <= 20 && this.scrollingMap.previousHash) {
                // 防止频繁触发，设置一个节流处理
                if (!this._loadingPrevious) {
                    this._loadingPrevious = true;
                    this.moreList(0); // 加载上一页
                    // 一定时间后重置状态，允许再次触发
                    setTimeout(() => {
                        this._loadingPrevious = false;
                    }, 500);
                }
            }
            
            // 滚动到底部时加载下一页 - 当滚动距离底部小于20px且nextHash存在
            const scrollBottom = scrollElement.scrollHeight - scrollElement.scrollTop - scrollElement.clientHeight;
            if (scrollBottom <= 20 && this.scrollingMap.nextHash) {
                // 防止频繁触发，设置一个节流处理
                if (!this._loadingNext) {
                    this._loadingNext = true;
                    this.moreList(1); // 加载下一页
                    // 一定时间后重置状态，允许再次触发
                    setTimeout(() => {
                        this._loadingNext = false;
                    }, 500);
                }
            }
        },
        async getNickNameAllList(option, callback) {
            if (typeof callback === 'function') {
                let o = await this.getScrollingDatas({ ...option, queryType: 3 });
                callback(o);
            }
        },
        async getOtherAllList(options, callback) {
            if (typeof callback === 'function') {
                let o = await this.getOtherList(options);
                callback(o);
            }
        },
        getOtherList(param) {
            let params = {
                recordDate: this.getStartTime * 1000,
                secUid: this.getSecUid,
                "batchNumber": this.getBatchNumber,
                "videoId": this.getVideoId,
                ...param
            }
            let id = 0;
            return this.$httpBack.v2100.queryOtherDanMuData(params).then(res => {
                if (res.code == 0) {
                    return res.data.map(d => {
                        d.id = `${++id}`;
                        d.list?.map((sd, index) => {
                            sd.id = `${id}-${index}`;
                            return sd;
                        })
                        return d
                    })
                }
            });
        },
        getScrollingDatas(param) {
            const parentVideoInfo = this.sentenceMarkData?.videoInfo?.parentVideoInfo || {};
            let params = {
                limit: 50,
                page: 1,
                queryType: 2,
                startTime: this.getStartTime * 1000, // 用于锁定时间，不查询时间之前的数据,
                endTime: this.getEndTime * 1000,
                videoId: parentVideoInfo?.videoId || this.getVideoId,
                batchNumber: this.getBatchNumber,
                // "batchNumber": "7460987530188442380",
                // "userId": "4394529311009275904",
                // "tenantId": "0",
                // "videoId": "5903f374-2d5a-4ced-a7d6-30663bbf9735",
                parentVideoInfo: parentVideoInfo,
                ...this.saveData,
                ...param,
            }
            // 是否是重要弹幕
            const isImportant = params.important === 1;

            if(this.ai){
                params.isBlessBag = 0;
            }
            this.$emit('scrollingDatasParams', params);
            this.exportParams = params;
            return this.$httpBack.v2100.queryDanMuData(params, { load: false }).then(res => {
                this.tabLaoding = false;
                // importantBarrageStatus 0未获取，1获取中，2获取成功，3获取失败
            if(isImportant && res.data.importantBarrageStatus !== 2){
                // 如果当前正在轮询获取状态，则不覆盖状态，避免冲突
                if(!this.getImportantBarrageStatusTimeout){
                    this.importantBarrageStatus = res.data.importantBarrageStatus || 0;
                }
                return;
            }
            this.importantBarrageStatus = 2;
                if (res.code === 0) {
                    //0:向上查 1:向下查 2:前后 3:昵称 4:向下搜索
                    if (params.queryType !== 3) {
                        if (params.queryType === 2) {
                            this.setScrollingDatas(res.data, 2,  {initPage: true});
                        } else {
                            // 4搜索数据
                            if (params.queryType === 4) {
                                // page不为1 则插入数据
                                if (params.page !== 1) {
                                    this.setScrollingDatas({
                                        ...res.data,
                                        list: [].concat(this.scrollingDatas,res.data.list)
                                    },param.queryType)
                                } else {
                                    // 如果为1 则刷新数据
                                    this.setScrollingDatas(res.data,param.queryType, {initPage: true});
                                }
                            } else if (params.queryType === 1) {
                                // 为1 表示加载完之后脱离播放进度，未搜索 点击上一页下一页。
                                this.setScrollingDatas({
                                    ...res.data,
                                    list: [].concat(this.scrollingDatas,res.data.list)
                                },param.queryType)
                            } else {
                                this.setScrollingDatas({
                                    ...res.data,
                                    list: [].concat(res.data.list, this.scrollingDatas)
                                },param.queryType)
                            }
                        }
                    } else {
                        return res.data.list;
                    }
                }
            })
        },
        // 设置弹幕播放时数据。
        setScrollingDatas(data, type, options={}) {
            const {initPage} = options;
            const { list, nextHash, previousHash } = data;
            this.scrollingDatas = list;
            let obj = {}
            list?.forEach((d, index) => {
                d.index = index;
                if (typeof obj[d.recordDate] === 'undefined') {
                    obj[d.recordDate] = {
                        recordDate: d.recordDate,
                        index: index,
                        list: []
                    }
                }
                obj[d.recordDate].list.push(d);
                // 总数减去，当前已经获取的条数计算剩余的弹幕条数
                obj[d.recordDate].Remaining = list.length - (list.length - (index));
            })
            obj.maxTime = list?.[list.length - 1]?.recordDate || 0
            obj.minTime = list?.[0]?.recordDate || 0
            obj.nextHash = nextHash;
            obj.previousHash = previousHash;
            if(initPage){
                obj.nextPage = 1;
                obj.previousPage = 1;
            }else{
                obj.nextPage = this.scrollingMap.nextPage;
                obj.previousPage = this.scrollingMap.previousPage;
            }
            this.scrollingMap = obj;
            
            // 如果是2 则初始化时间,将旧的时间戳设置为无穷大的情况，就会取消滚轮动画
            if(type === 2){
                this.oldTime = 9999999999999;
            }
        },
        // 执行弹幕滚动
        operation(recordDate) {
            if (this.search || this.lock) { return }


            // 获取最大时间戳
            let maxT = (this.scrollingMap.maxTime || 0)
            // 计算当前时间戳
            let r = (this.getStartTime + Math.ceil(recordDate)) * 1000;
            // 初始化时间，如果最大时间错和当前时间戳间隔小于5秒，或者当前视频播放时时间为小于1秒则会清空数据，重新获取数据。
            if (maxT - r < 5000 && recordDate < 1) {
                this.scrollingDatas = [];
                this.scrollingMap = {};
                this.saveData.recordDate = this.getStartTime * 1000;
            } else {
                // 保存请求时间
                this.saveData.recordDate = r;
            }
            // 如果当前播放时间和最后一条弹幕得时间戳相差5s以上将会不重拉数据,如果一直有数据，并且数据大于5条，则不请求数据
            // 获取当前滚动时间范围的最大值和最小值
            let max = this.scrollingMap.maxTime, min = this.scrollingMap.minTime;
            // 获取当前滚动时间对应的数据对象
            let o = this.scrollingMap[r];
            // 判断当前滚动时间是否在最大值和最小值的范围内（考虑5000毫秒的偏移量）
            let containBl = (max && max > (r + 5000)) && (min && min < (r - 5000));
            // 判断当前数据对象是否存在且其剩余时间大于5秒
            let oLen5 = o && o?.Remaining > 5;
            // 判断是否不存在下一个哈希值，且当前滚动时间大于最大值
            let nextBl = this.scrollingMap.nextHash === false && (max && max < r);
            // 如果当前播放时间和最后一条弹幕得时间戳相差5s以上将会不重拉数据,如果一直有数据，并且数据大于5条，则不请求数据
            if(containBl || oLen5) {
                this.toScrollingDom(o, r);
                return
            }
            if(nextBl){
                return
            }
            this.tabLaoding = true;
            this.getListDebounce(() => {
                // 计算当前时间下还剩多少条数据，如果少于5条数据 将会再次请求
                this.initFormData('operation');
                this.getScrollingDatas();
            })
        },
        // getImportantData() {
        //
        // },
        // getImportantDataStatus(){
        //     let a=1
        //     const abc=()=>{
        //         console.log(2222222222)
        //         a++
        //         if(a<10){
        //             this.scheduleNextPoll(500,abc)
        //         }
        //     }
        //     this.scheduleNextPoll(500,abc)
        // },
        /**
         * 初始化加载滚动数据
         * @param {Object} param - 传递的参数对象，用于加载数据
         */
        initLoadScrolling(param,type) {
            this.tabLaoding = true;
            this.getListDebounce(() => {
                // 计算当前时间下还剩多少条数据，如果少于5条数据 将会再次请求
                this.saveData.recordDate = this.getStartTime * 1000;
                this.initFormData(type);
                this.getScrollingDatas(param);
            })
        },
        
        /**
         * 执行弹幕滚动跳转
         * @param {Object|string} data - 弹幕数据对象或标识符
         */
         toScrollingDom(data, time) {
            if (typeof data === 'object') {
                let dom = document.querySelector(`.scrolling-${data.recordDate}-${data.index}`);
                if (dom) {
                    this.$nextTick(() => {
                        if(this.oldTime > time){
                            dom?.scrollIntoView({ behavior: "auto" })
                        }else{
                            dom?.scrollIntoView({ behavior: "smooth" })
                        }
                        this.oldTime = time;
                    })
                }
            }
        }
    },
    created() {
        // console.log(this.sentenceMarkData);
        // this.getImportantDataStatus()
    },
    mounted() {
        
    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() { }, //生命周期 - 销毁之前
    destroyed() { 
            // 清理定时器，防止内存泄漏
            if(this.getImportantBarrageStatusTimeout){
                clearTimeout(this.getImportantBarrageStatusTimeout);
            }
        }, //生命周期 - 销毁完成
    activated() { }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.scrolling-box {
    overflow: hidden;
    overflow-y: auto;
    height: 100%;
}

.scrolling-box-search {
    height: calc(100% - 105px);
}

.notScrollingType-box {
    height: calc(100% - 75px);
}
.scrolling-inline-notScrollingType-box {
    height: calc(100% - 40px);
}

.scrolling-form {
    ::v-deep(.el-form-item) {
        margin-bottom: 8px;

        .el-form-item__label {
            font-size: 12px;
        }

        .select-search {
            .el-select .el-input {
                width: 60px;

                .el-input__inner {
                    padding: 0 5px;
                }
            }
            .el-input__inner{
                font-size: 12px;
            }
        }

        .filter-input{
            .el-input__inner{
                font-size: 12px;
            }
        }

    }
}
</style>
