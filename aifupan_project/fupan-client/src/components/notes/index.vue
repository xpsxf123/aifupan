<template>
    <div class="pd-l10 h100">
        <div class="pd-10 main-bg brs-8 h100 p-r">
            <div class="p-a" style="right: 10px;top: 15px; z-index: 99;">
                <afp-button v-if="isSelectTabsName" type="primary" :plain="false"  @click="exportNotes">导 出</afp-button>
            </div>
            <tabs :tabs="getTabs" v-model="notesKey" @click="tabsClick" :reload="true" class="notes-tabs-box"
                style="height: calc(100% - 30px);">
                <template #text>
                    <div class="edit-time font-s14 pd-b12 flex-jc-sb">
                        <span>最近编辑时间: {{ editInfoMap['text'] }}</span>
                        <el-button v-if="isSlefAuth" type="text"  @click="reset('text')">重置文本</el-button>
                    </div>
                    <div class="edit-content-box">
                        <editor v-if="isSlefAuth" v-model="getHtml" ref="text" class="summary-review-box" @change="onChange" :editStatus="editStatus[notesKey]"
                            notLoadHtml :notMenus="['image', 'video', 'emoticon', 'link','fontName', 'table']"></editor>
                        <div v-else class="h100 overflow_hidden overflow_auto_y">
                            <div v-html="getHtml"></div>
                        </div>
                    </div>
                </template>
                <template #aiSharding>
                    <div class="p-r h100 w100">
                        <div v-if="!editInfoMap['aiSharding'] && getiShardingStatus !== 2 && isSlefAuth" class="p-a w100 h100 flex-ji-c" style="z-index: 1900;background: rgba(0,0,0,0.2);left: 0;top: 0;">
                            <div v-if="$isWeb" class="font-s12 text-colorc3">
                                请前往客户端获取AI脚本拆解后，即可编辑笔记...
                            </div>
                            <template v-else>
                                <span v-if="!selfUId">
                                    没有获取权限...
                                </span>
                                <afp-button v-else :loading="getiShardingStatus === 1" type="primary" :plain="false" size="medium" @click="getAiSharding">{{ getiShardingStatus === 1 ? '获取AI脚本拆解中...' : '获取AI脚本拆解' }}</afp-button>
                            </template>
                        </div>
                        <template v-else>
                            <div class="edit-time font-s14 pd-b12 flex-jc-sb">
                                <span>最近编辑时间: {{ editInfoMap['aiSharding'] }}</span>
                                <el-button v-if="isSlefAuth" type="text"  @click="reset('aiSharding')">重置文本</el-button>
                            </div>
                            <div class="edit-content-box">
                                <editor v-if="isSlefAuth" v-model="getHtml" class="summary-review-box" ref="aiSharding" @change="onChange" :editStatus="editStatus[notesKey]"
                                notLoadHtml :notMenus="['image', 'video', 'emoticon', 'link', 'fontName', 'table']"></editor>
                                <div v-else class="h100 overflow_hidden overflow_auto_y">
                                    <div v-html="getHtml"></div>
                                </div>
                            </div>
                        </template>
                    </div>
                </template>
                <template #notes99>
                    <div class="p-r h100 w100">
                        <div class="edit-time font-s14 pd-b12 flex-jc-sb">
                            <span>最近编辑时间: {{ editInfoMap['notes99'] }}</span>
                            <el-button v-if="isSlefAuth" type="text" @click="synchronous">重置数据</el-button>
                        </div>
                        <div class="edit-content-box">
                            <editor v-if="isSlefAuth" v-model="getHtml" class="summary-review-box" ref="notes99"
                            :editStatus="editStatus[notesKey]" @change="onChange" notLoadHtml
                            :notMenus="['image', 'video', 'emoticon', 'link','fontName', 'table']"></editor>
                            <div v-else class="h100 overflow_hidden overflow_auto_y">
                                <div v-html="getHtml"></div>
                            </div>
                        </div>
                    </div>
                </template>
                <template v-for="(item, index) in addTabs" :slot="item.name">
                    <slot :name="item.name"></slot>
                </template>
            </tabs>
            <div class="flex-jc-sb pd-6" style="height: 40px;">
                <afp-button @click="quitNotes">退出笔记</afp-button>
                <afp-button v-if="isNotes" v-auth="selfIds" :plain="false" :disabled="!isEditStatus" type="primary"
                    @click="saveNotesHandler">保存笔记</afp-button>
            </div>
        </div>
        <exportDialog ref="exportDialog"></exportDialog>
    </div>
</template>

<script>
import Tabs from './../Tabs';
import editor from './../editor/index.vue';
import textChannel from './textChannel';
import exportDialog from './export.vue';
import dataBoardMixins from '/src/components/analysis/dataBoard/text/common.js';
import { getSourceData } from '@/utils/common';
import renderAiResponseContent, { AI_RENDER_MODE_MAP } from '@/components/analysis/ai/common/aiContentRenderParser';
import auth from '@/mixins/auth';
export default {
    components: {
        Tabs,
        editor,
        exportDialog
    },
    props: {
        sentenceMarkData: {
            type: Object,
            default: () => { return {} }
        },

        addTabs: {
            type: Array,
            default: () => {
                return []
            }
        },
        notesData: {
            type: Object,
            default: () => { return {} }
        }
    },
    mixins: [dataBoardMixins,auth],
    data() {
        return {
            // AI脚本拆解状态
            aiSharding: 0,
            // text: 正常文本  aiSharding: ai文本分段  aiOptimal: ai优化内容
            notesKey: 'text',
            tabsList: [
                { label: '分钟段落笔记', name: 'text', dataIndex: 0, notesType: 3 },
                { label: 'AI脚本拆解笔记', name: 'aiSharding', dataIndex: 1, notesType: 1, hidden: () => true },
                { label: '复盘小结', name: 'notes99', dataIndex: 2, notesType: 2 },
            ],
            htmlData: {},
            editStatus: {},
            editUserInfo: {},
            editInfoMap: {}
        };
    },
    computed: {
        isNotes() {
            return this.tabsList.some(d => {
                return d.name === this.notesKey
            })
        },
        getTabs() {
            return [
                ...this.tabsList,
                ...this.addTabs
            ]
        },
        isSelectTabsName() {
            return this.tabsList?.some(d => d.name === this.notesKey)
        },
        getHtml: {
            get() {
                return this.htmlData[this.notesKey]
            },
            set(val) {
                this.htmlData[this.notesKey] = val
            },
        },
        getEditItem() {
            let keys = Object.keys(this.editStatus);
            keys = keys?.filter(d => !!d)?.map(k => {
                return this.getTabs?.find(t => t.name === k)
            });
            this.$emit('notesEditer', keys?.length);
            return keys
        },
        getEditStatusLabel() {
            return this.getEditItem?.map(d => d?.label);
        },
        isEditStatus() {
            return this.getEditStatusLabel?.length > 0;
        },
        getiShardingStatus(){
            return this.aiSharding ?? 0;
        }
    },
    watch: {},
    methods: {
        reset(type){
            this.$confirm('重置后原有笔记将会丢失,确定重置文本内容吗？',{
                type: 'warning'
            }).then(()=>{
                if(type === 'text'){
                    this.setNotesText(type, this.channelText(),{reset: true});
                }else if(type === 'aiSharding'){
                this.setNotesText(type, this.notesData[type]?.list, {
                    ...this.notesData[type]?.opt,
                    reset: true
                    });
                }
            })
        },
        async synchronous(){
            const html = this.getHtml;
            const { sourceId, sourceType } = getSourceData(this.sentenceMarkData);
            const resHtml = await this.$httpBack.notes.lastReview({
                sourceId, sourceType
            }).then(res=>{
                return res?.data || '';
            }).catch(err=>{
                return '';
            });
            if(!resHtml){
                return;
            }
            let newHtml ='';
            if (html.includes("<div class='video-data-screenshot'") || html.includes('<div class="video-data-screenshot">')) {
                let updatedHtml = html.replace(/<div class=["'']video-data-screenshot["'][^>]*>[\s\S]*?<\/div>/g, '&&##');
                newHtml = updatedHtml.replace(/(&&##){1,}/, resHtml).replace(/(&&##){1,}/g, '');
            } else {
                newHtml = resHtml + html;
            }
            this.setNotesText('notes99', newHtml, {
                reset: true
            });
        },
        getEditTime(type){
            let list = this.editUserInfo[type];
            if(!list?.length){
                return '-';
            }
            let pop = list[list?.length - 1];
            this.$set(this.editInfoMap,type,pop?.editTime?.replace('T', ' ')?.split('.')[0]);
        },
        exportNotes() {
            this.$nextTick(() => {
                const { videoInfo, uploadFile, anchorInfo } = this.sentenceMarkData;
                let name = (videoInfo?.VideoName || uploadFile?.fileName)?.split('.ts')[0]?.split('_');
                let sliceName = (videoInfo?.VideoName || uploadFile?.fileName)?.replace(/\.(ts|mp4|txt)$/i, '')
                name.pop();
                name = name.join('_') || sliceName;
                this.$refs.exportDialog.show({
                    data: {
                        name,
                        typeName: `-${this.getTabs?.find(d => d.name === this.notesKey)?.label}`,
                        anchorName: anchorInfo?.AnchorName,
                        startTime: videoInfo?.StartTime,
                        endTime: videoInfo?.EndTime,
                        duration: videoInfo?.Duration,
                        html: this.getHtml
                    }
                })
            })
        },
        ifGetAiSharding(){
            if(typeof this.getiShardingStatus === 'undefined'){
                this.getAiSharding();
            }
        },
        getAiSharding(){
            if(!this.selfUId){
                return;
            }
            this.$emit('getText', {type: 'aiSharding', status: this.getiShardingStatus || 0, event: 'click'});
        },
        // 设置笔记文本数据
        setNotesText(type, listOrText, opt={}) {
            const { status, setText, reset } = opt;
            if(typeof status !=='undefined'){
                let s = Number(status) ?? 1;
                this.$set(this, type, s);
            }
            if(!reset){
                if(this.editInfoMap[type]?.length && setText){return}
                if (this.htmlData[type]) { return }
            }
            let html = '';
            if (Array.isArray(listOrText)) {
                html = listOrText.filter(d => !!d).join('');
            } else if (typeof listOrText === 'string') {
                html = listOrText;
            }
            this.setHtml(type, html);
        },
        // 设置html数据
        setHtml(type, html) {
            if (!type) { return }
            this.$set(this.htmlData, type, html || '');
            this.setEditHtml(type, html);
        },
        // 设置富文本html内容
        setEditHtml(type, html) {
            this.$nextTick(() => {
                this.$refs?.[type]?.setEditrHtml(html);
            })
        },
        tabsClick(name) {
            this.$emit('tabsClick', name);
            // if(name === 'aiSharding'){
            //     this.ifGetAiSharding();
            // }
            // if (!this.htmlData[name] && name === 'aiSharding' && this.getiShardingStatus !== 2) {
            //     this.$emit('getText', name)
            //     return;
            // }
            this.$nextTick(() => {
                this.setEditHtml(name, this.getHtml);
            })
        },
        async getInfoByVideo() {
            let vData = '';
            const { videoInfo } = this.sentenceMarkData;
            let resData = await this.$httpBack.v2400.infoByVideoId({
                videoId: videoInfo?.VideoId
            });
            let assemblyData = videoInfo?.VideoId ? this.assemblyData(resData?.data || {}) : '';
            Object.values(assemblyData?.data)?.forEach((item, index) => {
                if (index === 0) {
                    vData += `<div>${assemblyData?.labels?.[index]?.label}</div>`;
                    Object.values(item)?.forEach(sItem => {
                        vData += `<div style="padding-left: 10px">##${sItem?.title}</div>`;
                        sItem.childData?.forEach(ssItem => {
                            vData += `<div style="padding-left: 20px">${ssItem?.label}：${ssItem.value}</div>`;
                        })
                    })
                }
            });
            return vData;
        },

        async getNoetsData(type,notesType){
            const { sourceId, sourceType } = getSourceData(this.sentenceMarkData);
            return await this.$httpBack.notes.getNotes({
                sourceId, sourceType: sourceType,
                notesType
            }).then(res => {
                this.editUserInfo[type] = res?.data?.editors || {};
                this.getEditTime(type);
                return res?.data?.content || '';
            }).catch(()=>{
                return '';
            });
        },

        // 初始化获取笔记内容
        /*
        notesType 笔记类型，支持以下取值：
        1: 原文笔记
        2: 复盘小结
        3: 分段笔记
        */
        async initGetNotesText(type) {
            const notesType = this.getTabs?.find(d => d.name === type)?.notesType;
            let o = {
                'text': '',
                'aiSharding': '',
                "notes99": ''
            };
            o[type] =await this.getNoetsData(type,notesType);
            if(type === 'notes99' && o[type]){
                // if(!o[type]?.includes('class="notes99"')){
                //     o[type] = '<div class="notes99">'+o[type]+'</div>';
                // }
                o[type] = o[type] + '<div><br/></div>';
                o[type] = o[type]?.replace(/(<div><br\/?><\/div>\s*)+/g, '<div><br/></div>');
            }
            // if (type === 'notes99' && !o[type]) {
            //     o[type] = await this.getInfoByVideo();
            // }
            if(type === 'aiSharding' && !o[type] && !this.editInfoMap['aiSharding']){
                const { sourceId, sourceType } = getSourceData(this.sentenceMarkData);
                this.$httpBack.words.getVideoContent({
                    sourceId, sourceType,
                    type: 1
                }).then(res=>{
                    const { contentStatus, videoFileContentList } = res?.data;
                    this.$set(this, 'aiSharding', contentStatus);
                    const contentList =[].concat(...(videoFileContentList?.map(item=>{
                        return item.contentList
                    }) || []));
                    const contentMd = contentList.filter(t => !!t).join('\n')
                    const contentHtml = renderAiResponseContent(contentMd || '', {
                        parserMode: AI_RENDER_MODE_MAP.MD_TAG,
                        forceStyle: true,
                        upgradePlainTable: true
                    }).html
                    this.setNotesText(type, contentHtml);
                    this.$emit('setTextData',{
                        type: type,
                        list: contentList,
                        status: contentStatus,
                    })
                });
            }
            this.setNotesText(type, o[type]);
            return this.editUserInfo[type]?.length
        },
        saveNotesHandler() {
            this.saveNotes();
        },
        saveNotes(opt = {}) {
            const { callback, title, next } = opt;
            if (!this.isEditStatus) {
                callback && callback(false);
                return Promise.reject();
            };
            document.body.classList.add('save-body-box');
            callback && callback(this.isEditStatus);
            return this.$msgbox({
                title: title || `保存笔记`,
                message: `
                    <div><span class="text-colorErr">${this.getEditStatusLabel?.join('、')}</span>有修改情况！是否保存笔记？</div>
                `,
                customClass: 'notes-save-dialog',
                showCancelButton: true,
                dangerouslyUseHTMLString: true,
                confirmButtonText: callback?'保存并退出':'保存',
                cancelButtonText: callback?'直接退出':'取消',
                beforeClose: (action, instance, done) => {
                    if (action === 'confirm') {
                        const { sourceId, sourceType } = getSourceData(this.sentenceMarkData);
                        instance.confirmButtonLoading = true;
                        instance.confirmButtonText = '保存中...';
                        this.getEditItem?.forEach(async (d) => {
                            await this.$httpBack.notes.saveNotes({
                                sourceId,
                                sourceType: sourceType,
                                notesType: d?.notesType,
                                content: this.htmlData[d?.name] || ''
                            },{notFormat: true})
                        })
                        done();
                        instance.confirmButtonLoading = false;
                    } else {
                        done();
                    }
                }
            }).then(action => {
                this.getEditItem?.forEach(d=>{
                    this.getNoetsData(d?.name,d?.notesType);
                })
                this.$set(this,'editStatus',{})
                this.$message.success('笔记保存成功');
            }).catch(()=>{
                this.$set(this,'editStatus',{})
            }).finally(() => {
                document.body.classList.remove('save-body-box');
                setTimeout(()=>{
                    next && next();
                },100)
            });

        },
        isNotesEditer(callback,next){
            this.saveNotes({callback, next});
        },
        // 监听tabs改变
        onChange() {
            this.$set(this.editStatus, this.notesKey, true);
        },
        // 退出笔记
        quitNotes() {
            if (this.isEditStatus) {
                this.saveNotes({ title: '确定退出笔记？' }).then(() => {
                    this.quitInit();
                }).catch((err) => {
                    this.$message.error('取消笔记保存！')
                    this.quitInit();
                })
            } else {
                this.quitInit();
            }
        },
        quitInit() {
            this.notesKey = this.tabsList[0]?.name;
            this.htmlData = {};
            this.$emit('quit');
        },
        // 清洗分钟段落数据
        channelText() {
            let o = textChannel(this.sentenceMarkData, {
                render: (item, html) => {
                    if(item.startHm){
                        return [`<p class="text-color3">${item.startHm}</p>`, html].join('')
                    }else{
                        return html;
                    }
                }
            });
            return o?.html;
        },
        // 初始化编辑器 加载数据
        initEditorData() {
            this.$nextTick(() => {
                this.tabsList?.forEach(async (d) => {
                    if (d.dataIndex === false) { return }
                    let key = d.name;
                    // 初始化获取，通过接口检查是否有笔记内容存在，如果有不执行初始化逻辑
                    if (await this.initGetNotesText(key)) {
                        return;
                    }
                    // 设置笔记内容，通过notesData获取笔记原始数据。
                    if (this.notesData[key]) {
                        this.setNotesText(key, this.notesData[key]?.list, this.notesData[key]?.opt);
                    } else if (key === 'text') {
                        this.setNotesText(key, this.channelText());
                    }
                })

            })
        }
    },
    created() {
        this.initEditorData()
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
</script>

<style lang="scss">
.save-body-box {
    .el-message-box__wrapper {
        z-index: 60000 !important;
    }

    .v-modal {
        z-index: 59999 !important;
    }
}
</style>

<style lang='scss' scoped>
.notes-tabs-box {
    ::v-deep(.el-tabs__item) {
        padding: 0 10px;
    }
    ::v-deep(.el-tabs) {
        height: 100%;
        display: flex;
        flex-direction: column;
        .el-tabs__content {
            height: 100%;
            flex: 1;
            .el-tab-pane {
                height: 100%;
                >div {
                    height: 100%;
                }
            }
        }
    }
}

.summary-review-box {
    ::v-deep(.editor-box) {
        border: 1px dashed var(--color-main) !important;
        // border-color: var(--color-main) !important;
        
        // overflow: hidden;
        .w-e-text-container {
            border-bottom: 1px dashed var(--color-main) !important;
            border-radius: 0 0 4px 4px !important;
        }
        .w-e-toolbar,
        .w-e-text-container {
            // border-color: var(--color-main) !important;
            background: #F2F8FF !important;
            border: none !important;
        }
        .w-e-toolbar{
            border-radius:4px 4px 0 0 !important;
            border-bottom: 1px dashed var(--color-main) !important;
        }

    }
}

.notes-save-dialog {}

.edit-time{
    height: 36px;
    line-height: 20px;
}
.edit-content-box{
    height: calc(100% - 36px);
}

</style>
