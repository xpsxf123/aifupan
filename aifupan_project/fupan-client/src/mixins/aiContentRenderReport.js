import {isEmpty} from "lodash";

export default {
	data() {
		return {
			renderList:{}
		}
	},
	methods: {
		async getAssemblyReportData(tabsList,ids,videoId,tradeId) {
			// 因为目前AI数据诊断报告只有一个问题id，为了方便以后扩展，这里将wordsId封装成数组
			const renderList = {}
			for (let i = 0; i < tabsList.length; i++) {
				const selectIdList = ids[tabsList[i].name]
				if (selectIdList?.length > 0) {
					renderList[tabsList[i].name] = {
						label: tabsList[i].tagName,
						ids: selectIdList,
						list: []
					}
				}
			}
			const cueWordsIds = Object.values(ids).flat(Infinity)
			const {data: QAList, code} = await this.$httpBack.v2500.conversationByCueWordsIds({
				videoId:videoId,
				tradeId: tradeId,
				cueWordsIds
			})
			if (code !== 0) return
			for (const key in renderList) {
				if (renderList.hasOwnProperty(key)) {
					const renderListIds = new Set(renderList[key].ids || [])

					// 渲染的pdf根据tabsList===>sort排序
					const currentSortItems = tabsList.find(a => a.name === key)
					const {cueWordsList = []} = currentSortItems || {}
					const list = QAList.filter(item => renderListIds.has(item.cueWordsId))
					list.forEach(a => {
						const match = cueWordsList?.find(word => word.cueWordsId === a.cueWordsId);
						if (!isEmpty(match)) a.sort = match.sort;
					});

					renderList[key].list = list.sort((a, b) => a.sort - b.sort);
				}
			}
			this.renderList = renderList
		}
	}
}