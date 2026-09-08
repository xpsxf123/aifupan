<template>
    <el-dialog
        class="lifeAuthDialog"
        title="来客授权"
        :visible.sync="visible"
        width="420px"
        :modal="false"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        :show-close="true"
        @close="handleClose">
        <div class="content">
            <div v-if="qrSrc" class="qr-wrap">
                <img :src="qrSrc" class="qr-img" alt="来客授权二维码"/>
                <div class="qr-tip">抖音扫码授权</div>
            </div>
            <div v-else class="placeholder">
                <div>正在打开来客授权窗口，请在弹出的授权窗口中扫码完成授权</div>
            </div>
        </div>
        <div slot="footer" class="footer">
            <afp-button size="default" @click="handleClose">取 消</afp-button>
            <afp-button size="default" type="primary" :plain="false" @click="handleAuthorized">我已完成授权</afp-button>
        </div>
    </el-dialog>
</template>

<script>
export default {
    name: 'LifeAuthDialog',
    data() {
        return {
            visible: false,
            secUid: '',
            qrSrc: ''
        }
    },
    methods: {
        open(secUid) {
            this.secUid = secUid || ''
            this.qrSrc = ''
            this.visible = true
            this.fetchAuthorizeQr()
        },
        resolveQrSrc(res) {
            const data = res?.data
            if (!data) return ''
            if (typeof data === 'string') {
                if (data.startsWith('data:image') || data.startsWith('http')) return data
                return ''
            }
            const candidates = [
                data.qrCodeUrl,
                data.qrcodeUrl,
                data.qrCode,
                data.qrcode,
                data.url,
                data.image,
                data.base64
            ]
            const hit = candidates.find(v => typeof v === 'string' && v)
            if (!hit) return ''
            if (hit.startsWith('data:image') || hit.startsWith('http')) return hit
            return ''
        },
        fetchAuthorizeQr() {
            if (!this.$httpClient?.life?.authorizeLife) {
                return
            }
            this.$httpClient.life.authorizeLife({secUid: this.secUid, authType: 0}).then((res) => {
                const qrSrc = this.resolveQrSrc(res)
                if (qrSrc) {
                    this.qrSrc = qrSrc
                }
            }).catch(() => {
            })
        },
        handleAuthorized() {
            this.visible = false
            const secUid = this.secUid
            this.secUid = ''
            this.qrSrc = ''
            this.$emit('authorized', {secUid})
        },
        handleClose() {
            this.visible = false
            this.secUid = ''
            this.qrSrc = ''
        }
    }
}
</script>

<style lang="scss" scoped>
.lifeAuthDialog{
    .content{
        padding: 8px 0 4px;
    }

    .qr-wrap{
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 10px 0 4px;
    }

    .qr-img{
        width: 210px;
        height: 210px;
        object-fit: contain;
        border: 1px solid #EFF2F6;
        border-radius: 6px;
        background: #fff;
    }

    .qr-tip{
        margin-top: 10px;
        font-size: 13px;
        color: #606266;
    }

    .placeholder{
        color: #606266;
        line-height: 20px;
        padding: 10px 0;
    }

    .footer{
        text-align: center;
    }
}
</style>

