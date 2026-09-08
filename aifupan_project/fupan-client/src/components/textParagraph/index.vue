<template>
    <div ref="textParagraphBox" class="text-paragraph-box" :class="{ 'is-text': getIsText }" @click="getIsText ? null : handleContainerClick($event)">
        <template v-if="getIsText">
            <!-- 文本模式直接展示标记过后的字段，非源文本则隐藏字体颜色，展示标记位置和源文本重叠形成标记，主要用于视频播放锁定文字位置。 -->
            <div class="content-text no-select" aria-hidden="true" :style="getIsTypeText ? {} : {} || { color: 'transparent' }"
                v-html="contentHtml"></div>
            <!-- 非文本模式当前字段源文件展示 -->
            <div class="text-bg content-box not-word" :class="`textParagraph-${paragraphIndex}`" v-html="getContent"
                @click="handleContainerClick($event)"></div>
        </template>
        <template v-else>
            <template v-for="(item, index) in textList">
                <!-- 标点符号渲染 -->
                <span v-if="['，', ',', '！', '!', '?', '。', '.'].includes(item.char)" class="mark text_item"
                    :class="getOtherMarkClass(item)"><Mark :mark="aMark"  :opt="MarkOpt" :item="item"></Mark>{{ item.char }}</span>
                <span v-else :key="index" class="text_item" :class="getMarkClass(item)" :style="getMarkStyle(item)"
                    @click="clickWord(item)">
                    <template v-if="item.children?.length > 0">
                        <template v-for="(cItem, cIndex) in item.children">
                            <span class="mark text_item" :class="getOtherMarkClass(item)"
                                v-if="['，', ',', '！', '!', '?', '。', '.'].includes(cItem.char)"><Mark :mark="aMark"  :opt="MarkOpt" :item="cItem"></Mark>{{ cItem.char }}</span>
                            <span class="text_item" :key="cIndex" :style="getMarkStyle(cItem)" v-else
                                :class="getMarkClass(cItem, 'sub')"><Mark :mark="aMark"  :opt="MarkOpt" :item="cItem"></Mark>{{ cItem?.char }}</span>
                        </template>
                    </template>
                    <!-- 文字渲染 -->
                    <template v-else><Mark :mark="aMark" :opt="MarkOpt" :item="item"></Mark>{{ item?.char }}</template>
                </span>
            </template>
        </template>
        
    </div>
</template>

<script>
import myUtils from '/src/utils/utils';
import Mark from './annotationMark.vue';
export default {
    name: "",
    components:{
        Mark
    },
    props: {
        sentenceMark: {
            type: Object,
            default: () => { }
        },
        markColor: {
            type: Object,
            default: () => { }
        },
        currentTime: {
            type: [Number, String],
            default: ''
        },
        selectMark: {
            type: Object,
            default: () => { }
        },
        notWordsType: {
            type: [Array, Object],
            default: () => {
                return {};
            }
        },
        // 是否是文本字段渲染模式，音频下的文本模式需要加图层，纯文本不需要
        isText: {
            type: Boolean,
            default: false
        },
        paragraphIndex: {
            type: Number,
            default: 0
        },
        keyWord: {
            type: String,
            default: ''
        },
        currentParagraphIndex: {
            type: Number,
            default: -1
        },
        oldSelectData: {
            type: Object,
            default: () => { }
        },
        // 文本类型是否是纯文本
        textType: {
            type: String,
            default: ''
        },
        name: {
            type: String,
            default: ''
        },
        textShow: {
            type: Boolean,
            default: false
        },
        annotation: {
            type: Array,
            default: () => {
                return []
            }
        }
    },
    computed: {
        getItems() {
            return this.sentenceMark.items;
        },
        getWordsList() {
            return this.sentenceMark.wordsList;
        },
        getContent() {
            if(this.contentText){
                return this.contentText;
            }
            return this.sentenceMark.content;
        },
        getRenderHtml() {
            return this.contentHtmlList.join('');
        },
        getIsText() {
            return this.isText || this.textShow;
        },
        getIsTypeText() {
            return this.textType === 'text' || this.textShow
        },
        getCharClass() {
            return `${this.name}char_`
        },
        aMark(){
            return this.annotationStartIndexMap
        },
        isDevMode(){
            return this.$store.getters.getMode
        },
        MarkOpt(){
            return {
                paragraphIndex: this.paragraphIndex
            }
        }
    },
    data() {
        return {
            textList: [],
            itemsMap: {},
            originalMap: {},
            wordListMap: {},
            keywordMap: {},
            itemsText: '',
            textIndex: 0,
            lastTime: 0,
            wordColor: ['pink', 'paleturquoise'],
            keyWordColor: 'darkorange',
            selectWordColor: 'limegreen',
            readColor: '#336df4',
            bindWordColor: '#e6a23c',
            keyWordsCharIndex: [],
            rangeWordMap: {},
            myDebounce: myUtils.debounce(50),
            annotationHtmlDebounce: myUtils.debounce(200),
            contentHtmlList: [],
            contentHtml: '',
            contentTextList: [],
            contentText: '',
            annotationStartIndexMap: {}
            
            // pink
        };
    },
    watch: {
        annotation: {
            handler() {
                this.syncAnnotationStartIndexMap()
            },
            deep: true,
            immediate: true
        },
        'keyWord': {
            handler(val) {
                if (val) {
                    this.$nextTick(() => {
                        this.setKeyWordMark(val);
                        this.getContentHtml();
                    })
                } else {
                    this.$emit('updateKeyWord', {
                        paragraphIndex: this.paragraphIndex,
                        notData: true
                    })
                    this.getContentHtml();
                }

            },
            immediate: true
        },
        "notWordsType": {
            handler(v) {
                this.getContentHtml();
            },
            deep: true
        },
        "getIsText": {
            handler(val) {
                this.init();
            },
            immediate: true
        },
        selectMark: {
            handler(val) {
                if (this.getIsText && this.paragraphIndex === this.selectMark.paragraphIndex) {
                    this.selectChangeDomStyle(val, this.oldSelectData)
                }
            },
            immediate: true,
            deep: true
        }
    },
    mounted() {
        // this.updateData()
        // document.body.addEventListener('selectchange', (e) => {
        //     console.log(e.target,'-------')
        // });
    },
    activated() {
        this.init();
    },
    created() {
        this.init();
    },
    beforeDestroy() {
        
    },
    methods: {
        init() {
            // 防抖处理
            this.myDebounce(() => {
                if (this.textShow) { return }
                this.initData();
                this.syncAnnotationStartIndexMap()
                this.setItemsObject();
                this.setWordMark();
                this.updateData();
                this.$nextTick(() => {
                    if (this.getIsText) {
                        this.getContentHtml()
                    } else {
                        // 防止多次执行渲染，进行手工操作渲染一次
                        this.getTextDatas()
                    }
                    // 首次渲染
                    if (this.textList.length <= 0) {
                        // 首次做缓存处理
                        this.getTextDatas()
                    }
                })
            })
        },
        syncAnnotationStartIndexMap(){
            const nextMap = {}
            if (Array.isArray(this.annotation) && this.annotation.length) {
                this.annotation.forEach((row) => {
                    const startIndex = row?.[0]
                    const endIndex = row?.[1]
                    const item = row?.[2]
                    if (typeof startIndex === 'undefined' || !item || item.isEnd) return
                    nextMap[startIndex] = {
                        ...item,
                        markStartIndex: typeof item.markStartIndex !== 'undefined' ? item.markStartIndex : startIndex,
                        markEndIndex: typeof item.markEndIndex !== 'undefined' ? item.markEndIndex : endIndex
                    }
                })
            }
            this.$set(this, 'annotationStartIndexMap', nextMap)
        },
        initData() {
            this.originalMap = {};
            this.itemsMap = {};
            this.textIndex = 0;
            this.wordListMap = {};
            this.rangeWordMap = {};
            this.updateData();
            
        },

        // 获取样式
        getMarkStyle(item) {

            // 判断选中是否是当前段落，并且判断选中文字index
            if (this.selectMark.paragraphIndex === this.paragraphIndex && this.selectMark.datas.length && this.selectMark.datas.includes(item.index)) {
                // 判断是否选中样式，选中渲染选中样式
                return { background: this.selectWordColor }
            } else if (item.rangeWord && this.notWordsType[item.rangeWord?.wordsType]) {
                // 判断限定词样式
                if(!this.isDevMode){
                    return null
                }
                return { background: this.bindWordColor }
            } else if (this.keyWord && this.keyWordsCharIndex.includes(item.index)) {
                // 优先渲染搜索样式
                return { background: this.keyWordColor };
                // 渲染敏感词和关键词
            } else if (this.notWordsType[item?.markItem1?.wordsType] || this.notWordsType[item?.markItem0?.wordsType]) {
                // 显示标注颜色(优先渲染敏感词)
                // 非通用关键词不进行颜色渲染。
                let isCount = item?.markItem1?.cruxTypeInfo?.isCount ?? 1;
                if (isCount === 0) { return null }
                if(!this.isDevMode && item?.markItem1?.wordsType === 1){
                    return null
                }
                if(typeof item?.markItem0?.wordsType !== 'undefined'){
                    return { background: this.wordColor[item?.markItem0?.wordsType] }
                }
                return { background: this.wordColor[item?.markItem1?.wordsType] }
            } else {
                //判断是否不显示标注颜色
                return null
            }
        },
        // 选中改变dom样式
        selectChangeDomStyle(data, oldData) {
            let datasEl = data.datas?.length && document.querySelectorAll(data.datas?.map(d => {
                return `.${this.getCharClass}${data.paragraphIndex}_${d}`
            })) || [];
            let oldDatasEl = oldData.datas?.length && document.querySelectorAll(oldData.datas?.map(d => {
                return `.${this.getCharClass}${data.paragraphIndex}_${d}`
            })) || [];

            datasEl.forEach((DOM, index) => {
                DOM.style.background = this.getMarkStyle(this.itemsMap[data.datas[index]])?.background || 'none';
            })
            oldDatasEl.forEach((DOM, index) => {
                DOM.style.background = this.getMarkStyle(this.itemsMap[oldData.datas[index]])?.background || 'none';
            })
        },
        // 获取文本数据
        getTextDatas() {
            let textList = Object.values(this.originalMap).toSorted((a, b) => a.prentIndex - b.prentIndex);
            this.textList = textList;
        },
        // 获取纯文本数据
        getContentHtml() {
            const baseContent = String(this.sentenceMark?.content || '');
            this.contentTextList = [baseContent];
            let contentHtml = baseContent.split('');
            //所有标记字段，敏感词，关键词。
            let allList = [].concat(...Object.values(this.wordListMap).map((item) => {
                return item.indexAll;
            })) || [];
            // 搜索关键字集合
            let keyAllList = this.keywordMap[this.keyWord]?.indexAll || [];
            // 限定词集合
            let rangeWordList = Object.values(this.rangeWordMap) || [];
            // 需要标注的所有字集合列表
            [...allList, ...keyAllList, ...rangeWordList].forEach(item => {
                item.datas.forEach(d => {
                    if (typeof d?.index === 'undefined') { return }
                    contentHtml[d.index] = this.getContentHtmlSpan(d);
                })
            })
            this.contentHtmlList = contentHtml;
            this.setAnnotationHtml();
        },
        addAnnotationMark(item, index, opt={}) {
            const { html, style="" } = opt;
            if(item.isEnd || html?.indexOf('annotation-item-mark')>=0){return ''};
            return `<span class="annotation-item-mark annotation_${index}" style="${style}">批${item.markNo}</span>`
        },
        setAnnotationHtml() {
            let annotationItem = [];
            this.$set(this,'annotationStartIndexMap',{});
            
            if (this.annotation?.length) {
                const contentTextList = String(this.sentenceMark?.content || '').split('');
                this.annotation.forEach(([startIndex, endIndex, item]) => {
                    if (contentTextList[startIndex]?.indexOf('annotation-item') < 0) {
                        this.$set(contentTextList, startIndex, `<span class="annotation-item">${contentTextList[startIndex]}`);
                        this.$set(contentTextList, endIndex, `${contentTextList[endIndex]}</span>`);
                    }
                    annotationItem.push({
                        startIndex,
                        endIndex,
                        data: item
                    });
                    if (!item.isEnd) {
                        this.$set(this.annotationStartIndexMap, startIndex, item)
                    }
                });
                this.$set(this, 'contentTextList', contentTextList)
                this.$set(this, 'contentText', contentTextList.join(''))
            } else {
                this.$set(this, 'contentTextList', [])
                this.$set(this, 'contentText', '')
            }
            this.$set(this, 'contentHtml', this.contentHtmlList.join(''))
            this.setAnnotationMark(annotationItem);
        },
        setAnnotationMark(annotationItem){
            this.$nextTick(()=>{
                setTimeout(()=>{
                    const dom = this.$refs?.textParagraphBox?.getElementsByClassName('content-box')?.[0];
                    if (!dom) return;
                    let annotationItemDom = dom.getElementsByClassName('annotation-item');
                    // let annotationMarkItemDom = this.$refs.textParagraphBox?.getElementsByClassName('annotation-item-mark');
                    // let annotationMarkItemDomLength = annotationMarkItemDom?.length;
                    // if(annotationMarkItemDomLength === annotationItemDom?.length){
                    //     for(let i=0;i<annotationItemDom?.length;i++){
                    //         if(annotationMarkItemDom[i] && annotationItemDom[i]){
                    //             annotationMarkItemDom[i].style.left = `${annotationItemDom[i].offsetLeft - 10}px`;
                    //             annotationMarkItemDom[i].style.top = `${annotationItemDom[i].offsetTop - 12}px`;
                    //         }
                    //     }
                    //     return;
                    // }
                    if (!annotationItem?.length) {
                        return
                    }
                    const baseHtml = this.contentTextList?.length ? this.contentTextList.join('') : String(this.sentenceMark?.content || '');
                    const markHtmlList = []
                    annotationItem.forEach(({data,startIndex, endIndex},index)=>{
                        let el = annotationItemDom?.[index];
                        if(!el){return}
                        markHtmlList.push(this.addAnnotationMark(data, `${startIndex}_${endIndex}_${this.paragraphIndex}`, {
                            style: `left: ${(el.offsetLeft - 10)+'px'};top: ${(el.offsetTop - 12)+'px'};`
                        }));
                    })
                    this.$set(this, 'contentText', `${baseHtml}${markHtmlList.join('')}`)
                },100)
            })
        },
        // 获取html内容抓换span
        getContentHtmlSpan(item) {
            return `<span class="${this.getMarkClass(item)}" style="${this.getMarkStyleIsCssText(item)}">${item.char}</span>`
        },
        updateData() {
            this.$nextTick(() => {
                this.$emit('updateData', {
                    itemsMap: this.itemsMap,
                    originalMap: this.originalMap,
                    paragraphIndex: this.paragraphIndex,
                    wordListMap: this.wordListMap
                })
            })
        },

        // 获取Css样式
        getMarkStyleIsCssText(item) {
            const objStyle = this.getMarkStyle(item);
            return Object.entries(objStyle || {}).map(([property, value]) => `${property}:${value};`).join(' ');
        },

        clickWord(item) {
            if (this.getIsText) { return }
            this.$emit('playerRead', item.startTime)
        },
        getAnnotationItemClass(item) {
            return this.annotation?.some(l => {
                return l[0] <= item.index && item.index <= l[1];
            }) ? 'annotation-item' : ''
        },
        getOtherMarkClass(item) {
            let cl = ['otherMark_' + this.paragraphIndex + '_' + item.index, this.getAnnotationItemClass(item)]
            return cl.join(' ');
        },
        getMarkClass(item, type) {
            let classNameAry = [`${this.getCharClass}${this.paragraphIndex}_${item.index !== undefined ? item.index : item.pIndex}`];

            // 不是子数据时进行段落标记
            // 文本段落不需要标记
            if (type !== 'sub' && !this.getIsText) {
                // 对段落时间进行标记
                classNameAry.push('time_' + item.startTime + '-' + item.endTime)
                // 如果是文本标注，则不执行读取样式
                // console.log(item.startTime, this.currentTime, item.endTime);
                if (item.startTime < this.currentTime && this.currentTime < item.endTime) {
                    // console.log(item.startTime,this.currentTime,item.endTime);
                    // console.log(item.startTime<this.currentTime , this.currentTime<item.endTime);
                    // console.log(item.startTime<this.currentTime && this.currentTime<item.endTime);
                    classNameAry.push('word-time-hover')
                }
            }
            //当数据有段落标记时 进行段落标记
            if (item.markItem) {
                // 对字段进行标记
                classNameAry.push('mark_' + item.prentIndex + '-' + item.index)
            }
            
            if(!this.getIsText){
                // 添加批注
                const annItem = this.getAnnotationItemClass(item);
                if (annItem) {
                    classNameAry.push(annItem);
                }
            }
            return classNameAry.join(' ')
        },

        //设置文本数据索引对象
        setItemsObject() {
            if (this.getItems) {
                this.getItems?.forEach((item, index) => {
                    // 建立单字段文本索引前，先建立原有数据文本索引
                    this.setOriginalObject(item, index)
                })
            } else {
                this.getContent?.split('')?.forEach((char, index) => {
                    this.setOriginalObject({
                        word: char,
                        char: char,
                        index: index
                    }, index)
                })
            }
        },
        // 设置元数据对象
        setOriginalObject(item, index) {
            // 检查文本是否为多字段文本
            if (item.word.length > 1) {
                // 生成单个字段数据并储存在原始数据中
                this.$set(this.originalMap, index, {
                    ...item,
                    pIndex: index,
                    children: []
                })
                item.word.split('').map(text => {
                    // 储存生成单个数据索引并且储存在原有数据下
                    this.originalMap[index].children.push(this.setTextObject({
                        ...item,
                        char: text,
                        prentIndex: index
                    }))
                })
            } else {
                this.$set(this.originalMap, index, this.setTextObject({
                    ...item,
                    char: item.word,
                    pIndex: index,
                    prentIndex: index
                }))
            }
        },
        //建立单字段文本对象
        setTextObject(item) {
            // 获取字段下标
            let index = this.textIndex;
            // 建立字段对象，储存所需数据
            let o = {
                ...item,
                up: index - 1,
                down: index + 1,
                index: index,
                indexs: [index - 1, index, index + 1]
            }
            //放入map对象
            this.itemsMap[`${index}`] = o;
            //执行一次提升下标
            this.textIndex = this.textIndex + 1;
            // 手动在对象内部描述高度
            this.itemsMap.length = this.textIndex;
            return o;
        },
        // 查询字段:item 配置项，itemKey 是否储存配置项数据到索引数据中，不传入则只返回当前查询数据坐标并建立索引，如果传入则会吧当前item配置项储存到数据中
        indexOfText(item, itemKey) {
            let indexAll = [];

            let index = this.getContent.indexOf(item.name);

            let needIndex = 0; // 敏感词限定词标记 



            while (index !== -1) {
                let datas = [];
                // 如果限定词列表高度一样，则不进行卷标判定可直接标定当前词语，如果高度不一样说明有限定词标定，则需要判断出现次数以及第几次标定别切限定词标记
                let recordNeedsState = item.recordNeedsWordList !== undefined ? item.recordNeedsWordList?.some(i => {
                    return i.recordNeedsNum === needIndex;
                }) : true;
                // 限定词状态通过，卷标和出现卷标匹配相同，则可以记录为有效限定词。
                if (recordNeedsState) {
                    // 首次成功添加
                    if (item.name.length > 1) {
                        // 对整个段落（超出1个字的即为段落）进行挨个标记
                        item.name.split('').forEach((name, sIndex) => {
                            // 设置字段标记项属性名称，用于区分敏感默认与自定义标记的属性名称
                            if (itemKey) {
                                this.$set(this.itemsMap[`${index + sIndex}`], itemKey, item)
                            }
                            datas.push(this.itemsMap[`${index + sIndex}`]);
                        })
                    } else {
                        // 设置字段标记项属性名称，用于区分敏感默认与自定义标记的属性名称
                        if (itemKey) {
                            this.$set(this.itemsMap[`${index}`], itemKey, item)
                        }
                        datas.push(this.itemsMap[`${index}`]);
                    }

                    indexAll.push({
                        index,
                        needIndex,
                        paragraphIndex: this.paragraphIndex,
                        datas
                    });
                }
                //持续查询
                index = this.getContent.indexOf(item.name, index + 1);
                // 需要索引地址
                needIndex = needIndex + 1;
            }

            return {
                paragraphIndex: this.paragraphIndex,
                indexAll
            }
        },
        //搜索查询文本字段标记（自定义的查询字段）
        setKeyWordMark(word) {
            // this.keywordMap = {};
            this.keyWordsCharIndex = [];
            let item = {
                name: word
            }
            // 设置样式
            // this.$set(item,'style',this.setWordMarkstyle('color', this.keyWordColor))
            // 查询位置
            let wordData = this.indexOfText(item);
            // 清空原有标记数据
            Object.keys(this.keywordMap).map(wKey => {
                // 删除原有索引数据
                this.$delete(this.keywordMap, wKey);
            })
            // 储存数据
            this.$set(this.keywordMap, word, wordData)

            // 获取搜索数据的所有字段index
            this.keyWordsCharIndex = [].concat(...wordData.indexAll.map(item => {
                return item.datas.map(i => i?.index);
            }));
            // 跟新数据
            this.$emit('updateKeyWord', { paragraphIndex: this.paragraphIndex, data: this.keywordMap, datas: this.keyWordsCharIndex })

        },
        // 设置文本标记（敏感词）
        setWordMark() {
            this.wordListMap = {};
            this.getWordsList.map(item => {
                // 设置样式
                // this.$set(item,'style'+item?.wordsType, this.setWordMarkstyle(item?.wordsType))
                // 查询位置
                this.wordListMap[item.name + item.wordsType] = this.indexOfText(item, 'markItem' + item?.wordsType);
                // if(item.wordsType === 1){ return }
                this.setQualifyWord(item); //设置限定词
            })
        },
        // 设置限定词信息
        setQualifyWord(item) {
            this.wordListMap[item.name + item.wordsType].indexAll?.forEach(needItem => {
                // 找到需要匹配限定词的敏感词
                let need = item.recordNeedsWordList.find(d => d.restrictWord && d.restrictRange && d.recordNeedsNum === needItem.needIndex);
                if (need) {
                    //限定词
                    let rangeWord = need.restrictWord;
                    // 有限定词数据在进行数据处理
                    let textLeftIndex = needItem.index; //获取敏感词开始位置
                    let texRightIndex = textLeftIndex + needItem?.datas?.length; // 获取敏感词结束位置
                    // 计算向前，和向后查询的坐标点
                    let rangeLeft = textLeftIndex - need.restrictRange < 0 ? 0 : textLeftIndex - need.restrictRange; // 往前查询坐标
                    let rangeRight = texRightIndex + need.restrictRange; //往后查询坐标

                    // 优先查询左边的限定词,type：0-表示左边，1-表示右边
                    let lstr = this.getContent.substring(rangeLeft, textLeftIndex); //取左边范围字段
                    let rstr = this.getContent.substring(texRightIndex, rangeRight); //取右边限定词坐标

                    // 截取左边的数据index
                    let leftIndexs = myUtils.indexOfAll(lstr, rangeWord);
                    // 截取右边的数据index
                    let rightIndexs = myUtils.indexOfAll(rstr, rangeWord);
                    let rangeData = {};
                    // 取左边限定词坐标
                    if (leftIndexs.length) {
                        rangeData = {
                            type: 0,
                            substring: lstr,
                            index: leftIndexs[leftIndexs?.length - 1] //取左边最后的数据
                        };
                    } else if (rightIndexs.length) {
                        rangeData = {
                            type: 1,
                            substring: rstr, // 限定词文本
                            index: rightIndexs[0] // 限定词坐标取第一条数据坐标
                        };
                    }
                    // 最后判断rangeData限定词坐标是否不为-1，如果是-1表示没有限定词，有则打标记
                    if (rangeData.index !== -1) {
                        let rangeWordIndex = rangeData.type ? texRightIndex + rangeData.index : rangeLeft + rangeData.index;
                        let datas = [];
                        if (rangeWord.length > 1) {
                            rangeWord.split('').forEach((s, i) => {
                                // 记录限定词信息
                                this.saveRangeWord(rangeWordIndex + i, item, need, rangeData, 'split')
                                // 打包储存，用于map数据快速索引，只存内存地址
                                datas.push(this.itemsMap[rangeWordIndex + i])
                            })
                        } else {
                            // 记录限定词信息
                            this.saveRangeWord(rangeWordIndex, item, need, rangeData, 'all')
                            // 打包储存
                            datas.push(this.itemsMap[rangeWordIndex])
                        }
                        // 储存限定词
                        if (typeof this.rangeWordMap[rangeWord] === 'undefined') {
                            this.rangeWordMap[rangeWord] = {
                                paragraphIndex: this.paragraphIndex,
                                datas: []
                            }
                        }
                        // 按照限定词文案合并所有标记限定词
                        this.rangeWordMap[rangeWord].datas.push(...datas);
                    }

                }
            })
        },
        // 储存限定词
        saveRangeWord(index, item, need, data, type) {
            // 储存到字段对象
            this.$set(this.itemsMap[index], 'rangeWord', {
                index: index,
                paragraphIndex: this.paragraphIndex,
                wordsType: item.wordsType,
                item,
                need,
                rangeData: data
            })
        },
        // 设置字段样式
        setWordMarkstyle(wordsTypeOrColor, color) {
            //wordsTypeOrColor === 'color 自定义颜色
            if (wordsTypeOrColor === 'color') {
                return {
                    background: color
                }
            } else {
                return {
                    background: this.wordColor[wordsTypeOrColor]
                }
            }
        },
        // 添加点击代理处理函数
        handleContainerClick(event) {
            // 阻止事件冒泡
            event.stopPropagation();
            // 检查点击的目标元素是否为annotation-item-mark
            if (event.target.classList.contains('annotation-item-mark')) {
                // 如果已经点击的就是annotation-item-mark元素，直接处理
                this.handleAnnotationMarkClick(event.target);
            }
        },
        showAnnotationMark(id) {
            let find = Object.values(this.annotationStartIndexMap)?.find(d=>d.id === id);
            if(!find){return;};
            this.annotationMarkHandler(find);
        },
        // 处理批注标记的点击
        handleAnnotationMarkClick(markElement) {
            // 获取批注标记的类名以提取annotation_x中的x
            const classNames = markElement.className.split(' ');
            const annotationClass = classNames.find(cls => cls.startsWith('annotation_'));
            if (annotationClass) {
                let index = annotationClass.replace('annotation_', '')?.split('_')?.[0];
                const annotationItem = this.annotationStartIndexMap[index];
                this.annotationMarkHandler(annotationItem, classNames);
            }
        },
        annotationMarkHandler(item, classNames = []){
            if(!item){return}
            const o = {
                markNo: item.markNo,
                paragraphIndex: this.paragraphIndex,
                className: classNames?.pop() || '',
                annotationItem: item
            };
            // 触发批注标记点击事件，传递批注信息
            this.$emit('annotation-mark-click', o);
        }
    },

};
</script>

<style lang="scss" scoped>
.text-paragraph-box {
    user-select: text;
    cursor: pointer;
    font-size: 14px;
    /* letter-spacing: 0.1em; */
    line-height: 30px;
    /* letter-spacing: 1px; */
    /* 防止子元素被意外选中 */
    // user-select: none;
}

.is-text {
    /* letter-spacing: 0.1em; */
    position: relative;
}

.no-select {
    pointer-events: none;
    user-select: none;
}

.content-text {
    position: relative;
    background: #fff;
    z-index: 77;
    color: transparent;
    // -webkit-text-fill-color: transparent;
    text-shadow: 0 0 0 transparent;
    /* 视觉可见但代码透明 */
    pointer-events: none;
    user-select: none;
    // span{
    //     color: none;
    // }
}


.text-bg {
    position: absolute;
    /* background: #fff; */
    opacity: 1;
    z-index: 88;
    top: 0;
    user-select: text;
    /* 允许选中 */
    pointer-events: auto;
    /* 允许交互 */
    /* 关键：阻止事件冒泡影响父容器 */
    isolation: isolate;
    /* 或使用 mix-blend-mode: normal; */
    // display: none;

}

.text-paragraph-box * {
    cursor: pointer;
    /* display:inline-block;
    margin: 0;
    padding: 0; */
}

.word-time-hover {
    cursor: pointer;
    background: #336df4;
    border-radius: 4px;
    color: #FFF;
}

.text_item{
    position: relative;
}

::v-deep(.annotation-item) {
    position: relative;
    border-bottom: 1px solid #F8307B !important;
}
::v-deep(.annotation-item-mark){
    white-space: nowrap;
    position: absolute;
    top: -12px;
    left: -10px;
    line-height: 18px;
    height: 18px;
    vertical-align: middle;
    font-size: 10px;
    border-radius: 4px;
    display: inline-block;
    padding: 2px;
    color: #F8307B !important;
    background: #FED7E6;
    pointer-events: all;
    /* 修改为all，确保可以接收点击事件 */
    user-select: none;
    cursor: pointer;
    z-index: 99;
    opacity: 0.6;
    &:hover{
        opacity: 1;
    }
    /* 添加鼠标指针样式 */
    // color: var(--text-c2) !important;
    // border:1px solid var(--text-c2);
}
</style>
