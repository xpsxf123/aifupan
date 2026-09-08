<template>
    <div>
    <el-dialog
        :visible.sync="statusDialog"
        :show-close="false"
        :close-on-click-modal="false"
        custom-class="addCompereStatusDialog"
        width="420px">
        <div style="text-align: center">
            <img :src="statusDialogIcon[status]" style="width: 60px;margin-top: 10px;"/>
            <div style="font-size: 18px;margin-top: 10px;font-weight: bold">{{ statusDialogTitle }}</div>
            <div v-if="status==='success'">
                <div class="flex items-center justify-center text-left" style="margin-top: 10px;">
                    <img :src="dialogResult.AnchorAvatar" alt="头像"
                         style="min-width: 40px;height:40px;border-radius: 50%"/>
                    <div style="margin-left: 12px;">
                        <div>{{ dialogResult.AnchorName }}</div>
                        <div>{{ dialogResult.broadcastUrls || dialogResult.ksUrls }}</div>
                    </div>
                </div>
                <div v-if="dialogResult.AccountType===0 && platform === 0">
                    <div class="text-center" style="padding-block: 8px">
                        *自有账号建议完成授权，数据分析和AI复盘更精准
                    </div>
                    <div class="flex items-center justify-center">
                        <LiveRoomAuthorizeDialog
                            ref="authButtons"
                            mode="button"
                            :secUid="resolvedSecUid"
                            :platforms="['juliang', 'life']"
                            layout="horizontal"
                            @authorized="handleAuthDialogAuthorized"
                        />
                    </div>
                </div>
            </div>
            <div style="margin-top: 10px;" v-if="status==='error' && platform === 0">
                请检查{{ dialogResult.urlType === 0 ? '账号' : '直播间地址' }}（<span
                style="color:var(--color-main);">{{ dialogResult.broadcastUrls || dialogResult.ksUrls }}</span>）是否正确，或者1分钟后再试
            </div>
            <div v-else-if="status==='error' && platform === 2">
                <span v-if="dialogResult.message">
                    {{ dialogResult.message }}
                </span>
                <span v-else>
                    请检查授权视频号是否正确,或者1分钟后再试
                </span>
            </div>
            <div style="margin-top: 10px;" v-if="status==='warning'">
                您已添加{{ dialogResult.addCompereNumber }}个直播间，请删除部分直播间后，再进行添加
            </div>
            <div style="margin-top: 10px;" v-if="status==='info'">
                您已添加过 <span style="color: var(--color-main);">({{ dialogResult.broadcastUrls  || dialogResult.ksUrls || dialogResult.AnchorName }})</span> 此账号，无需再次添加
            </div>
        </div>
        <div slot="footer" class="footer" style="text-align: center;margin-top: 12px">
            <afp-button @click="closeStatusDialog('right')" size="default">{{ confirmButtonText }}</afp-button>
            <afp-button  @click="closeStatusDialog" size="default">去录制</afp-button>
        </div>
    </el-dialog>

    <el-dialog
        :visible.sync="authSuccessVisible"
        :show-close="false"
        :close-on-click-modal="false"
        custom-class="addCompereStatusDialog"
        width="420px">
        <div style="text-align: center">
            <img :src="statusDialogIcon.success" style="width: 60px;margin-top: 10px;"/>
            <div style="font-size: 18px;margin-top: 10px;font-weight: bold">授权成功</div>
            <div style="margin-top: 10px;color: #606266;">
                正在拉取最近7天数据，请在10分钟后查看直播间列表
            </div>
            <div style="margin-top: 6px;color: #909399;">
                （平台无法拉取当天数据，且仅下载5小时以内的视频）
            </div>
        </div>
        <div slot="footer" class="footer" style="text-align: center;margin-top: 12px">
            <afp-button @click="handleAuthSuccessContinue" size="default">继续添加</afp-button>
            <afp-button  @click="handleAuthSuccessToList" size="default">去查看</afp-button>
        </div>
    </el-dialog>

    </div>
</template>

<script>
import buyIn from '@/mixins/buyIn'
import LiveRoomAuthorizeDialog from '@/components/liveRoomAuthorizeDialog.vue'

export default {
    components: { LiveRoomAuthorizeDialog },
    mixins: [buyIn],
    props: {},
    data() {
        return {
            statusDialog: false, // 添加成功弹窗
            statusDialogTitle: '添加成功',
            confirmButtonText: '继续添加',
            statusDialogIcon: {
                success: require('@/assets/imgs/add_compere_success.png'),
                error: require('@/assets/imgs/add_compere_error.png'),
                warning: require('@/assets/imgs/add_compere_warn.png'),
                info: require('@/assets/imgs/add_compere_warn.png')
            },
            confirmButtonContent: ``,
            status: 'success',
            dialogResult: {},
            platform: 0,
            authSuccessVisible: false
        }
    },
    computed: {
        /** 兼容多种 secUid 字段名（C# 返回的 AnchorInfo 字段名可能不一致） */
        resolvedSecUid() {
            const d = this.dialogResult || {}
            const candidates = [d.SecUid, d.secUid, d.anchorUrlSecUid, d.anchorUrlSecuid]
            for (const item of candidates) {
                if (item !== undefined && item !== null && String(item).trim()) {
                    return String(item).trim()
                }
            }
            return ''
        }
    },
    watch: {},
    methods: {
        openStatusDialog(result = {}) {
            this.confirmButtonText = '继续添加';
            this.dialogResult = result
            this.platform = result.platform
            this.status = result.status;
            if (result.status === 'error') {
                this.statusDialogTitle = '添加失败';
            } else if (result.status === 'warning') {
                this.statusDialogTitle = '添加上限';
                this.confirmButtonText = '前去删除';
            } else if (result.status === 'info') {
                this.statusDialogTitle = '重复添加';
            } else {
                this.statusDialogTitle = '添加成功';
            }
            // 视频号逻辑
            if(result.message){
                this.confirmButtonText = '重新添加';
            }
            this.statusDialog = true;
            // 添加成功且已有授权，自动触发数据拉取（后台自动拉取，前端无需调接口）
            // if (result.status === 'success' && this.platform === 0 && Number(result.juliangAuthStatus) === 1) {
            //     this.pullVideoDataAfterAuth(result.SecUid)
            // }
        },
        closeStatusDialog(type) {
            this.statusDialog = false;

            this.$emit('cleseStatus', {
                dialogResult: this.dialogResult,
                platform: this.platform,
                status: this.status,
                type: type
            })
            if(this.status !== 'warning' && type === 'right'){
                this.$emit('cleseStatus',{type,status:this.status})
                return;
            }
            this.$emit('toCompereList',type)
        },
        handleAuthDialogAuthorized() {
            this.statusDialog = false
            this.authSuccessVisible = true
        },
        // /**
        //  * @description 授权成功后触发后台拉取直播数据（巨量），后台自动拉取，前端无需调接口
        //  * @param {string} secUid 主播 sec_uid
        //  */
        // pullVideoDataAfterAuth(secUid) {
        //     if (!secUid || !this.$httpClient?.buyIn?.pullVideoData) return
        //     this.$httpClient.buyIn.pullVideoData({secUid, platform: 'juliang'}).catch(() => {})
        // },
        handleAuthSuccessContinue() {
            this.authSuccessVisible = false
            this.closeStatusDialog('right')
        },
        handleAuthSuccessToList() {
            this.authSuccessVisible = false
            this.closeStatusDialog()
        }
    },
    created() {

    },
    mounted() {
        this.watchAuthorizedBuyInSuccess(() => {
            this.statusDialog = false
            this.authSuccessVisible = true
            // 后台自动拉取数据，前端无需调接口
            // this.pullVideoDataAfterAuth(this.dialogResult.SecUid)
        })
        this.watchAuthorizedBuyInError()
    },
    beforeCreate() {
    }, //生命周期 - 创建之前
    beforeMount() {
    }, //生命周期 - 挂载之前
    beforeUpdate() {
    }, //生命周期 - 更新之前
    updated() {
    }, //生命周期 - 更新之后
    beforeDestroy() {
    }, //生命周期 - 销毁之前
    destroyed() {
    }, //生命周期 - 销毁完成
    activated() {
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
