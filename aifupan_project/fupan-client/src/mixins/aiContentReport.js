export default {
    data() {
        return {
            uploadType:1,
            activeTab: '',
            tabsList: [],
            selectedCueWordsList: {},
            changeUnSelectIds: [],
            selectedRow: {},
            isReport: false,
            drawerStatus: false,
            sponsorship: ''
        };
    },
    methods: {
        hideReportModal(status) {
            this.isReport = status
        },
        assembly() {
            const selectedCueWordsList = {}
            this.tabsList?.forEach((group) => {
                const name = group.cueType.toString();
                selectedCueWordsList[name] = [];
                group.cueWordsList.forEach((word) => {
                    const {cueWordsIds = []} = this.diagnosisParams
                    if (cueWordsIds?.includes(word.cueWordsId)) {
                        selectedCueWordsList[name].push(word.cueWordsId);
                    }
                });
            });
            this.selectedCueWordsList = selectedCueWordsList
        },
        async getListDiagnosis(isAddCompere) {
            const result = await this.$httpBack.v2500.listDiagnosis({
                sourceId: this.selectedRow?.videoId || this.selectedRow?.sourceId,
                sourceType: this.selectedRow?.videoId ? 1 : 0,
                tradeId: this.selectedRow?.tradeId,
            })
            if (result.code === 0) {
                this.aiModel = this.isAdd ? (this.aiModel || result.data?.modelId || this.modelOptions[0]?.id) : (result.data?.modelId || this.modelOptions[0]?.id)
                this.sponsorship = result.data?.modelName || ''
                const list = result.data?.list || []
                list.forEach(item => {
                    item.name = item.cueType.toString()
                    item.checkAll = false
                    item.isIndeterminate = false
                })
                this.tabsList = list
                this.activeTab = this.activeTab || list[0]?.name
                this.$nextTick(() => {
                    if (!this.diagnosisParams?.modelId) {
                        let selectedCueWordsList = {}
                        this.tabsList.forEach(item => {
                            selectedCueWordsList[item.name] = []
                            // 非新增主播(AI复盘、主播列表基础设置)
                            if (!isAddCompere) {
                                item.cueWordsList.forEach((word) => {
                                    // 设置默认选中
                                    const satisfy = this.isReplay ? (word?.anchorSelect === 1 && word?.qaStatus === 2) : word?.anchorSelect === 1;
                                    if (satisfy) {
                                        selectedCueWordsList[item.name].push(word.cueWordsId);
                                    }
                                });
                            }
                        })

                        const merged = {};
                        const keys = new Set([...Object.keys(selectedCueWordsList), ...Object.keys(this.selectedCueWordsList)]);

                        keys.forEach(key => {
                            const arrA = selectedCueWordsList[key] || [];
                            const arrB = this.selectedCueWordsList[key] || [];
                            // 合并 -> 去重 -> 过滤掉不选中的值
                            merged[key] = Array.from(new Set([...arrA, ...arrB]))
                                .filter(val => !this.changeUnSelectIds.includes(val));
                        });
                        this.selectedCueWordsList = merged
                    } else {
                        this.assembly()
                    }
                })

                this.$nextTick(() => {
                    this.scheduleNextPoll?.()
                })
                return list
            }
        }
    }
}