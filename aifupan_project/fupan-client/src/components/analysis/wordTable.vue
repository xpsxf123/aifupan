<template>
    <div class="wordsSummaryContainer pd-b6">
        <div class="word-tag-box" ref="wordTagBox">
            <div v-if="btns.l" class="word-move word-move-left">
                <afp-button type=""  @click="move(0)">
                    <svg class="icon" aria-hidden="true">
                        <use xlink:href="#icon-a-Frame567"></use>
                    </svg>
                </afp-button>
            </div>
            <!-- {{ getScroll() }} -->
            <div class="word-tag-content" ref="wordTag">
                <el-radio-group v-model="radio"  @change="tagsClick">
                    <el-radio v-for="(item, index) in newGetTags" :id="item?.id" :label="item?.key" border>
                        <slot name="word-tag" :item="item">
                            {{ item?.label }}:{{ item?.countNum0 || item?.countNum }}
                            <span class="word-tag-rate" v-if="item?.type !== 'def'" :key="item.id"
                                v-html="rate(item)"></span>
                        </slot>
                    </el-radio>
                </el-radio-group>
            </div>
            <div v-if="btns.r" class="word-move word-move-rigth">
                <afp-button type=""  @click="move(1)">
                    <svg class="icon" aria-hidden="true">
                        <use xlink:href="#icon-a-Frame566"></use>
                    </svg>
                </afp-button>
            </div>
        </div>
        <el-table class="word-table" size="mini" :data="getPageList" max-height="300" :highlight-current-row="false" border
            style="width: 100%; margin-top: 6px;"  :header-cell-style="{ 'background': 'rgb(245,247,249)' }"
            :row-class-name="rowClassName">
            <el-table-column prop="wordsTypeStr" label="类型" align="center" width="110">
                <template slot-scope="{row}">
                    <span v-if="row.wordsType === 0">智能敏感词</span>
                    <span v-if="row.wordsType === 1">运营关键词</span>
                </template>
            </el-table-column>
            <el-table-column prop="typeStr" label="分类" align="center" width="130"></el-table-column>
            <el-table-column prop="name" label="词语" align="center" width="150">
                <template slot-scope="{row}">
                    <span @click="clickWord(row.name)"
                        :style="{ color: row.wordsType ? '#2C61A7' : '#B24343', cursor: isClickWord ? 'pointer' : 'initial' }">
                        {{ row.name }}
                    </span>
                </template>
            </el-table-column>
            <slot name="table-column"></slot>
            <el-table-column prop="totalNum" v-if="!isContrast" label="全文次数" align="center" width="80">
                <template slot-scope="{row}">
                    <span class="text-color3">{{ row.totalNum }}</span>
                </template>
            </el-table-column>
            <slot name="table-view"></slot>
            <el-table-column prop="resourceTypeStr" label="来源" align="center" width="80">
            </el-table-column>
            <slot name="table-scene">
                <el-table-column prop="remarks" label="场景描述" align="center" min-width="150">
                    <template slot-scope="scope">
                        <el-popover placement="top-start" width="300" trigger="hover" :close-delay="30"
                            popper-class="myPopover" :content="scope.row.remarks">
                            <div slot="reference"
                                style="width: 100%;overflow: hidden;white-space: nowrap;text-overflow: ellipsis;">
                                <span>{{ scope.row.remarks }}</span>
                            </div>
                        </el-popover>
                    </template>
                </el-table-column>
            </slot>

            <el-table-column prop="tradeStr" label="行业" align="center" width="200">
            </el-table-column>

        </el-table>
        <!-- 分页 -->
        <div class="flex-jc-sb flex-ai-c mg-t6" v-if="pageSize">
            <slot name="page-left">&nbsp;</slot>
            <el-pagination style="text-align: center; " @current-change="currentChangeHandle" :current-page="pageIndex"
                :page-sizes="pages" :page-size="pageSize" :total="getTotalCount" @size-change="sizeChange"
                layout="total, sizes, prev, pager, next, jumper">
            </el-pagination>
            <slot name="page-right">&nbsp;</slot>
        </div>
    </div>
</template>

<script>
import resize from './../../mixins/resize';
import myUtils from '../../utils/utils';
export default {
    name: "",
    mixins: [resize],

    props: {
        tableList: {
            type: Array,
            default: () => {
                return []
            }
        },
        tabsList: {
            type: Array,
            default: () => {
                return []
            }
        },
        column: {
            type: Array,
            default: () => { return [] }
        },
        filterData: {
            type: Function,
            default: null
        },
        closeParagraph: {
            type: Function,
            default: () => { }
        },
        callbackTags: {
            type: Function,
            default: null
        },
        isNewTag: {
            type: Boolean,
            default: true
        },
        cruxTypeList: {
            type: Array,
            default: () => {
                return []
            }
        },
        isContrast: {
            type: Boolean,
            default: false
        },
        sentenceMarkData: {
            type: Object,
            default: () => {
                return {}
            }
        },
        wordsInfo: {
            type: Object,
            default: () => {
                return {}
            }
        }
    },
    computed: {
        // 获取表格所有数据
        getTableListAll() {
            const map = new Map();
            // 通过数组合并解决map返回形成的三维数组，在通过map的唯一性判断是否重复通过fliter筛选。去除重复数据得到不重复的敏感词和关键词
            let list = (this.tableList?.some(d => !!d?.wordsList?.length) ? [].concat(...this.tableList?.map(d => d.wordsList)) : this.tableList) || [];
            list = list?.filter(sItem => {
                let only = !map.has(sItem.name + sItem.wordsType) && map.set(sItem.name + sItem.wordsType, true);
                if (typeof this.filterData === 'function') {
                    let bl = this.filterData(sItem, only);
                    return only && bl;
                }
                return only
            }).map(d => {
                // 截取分类
                let nameArr = d?.cruxTypeInfo?.nameArr || [];
                // 设置类型标题
                d.typeStr = d.wordsType === 1 ? (nameArr.slice(1, 3) || []).join('-') : d.typeStr;
                return d;
            })
            return list;
        },
        //新版获取tag
        newGetTags() {
            // const {markSensitive, markCrux } = this.wordsInfo;
            let tags = this.tabsList?.filter(d => !!(d.countNum || d.num))?.map(d => {
                let countNum = d.countNum || d.num;
                if (d.tabType === 0) {
                    return {
                        ...d,
                        key: 'all',
                        type: 'def',
                        tabSort: 0.1,
                        label: d.tabName,
                        countNum
                    }
                } else if (d.tabType === 1 || d.tabType === 2) {
                    if(d.tabType === 1 && !this.wordsInfo.markSensitive){return false}
                    return {
                        ...d,
                        label: d.tabName,
                        countNum,
                        tabSort: d.tabType / 10,
                        key: `wordsType_${d.tabType - 1}`,
                        type: 'def',
                        id: d.tabType === 2 ? 'wordsType1-dom' : ''
                    }
                } else {
                    return {
                        ...d,
                        label: d.tabName,
                        key: d.cruxTypeId,
                        countNum
                    }
                }
            }).filter(d=>d);
            this.$nextTick(() => {
                this.initMoveBtn();
            })
            return tags;
        },
        // 获取分类按钮----老版本逻辑
        getTags() {
            if (this.isNewTag) { return this.newGetTags }
            let o = {}, countNum = 0;
            this.getTableListAll?.forEach(item => {
                if (!item) { return }
                // 统计关键词/敏感词
                let wKey = `wordsType_${item?.wordsType}`
                if (o[wKey] === undefined) {
                    o[wKey] = { key: wKey, sort: item.wordsType + 10, label: item.wordsTypeStr, countNum: 0 }
                }
                if (this.callbackTags) {
                    this.callbackTags(o[wKey], item)
                } else {
                    o[wKey].countNum += item.count;
                }
                // 关键词分类统计
                if (item.wordsType === 1) {
                    // 关键词分类
                    let key = `type_1_${item.type}`;
                    if (o[key] === undefined) {
                        o[key] = { key, type: 'type', sort: (100 + item.typeSort), label: item.typeStr, countNum: 0 }
                    }
                    if (this.callbackTags) {
                        this.callbackTags(o[key], item);
                    } else {
                        o[key].countNum += item.count;
                    }
                }
                // 全部统计
                if (item.count !== undefined) {
                    countNum += item.count;
                } else {
                    countNum += item.countNum;
                }
            });

            let arys = Object.values(o)?.sort((a, b) => {
                // sort 转换过后的数据,敏感词+10 ,关键词+100 用于升序排序,全部按钮为0.
                return a.sort - b.sort;
            })
            arys.unshift({ key: 'all', value: 0, sort: 0, label: '全部', countNum });
            this.$nextTick(() => {
                this.initMoveBtn();
            })
            return arys;
        },
        // 根据按钮过滤数据
        getTables() {
            return this.getTableListAll?.sort(myUtils.wordsSort)?.filter(d => {
                if (!d) { return false }
                if (this.radio !== 'all') {
                    return [`wordsType_${d.wordsType}`, ...(d?.cruxTypeInfo?.idArr || [])].includes(this.radio);
                } else {
                    return true
                }
            }) || [];
        },
        // 前端分页
        getPageList() {
            // 数据分页判断页码。如果第一页则设置为1.否则出错。页码从第一页开始算。否则从0开始要出问题
            let index = this.pageIndex;
            if (index <= 0) {
                index = 1;
            }
            // 数据分页
            let list = this.getTables.slice((index - 1) * this.pageSize, ((index - 1) * this.pageSize) + this.pageSize);
            this.$emit('table-len', list.length)
            return list;
        },
        // 获取总数
        getTotalCount() {
            return this.getTables?.length || 0
        },
        // 获取类型为关键字的key
        getWordsType1() {
            return this.getTags.find(d => d.key === `wordsType_1`);
        },
        isClickWord() {
            return !!this.$listeners?.clickWord
        },
    },
    data() {
        return {
            radio: 'all',
            pageIndex: 0,
            pageSize: undefined,
            btns: {
                sW: 0,
                l: false,
                r: false
            },
            pages: []
        };
    },
    watch: {
        pages: {
            handler(val) {
                this.pageSize = val[0];
            },
            deep: true,
            immediate: true
        },
        getTotalCount: {
            handler(v) {
                this.$emit('total', v)
            }
        },
        tableList(val, oldValue) {
            if (val.length !== oldValue.length) {
                this.tagsClick();
            }
        }
    },
    mounted() {
        this.$nextTick(() => {
            this.addResizeFns('discernSearchContainerContent', () => {
                this.getTableHeight()
            }, 10)
        })
    },
    created() {

    },
    methods: {
        rowClassName({ row }) {
            return (row.level === 0 && row.wordsType === 0) ? 'level0-color' : ''
        },
        getScroll() {
            let boxW = this.$refs?.wordTagBox?.clientWidth;
            let contW = this.$refs?.wordTag?.scrollWidth + 10;

            return {
                boxW, contW
            }
        },
        initMoveBtn() {
            this.$nextTick(() => {
                const { boxW, contW } = this.getScroll();
                if (!boxW && !contW) { return }
                if (boxW >= contW) {
                    this.btns.l = false;
                    this.btns.r = false
                    return
                }
                let sW = contW - boxW;

                this.btns.sW = sW;
                // this.btns.l = this.$refs.wordTag.offsetLeft>15;
                this.isBtnShow(this.$refs.wordTag.scrollLeft)
            })
        },
        // 按钮显示范围
        isBtnShow(left) {
            this.btns.l = left > 1;
            this.btns.r = left < this.btns.sW;
        },
        move(type) {
            // type: 0-左 1-右
            if (type) {
                let l = this.$refs.wordTag.scrollLeft + 50;
                this.$refs.wordTag.scrollTo({
                    left: l
                })
            } else {
                let l = this.$refs.wordTag.scrollLeft - 50;
                this.$refs.wordTag.scrollTo({
                    left: l > 0 ? l : 0
                })
            }
            // 重修判断按钮显隐
            this.initMoveBtn();
        },
        rate(item) {
            if (this.isNewTag) {
                let scale = !!item?.isCount ? parseFloat(((item?.scale0 || item?.scale) * 100).toFixed(1)) : -1;
                return scale < 0 ? `<span class="text-colorErr">非通用</span>` : `${scale}%`;
            } else {
                return (parseFloat(((item.countNum / this.getWordsType1.countNum) * 100))).toFixed(1) + '%'
            }
        },
        tagsClick() {
            this.currentChangeHandle(1)
        },
        sizeChange(size) {
            this.pageSize = size;
            this.pageIndex = 1;
            this.$emit('size-change', size)
            this.closeParagraph && this.closeParagraph()
        },
        // 切换分页回调
        currentChangeHandle(val) {
            this.pageIndex = val;
            this.closeParagraph && this.closeParagraph()
        },

        clickWord(word) {
            this.$emit('clickWord', word)
        },
        getTableHeight() {

            this.$nextTick(() => {
                let height = document.body.clientHeight;
                let pages = [];
                if (height < 350) {
                    pages = [1, 2, 3, 4]
                } else if (height < 700) {
                    pages = [2, 4, 6, 8]
                } else {
                    pages = [4, 8, 10]
                }
                this.$emit('getPages', pages);
                this.$set(this, 'pages', pages);
                this.pages = pages;
            })
        },
    }

};
</script>

<style scoped lang="less">
.wordsSummaryContainer {
    font-weight: 600;
    font-size: 14px;
    color: #2E3742;
    // border-top: 0.5px solid #eee;
    // .el-table{
    //     margin-top: 0 !important;
    // }
}

.word-tag-box {
    padding: 5px 12px;
    position: relative;
    overflow: hidden;

    .word-tag-content {
        overflow-x: auto;
        white-space: nowrap;

        &::-webkit-scrollbar {
            width: 0;
            height: 0;
        }
    }

    .word-move {
        position: absolute;
        // width: 30px;
        // height: 30px;
        top: 4px;
        // background: #95A1AF;
        z-index: 88;
        cursor: pointer;

        .el-button {
            border: none;
            background: #E8F5FF;
            padding: 7px 9px;
        }
    }

    .word-move-left {
        left: 0;
    }

    .word-move-rigth {
        right: 0;
    }
}


/deep/ .el-dialog__body {
    padding: 0;
}

/deep/ .el-dialog__header {
    display: none;
}

/deep/ .el-table--mini .el-table__cell {
    padding: 0;
}

/deep/ .el-table .el-table__cell {
    padding: 0;
}

.word-tag-box {
    ::v-deep(.el-radio-group) {
        margin: 0 -10px;

        .el-radio {
            background: #fff;
            border-radius: 0;
            margin: 0 10px;
            text-align: center;
            padding: 0 10px;
            // width: 72px;
            height: 24px;
            line-height: 24px;
            border: none;
        }

        .el-radio:last-child {
            margin-right: 0;
        }

        .is-checked {
            background: rgba(255, 197, 93, 0.5);
            font-weight: 400;
            font-size: 12px;
            color: #151719;

            .el-radio__label {
                color: #151719;
            }
        }

        .el-radio__label {
            padding: 0;
        }

        .el-radio__input {
            display: none;
        }
    }

}

.word-tag-rate {
    padding-left: 4px;
    border-left: 1px solid #95A1AF;
}

.word-table {
    ::v-deep(.level0-color) {
        background: #ECE4FC !important;
    }

    ::v-deep(.level0-color:hover) {
        background-color: #ECE4FC !important;
    }
}
</style>