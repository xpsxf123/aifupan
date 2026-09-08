export default {
    props: {
        /**
         * 类型标识符，用于区分不同的功能模块
         * 可选值包括：
         * - 'assistant': 运营 assistant
         * - 'violation': 违规 violation
         * - 'scrolling': 弹幕 scrolling
         * - 'dataBoard': 数据看板 dataBoard
         * - 'dataCapture': AI数据识图 dataCapture
         * - 'textAssistant': 话术分析 textAssistant
         * 
         * @type {String}
         * @default ''
         */
        type: {
            type: String,
            default: ''
        }
    },
    data() {
        return { 
            CueTypeLabelMap: {
                'assistant': '运营',
                'violation': '违规',
                'scrolling': '弹幕',
                'dataBoard': '数据看板',
                'dataCapture': 'AI数据识图',
                'textAssistant': '话术分析'
            },
            CueTypeNumberMap:{
                0:'assistant',
                1:'violation',
                2:'scrolling',
                3:'dataCapture',
                4:'dataBoard',
                6:'textAssistant'
            },
            CueTypeKeyMap:{
                assistant: 0,
                violation: 1,
                scrolling: 2,
                dataCapture: 3,
                dataBoard: 4,
                textAssistant: 6
            }
        }
    },
    computed: {
        getAiCueTypeKeyValue(){
            return this.CueTypeNumberMap[this.type] || this.type
        },
        /**
         * 判断当前类型是否为 'scrolling'
         * 
         * @returns {boolean} 如果类型为 'scrolling' 返回 true，否则返回 false
         */
        isScrolling() {
            return this.getAiCueTypeKeyValue ===  'scrolling';
        }, 
        /**
         * 判断当前类型是否为 'dataBoard'
         * 
         * @returns {boolean} 如果类型为 'dataBoard' 返回 true，否则返回 false
         */
        isDataBoard() {
            return this.getAiCueTypeKeyValue ===  'dataBoard';
        },
        /**
         * 判断当前类型是否为 'dataCapture'
         * 
         * @returns {boolean} 如果类型为 'dataCapture' 返回 true，否则返回 false
         */
        isDataCapture() {
            return this.getAiCueTypeKeyValue ===  'dataCapture';
        },
        /**
         * 判断当前类型是否为 'assistant'
         * 
         * @returns {boolean} 如果类型为 'assistant' 返回 true，否则返回 false
         */
        isAssistant() {
            return this.getAiCueTypeKeyValue ===  'assistant';
        },
        /**
         * 判断当前类型是否为 'violation'
         * 
         * @returns {boolean} 如果类型为 'violation' 返回 true，否则返回 false
         */
        isViolation() {
            return this.getAiCueTypeKeyValue ===  'violation';
        },
        /**
         * 判断当前类型是否为 'textAssistant'
         * 
         * @returns {boolean} 如果类型为 'textAssistant' 返回 true，否则返回 false
         */
        isTextAssistant() {
            return this.getAiCueTypeKeyValue ===  'textAssistant';
        },
        /**
         * 判断当前类型是否为 'violation' 或 'assistant'
         * 
         * @returns {boolean} 如果类型为 'violation' 或 'assistant' 返回 true，否则返回 false
         */
        isWordDiscern() {
            return this.isViolation || this.isAssistant || this.isTextAssistant;
        },
        /**
         * 根据当前类型获取对应的提示类型
         * 此函数没有输入参数，但依赖于实例的'type'属性
         * 返回一个整数，代表不同的提示类型
         */
        getCueType(){
            // 根据当前实例的'type'属性值，决定返回对应的提示类型
            return this.CueTypeKeyMap[this.getAiCueTypeKeyValue] || 0;
        },
        /**
         * 根据类型获取提示标签
         * 
         * 此函数用于根据当前对象的type属性返回相应的字符串标签
         * 它定义了一系列类型与人类可读标签之间的映射关系
         * 如果类型不在预定义的映射中，则默认返回'运营'
         * 
         * @returns {string} 根据type属性映射的标签，如果type不匹配任何预定义值，则返回'运营'
         */
        getCueTypeLabel(){
            return this.CueTypeLabelMap[this.getAiCueTypeKeyValue];
        },
        getAiTypeTitle(){
            return this.getCueTypeLabel + '助手';
        }
    }
}