export default [
    {
        el: '#sensitive-keyword-dom',
        step: {
            on: 'bottom',
            text: '点击取消标注后，关键词汇总列表和话术区域将不再展示敏感词和关键词。',
            canClickTarget: true
        }
    },
    {
        el: '#pace-dom',
        step: {
            on: 'bottom',
            text: '针对不同的人群、人设和不同赛道，语速能影响停留效果。',
            canClickTarget: true
        }
    },
    {
        el: '#share-dom',
        name: 'share',
        step: {
            on: 'bottom',
            text: '可以将本场直播分析上传到云空间，并分享给同事。上传云空间后，即使不在同一台电脑上，也可以进行直播复盘查看。',
            canClickTarget: true
        }
    },
    {
        el: '#toolbar-search-dom',
        name: 'text1',
        step: {
            text: '搜索您重点关注的内容，点击“蓝色箭头”能调到对应的位置。',
            canClickTarget: true
        }
    },
    {
        el: '#textParagraph-1-dom',
        name: 'wordsNext',
        step: {
            text: '<span class="">鼠标左键双击任意文字</span>，可以跳转到对应的视频和音频。',
            canClickTarget: true
        }
    },
    {
        el: '#wordsType-dom',
        name: 'compass',
        step: {
            text: '运营关键词的类型、次数、和百分比，决定了直播间的核心运营指标。例如：同一个直播间的水平相似的主播，互动词越多，表示其要互动的频次越高，则互动率大概率就是高的。',
            canClickTarget: true
        }
    },
    {
        el: '#compass-dom',
        name: 'wordsBack',
        step: {
            text: '点击并查看内容罗盘，能通过图表的方式，与标准内容模型进行更直观的对比。',
            canClickTarget: true
        }
    },
    {
        el: '#barchat-1-dom',
        step: {
            text: '柱状图能帮助更直观的评估直播间内容与标准内容模型',
            canClickTarget: true
        }
    },
    {
        el: '#compass-chat-dom',
        name: 'model',
        step: {
            text: '柱状图能帮助更直观的评估直播间模型内的所有指标数据',
            canClickTarget: false,
        }
    },
    {
        el: '#compass-model-dom',
        name: 'chat',
        step: {
            text: '不同的赛道和不同的变现方式，决定了什么样的内容模型适合当前直播间，通过切换模型可以看到不同模型下直播间的表现情况。',
            canClickTarget: true,
            buttons: {
                nextText: '结束'
            }
        }
    }
]