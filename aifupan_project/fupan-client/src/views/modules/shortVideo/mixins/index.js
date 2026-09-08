import myUtils from "@/utils/utils";
import TextDialog from "@/views/modules/shortVideo/component/textDialog.vue";

export default function(...args) {
    const isNotCopy = args.includes('notCopy');
    return {
    components: {TextDialog},
    data() {
        return {
            selectionChangeList: [],
            currentVideoItem: {},
            textVisible: false,
            extractOptions: [{
                label: '提取文案',
                show: (item) => {
                    return item.extractStatus === 0
                },
                click: (item) => {
                    this.selectionChangeList = [item]
                    this.batchCreateExtract?.()
                }
            }, 
            {
                label: '查看文案',
                type: 'success',
                show: (item) => {
                    return item.extractStatus === 3
                },
                click: (item) => {
                    if (!item?.extractId) return this.$message.error("获取文案失败")
                    this.currentVideoItem = item
                    this.textVisible = true
                }
            },
            ...(isNotCopy ? [] : [{
                label: '复制文案',
                show: (item) => {
                    return item.extractStatus === 3
                },
                click: async (item) => {
                    const result = await this.$httpBack.shortVideo.getContent(item.extractId)
                    if (result.code !== 0 || !result.data?.audioContent) return this.$message.error('获取文案失败')
                    await myUtils.copyToClipboard(result.data?.audioContent?.replace(/\*\*/g, ''))
                    this.$message.success('文案复制成功')
                }
            }]),
            {
                label: '重新提取',
                type: 'danger',
                show: (item) => {
                    return item.extractStatus === 4
                },
                click: async (item) => {
                    await this.extractAgain(item)
                }
            }, {
                label: '提取中...',
                type: 'warning',
                disabled: true,
                plain: true,
                show: (item) => {
                    return item.extractStatus === 2
                },
            }, {
                label: '待处理',
                type: 'info',
                disabled: true,
                plain: true,
                show: (item) => {
                    return item.extractStatus === 1
                },
            }]
        }
    },
    created() {
        this.tableConfig = {
            ...this.tableConfig,
            options: [...this.extractOptions, ...(this.tableConfig?.options || [])]
        }
    },
    watch: {
        textVisible: {
            handler(val) {
                if (!val) this.currentVideoItem = {};
            },
            deep: true,
            immediate: true
        }
    },
    computed: {},
    methods: {
        //重新提取文案
        async reExtract(item, callback) {
            if (!item.id) return this.$message.error('获取视频信息失败，请稍后再试')
            const result = await this.$httpClient.shortVideo.reExtract(item)
            if (result.code !== 0) return this.$message.error(result.msg)
            await callback?.()
            this.$message.success('重新提取处理成功')
        },
        //创建提取文案任务
        async createExtract(item) {
            const list = this.selectionChangeList?.filter(a => a.duration <= 900) || []
            const filterList = this.selectionChangeList?.filter(a => a.duration > 900) || []
            if (filterList.length !== 0) this.$message.warning('提取文案的视频时长需小于15分钟')
            if (list.length === 0) return
            const videoInfoVos = list.map(_item => {
                return {
                    platformType: _item.platformType,
                    platformVideoId: _item.platformVideoId
                }
            })
            try {
                const result = await this.$httpClient.shortVideo.batchCreateExtract({
                    ...item,
                    videoInfoVos: videoInfoVos
                })
                if (result.code !== 0) return
                this.$message.success('文案提取中，每条耗时1分钟左右')
            } catch (e) {

            } finally {
                this.selectionChangeList = []
            }
        }
    },
}
}


