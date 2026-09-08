import Vue from 'vue'
import Vuex from 'vuex'
import createPersistedstate from 'vuex-persistedstate'
import {VERSION_TYPE} from "@/enum";

Vue.use(Vuex)

const getVersionTypeValue = (response) => {
  const data = response?.data
  if (typeof data === 'string') {
    return data
  }
  if (data && typeof data === 'object') {
    return data?.clientVersion
  }
  return ''
}

const PERSIST_PATHS = ['userInfo', 'token', 'replayPageMenu', 'homeMenu', 'startDetectionTime', 'versionType']
const store = new Vuex.Store({
  plugins: [createPersistedstate({
    key: 'replayVuex',
    paths: PERSIST_PATHS
  })],
  state: {
    versionType: '',
    userInfo: '',
    replayPageMenu: {},
    token: '',
    homeMenu: "",
    detectionStatus: false,
    startDetectionTime: null,
    userproperty:{},
    tabs: [],
    tabsName: '',
    tabsClickCallback:null,
    configInfo: {},
    TimeAccurate: 0,
    titleRightRender: null,
    mode: false, // 开发者模式
    compereInfo: {},//主播信息
    domZoom:1,
    routerPathBack:null,
    machineCode:'',//电脑 mac地址
    actionKey:0,
    versionTypeLoading: false,
    pureValue: '',//纯录制版版本值
  },
  getters:{
    getUserproperty:(state)=>{
      return state.userproperty;
    },
    getTabsName:(state)=>{
      return state.tabsName;
    },
    getTabsList:(state)=>{
      return state.tabs;
    },
    getToken:(state)=>{
      return state.token;
    },
    getBiscInfo:(state)=>{
      return state.configInfo
    },
    getDetectionStatus:(state)=>{
      return state.detectionStatus;
    },
    getUserInfo:(state)=>{
      return state.userInfo;
    },
    getUserType:(state)=>{
      return state.userInfo?.userType
    },
    getNickName:(state)=>{
      return state.userInfo?.nickName
    },
    getTimeAccurate:(state)=>{
      return state.TimeAccurate
    },
    // -1 激活版 0免费版 10个人 15工作室 20企业 30旗舰
    getPackageLevel:(state)=>{
      return state?.userInfo?.packageLevel
    },
    getPackageLevelName:(state)=>{
      return state?.userInfo?.packageName;
    },
    isFree:(state)=>{
      return state?.userInfo?.packageLevel === 0
    },
    isPure: (state) => {
      return state?.userInfo?.packageLevel == state.pureValue
    },
    isActivated:(state)=>{
      return state?.userInfo?.packageLevel === -1
    },
    largeEnterprises:(state)=>{
      return state?.userInfo?.packageLevel >= 20
    },
    getMode:(state)=>{
      return state.mode === 0;
    },
    getDomZoom:(state)=>{
      return 1;//state.domZoom;
    },
    getMachineCode:(state)=>{
      return state.machineCode
    },
    getActionKey:(state)=>{
      return state.actionKey
    },
    getCompereInfo:(state)=>{
      return state.compereInfo
    },
    getVersionType:(state)=>{
      const ua = (typeof navigator !== 'undefined' && navigator.userAgent) ? navigator.userAgent : ''
      const isAiFuPan = ua.indexOf('aifupan') >= 0
      if (!isAiFuPan) {
        if (state.versionType) {
          store.dispatch('initVersionType')
        }
        return ''
      }
      store.dispatch('initVersionType')
      return state.versionType
    },
  },

  mutations: {
    setVersionType(state, val) {
      state.versionType = val
    },
    setVersionTypeLoading(state, val) {
      state.versionTypeLoading = val
    },
    setMode:(state,data)=>{
      state.mode = data
    },
    setTitleRightRender(state,render){
      state.titleRightRender = render;
    },
    setBiscInfo(state,data){
      state.configInfo = data;
    },
    setTabsName(state,name){
      state.tabsName = name;
    },
    setTabsCallback(state,callback){
      state.tabsNameCallback = callback;
    },
    setTabsList(state,list){
      state.tabs = list
    },
    /**
    * 设置标签点击回调函数
    *
    * @param {Object} state - 包含状态的对象
    * @param {Function} callback - 当标签被点击时执行的回调函数
    */
    setTabsClickCallback(state,callback){
      state.tabsClickCallback = callback;
    },
    setTimeAccurate(state){
      state.TimeAccurate +=1
    },
    // 保存登录返回的信息
    saveLoginResultData(state, val) {
      state.token = val ? (val.token || '') : ''
	  state.versionType = val ? (val.versionType || '') : ''
      state.userInfo = val || ''
    },
    // 保存用户信息
    saveUserInfo(state, val) {
      state.userInfo = val || '';
    },
    // 设置是否加载了动态路由标识
    setLoadRouterFlag(state, val) {
      state.loadRouterFlag = val;
    },
    // 保存复盘页面菜单标识
    saveReplayPageMenu(state, val) {
      state.replayPageMenu = val;
    },
    // 保存首页菜单索引
    saveHomeMenu(state, val) {
      state.homeMenu = val;
    },
    // 保存设置页菜单索引
    saveSetupMenu(state, val) {
      state.setupMenu = val;
    },
    // 保存检测状态
    saveDetectionStatus(state, val) {
      state.detectionStatus = val;
    },
    // 保存检测开始时间
    saveDetectionTime(state, val) {
      state.startDetectionTime = val;
    },
    // 保存分析的视频id
    saveVideoAnalysisId(state, val) {
      state.videoAnalysisId = val;
    },
    // 保存分析的文件id
    saveFileAnalysisId(state, val) {
      state.fileAnalysisId = val;
    },
    // 保存进入主播列表页是否停止录制标识
    saveStopRecord(state, val) {
      state.stopRecord = val;
    },
    // 保存选择分析的行业数组
    saveAnalysisTrade(state, val) {
      state.analysisTrade = val;
    },
    // 保存视频列表页的查询参数
    saveVideoListQuery(state, val) {
      state.videoListQuery = val;
    },
    // 保存文件列表页的查询参数
    saveFileListQuery(state, val) {
      state.fileListQuery = val;
    },
    // 保存视频完成列表页的查询参数
    saveFinishVideoListQuery(state, val) {
      state.finishVideoListQuery = val;
    },
    // 保存对比列表页的查询参数
    saveContrastListQuery(state, val) {
      state.contrastListQuery = val;
    },
    // 保存当前上传的视频id
    saveCurrentUploadVideoId(state, val) {
      state.currentUploadVideoId = val;
    },
    saveUserproperty(state,val){
      state.userproperty = val;
    },
    setDomZoom:(state,val)=>{
       state.domZoom = val
    },
    setMachineCode:(state,val)=>{
      state.machineCode = val
    },
    setActionKey:(state,val)=>{
      state.actionKey = val
    },
    setCompereInfo:(state,val)=>{
      state.compereInfo = val
    },
    setPureValue:(state,val)=>{
      state.pureValue = val
    },
  },
  actions: {
    async initVersionType({commit, state}) {
      const ua = (typeof navigator !== 'undefined' && navigator.userAgent) ? navigator.userAgent : ''
      const isAiFuPan = ua.indexOf('aifupan') >= 0
      if (!isAiFuPan) {
        if (state.versionType) {
          commit('setVersionType', '')
        }
        return ''
      }
      if (state.versionType && [VERSION_TYPE.AGENT, VERSION_TYPE.PURE].includes(state.versionType)) {
        return state.versionType
      }
      if (state.versionTypeLoading) {
        return state.versionType
      }

      commit('setVersionTypeLoading', true)
      try {
        const httpClient = require('@/utils/request-api-client').default;
        const response = await httpClient.setup.getVersionType();
        const clientVersion = getVersionTypeValue(response)
        const versionType = clientVersion === 'replay' ? VERSION_TYPE.AGENT : VERSION_TYPE.PURE;
        if (clientVersion) {
          commit('setVersionType', versionType)
        }
        return versionType
      } catch (error) {
        return ''
      } finally {
        commit('setVersionTypeLoading', false)
      }
    },
    async getPurePackageLevel({commit, state}) {
      if(state.pureValue) return
      try {
        const httpBack = require('@/utils/request-api-back').default;
        const response = await httpBack.common.getByKey('pure_recording_version_level')
        const {kvValue} = response?.data || {}
        commit('setPureValue', kvValue)
      } catch (error) {
        commit('setPureValue', 1)
      }
    },
  },
  modules: {
  }
})

export default store;
