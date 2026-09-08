<!--
@description AI 智能体手机使用入口：点击按钮后展示二维码气泡，便于手机扫码进入工作台。
注意：扫码地址复用工作台配置来源，但不会拼接桌面 token，避免直接透传客户端登录态。
-->
<template>
    <el-popover
        v-model="popoverVisible"
        placement="top"
        width="180"
        trigger="click"
        popper-class="ai-agent-mobile-popover"
    >
        <div class="ai-agent-mobile-qr">
            <div v-if="qrLoading" class="ai-agent-mobile-qr__status">
                二维码生成中...
            </div>
            <div v-else-if="qrError" class="ai-agent-mobile-qr__status is-error">
                {{ qrError }}
            </div>
            <div v-else class="ai-agent-mobile-qr__image-wrap">
                <img class="ai-agent-mobile-qr__image" :src="qrImageUrl" alt="手机扫码使用 AI 智能体">
            </div>
            <div class="ai-agent-mobile-qr__tip">手机扫码使用</div>
        </div>
        <afp-button
            slot="reference"
            class="ai-agent-mobile-button"
            @click="handleButtonClick"
        >
            微信扫码使用
        </afp-button>
    </el-popover>
</template>

<script>
/**
 * @file AI 智能体手机扫码入口组件。
 * @description 负责读取 AI 工作台配置地址、去除 token 占位，并在前端点击后展示二维码气泡。
 */
import QRCode from 'qrcode'
import aiElfLogo from '@/assets/imgs/aiElf.png'

export default {
    name: 'AiAgentMobileQrAction',
    data() {
        return {
            popoverVisible: false,
            qrLoading: false,
            qrError: '',
            qrImageUrl: ''
        }
    },
    methods: {
        /**
         * @description 点击按钮后按需加载二维码，避免首屏多余请求。
         * @returns {Promise<void>}
         */
        async handleButtonClick() {
            if (this.popoverVisible) {
                return
            }
            this.qrError = ''
            if (this.qrImageUrl) {
                return
            }
            this.qrLoading = true
            try {
                const url = await this.getWorkbenchUrl({ includeToken: false })
                if (!url) {
                    this.qrError = '暂未获取到可用链接'
                    return
                }
                this.qrImageUrl = await this.buildQrCodeImageUrl(url)
            } catch (e) {
                this.qrError = '二维码生成失败，请稍后重试'
            } finally {
                this.qrLoading = false
            }
        },
        /**
         * @description 获取 AI 工作台地址，供手机二维码复用同一套配置来源。
         * @param {{ includeToken?: boolean }} options 地址选项
         * @returns {Promise<string>}
         */
        async getWorkbenchUrl(options = {}) {
            const { includeToken = true } = options
            const httpBack = require('@/utils/request-api-back').default
            const res = await httpBack.common.getByKey('ai_workbench_page')
            const urlTemplate = res?.data?.kvValue || ''
            return this.normalizeWorkbenchUrl(urlTemplate, {
                includeToken,
                token: includeToken ? (this.$store.getters.getToken || '') : ''
            })
        },
        /**
         * @description 规范化工作台地址；扫码模式会移除 token 参数占位，避免透传桌面登录态。
         * @param {string} urlTemplate 后端配置的 URL 模板
         * @param {{ includeToken?: boolean, token?: string }} options 地址处理选项
         * @returns {string}
         */
        normalizeWorkbenchUrl(urlTemplate, options = {}) {
            const { includeToken = true, token = '' } = options
            const rawUrl = String(urlTemplate || '').trim()
            if (!rawUrl) {
                return ''
            }
            const [urlWithoutHash, hashFragment] = rawUrl.split('#')
            const [basePath = '', search = ''] = urlWithoutHash.split('?')
            const tokenValue = encodeURIComponent(token)
            const normalizedBasePath = includeToken
                ? basePath.replace(/\{token\}/g, tokenValue)
                : basePath.replace(/\{token\}/g, '')

            // 扫码地址不透传 token，因此直接移除包含 {token} 的查询参数。
            const searchList = search
                .split('&')
                .filter(Boolean)
                .reduce((list, segment) => {
                    if (!segment.includes('{token}')) {
                        list.push(segment)
                        return list
                    }
                    if (!includeToken) {
                        return list
                    }
                    list.push(segment.replace(/\{token\}/g, tokenValue))
                    return list
                }, [])

            const queryString = searchList.length ? `?${searchList.join('&')}` : ''
            const hashString = typeof hashFragment === 'string' ? `#${hashFragment}` : ''
            return `${normalizedBasePath}${queryString}${hashString}`
        },
        /**
         * @description 使用前端二维码库生成图片，并将 AI 精灵 logo 合成到二维码中心。
         * @param {string} url 需要编码的目标地址
         * @returns {Promise<string>}
         */
        async buildQrCodeImageUrl(url) {
            const size = 150
            const qrCanvas = document.createElement('canvas')
            await QRCode.toCanvas(qrCanvas, url, {
                width: size,
                margin: 1,
                errorCorrectionLevel: 'H',
                color: {
                    dark: '#2E3742',
                    light: '#FFFFFF'
                }
            })

            const canvas = document.createElement('canvas')
            canvas.width = size
            canvas.height = size
            const ctx = canvas.getContext('2d')
            ctx.drawImage(qrCanvas, 0, 0, size, size)

            // 中央增加白底 logo，兼顾识别率与品牌感知。
            const logo = await this.loadImage(aiElfLogo)
            const logoBoxSize = 34
            const logoSize = 24
            const logoX = (size - logoBoxSize) / 2
            const logoY = (size - logoBoxSize) / 2
            this.drawRoundRect(ctx, logoX, logoY, logoBoxSize, logoBoxSize, 10)
            ctx.fillStyle = 'rgba(255, 255, 255, 0.96)'
            ctx.fill()
            ctx.drawImage(
                logo,
                (size - logoSize) / 2,
                (size - logoSize) / 2,
                logoSize,
                logoSize
            )

            return canvas.toDataURL('image/png')
        },
        /**
         * @description 加载图片资源，供二维码合成 logo 使用。
         * @param {string} src 图片地址
         * @returns {Promise<HTMLImageElement>}
         */
        loadImage(src) {
            return new Promise((resolve, reject) => {
                const image = new Image()
                image.onload = () => resolve(image)
                image.onerror = reject
                image.src = src
            })
        },
        /**
         * @description 绘制圆角矩形，作为二维码中间 logo 的白底容器。
         * @param {CanvasRenderingContext2D} ctx 画布上下文
         * @param {number} x 起始横坐标
         * @param {number} y 起始纵坐标
         * @param {number} width 宽度
         * @param {number} height 高度
         * @param {number} radius 圆角半径
         * @returns {void}
         */
        drawRoundRect(ctx, x, y, width, height, radius) {
            ctx.beginPath()
            ctx.moveTo(x + radius, y)
            ctx.lineTo(x + width - radius, y)
            ctx.quadraticCurveTo(x + width, y, x + width, y + radius)
            ctx.lineTo(x + width, y + height - radius)
            ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height)
            ctx.lineTo(x + radius, y + height)
            ctx.quadraticCurveTo(x, y + height, x, y + height - radius)
            ctx.lineTo(x, y + radius)
            ctx.quadraticCurveTo(x, y, x + radius, y)
            ctx.closePath()
        }
    }
}
</script>

<style scoped lang="scss">
.ai-agent-mobile-button{
    width: 178px;
    height: 62px !important;
    border-radius: 62px !important;
    background: #444DFF !important;
    border: 1px solid #444DFF !important;
    box-shadow: 0 1px 3px rgba(68,77,255,0.3), 0 4px 8px 3px rgba(68,77,255,0.3) !important;
    color: #fff !important;
}

::v-deep(.ai-agent-mobile-button span){
    font-size: 24px;
    font-weight: 500;
}

::v-deep(.ai-agent-mobile-popover){
    padding: 14px 14px 12px;
    border-radius: 20px;
    border: 1px solid #ebeef5;
    box-shadow: 0 12px 30px rgba(61, 73, 119, 0.14);
}

.ai-agent-mobile-qr{
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 158px;
}

.ai-agent-mobile-qr__status{
    display: flex;
    align-items: center;
    justify-content: center;
    width: 150px;
    height: 150px;
    border-radius: 18px;
    background: #f6f7fb;
    color: #6c7386;
    font-size: 14px;
    line-height: 20px;
    text-align: center;
}

.ai-agent-mobile-qr__status.is-error{
    color: #f56c6c;
}

.ai-agent-mobile-qr__image-wrap{
    position: relative;
    width: 150px;
    height: 150px;
    padding: 0;
    border-radius: 18px;
    background: #fff;
    box-shadow: inset 0 0 0 1px #eef0f7;
    box-sizing: border-box;
    overflow: hidden;
}

.ai-agent-mobile-qr__image{
    display: block;
    width: 150px;
    height: 150px;
}

.ai-agent-mobile-qr__tip{
    margin-top: 10px;
    font-size: 13px;
    line-height: 18px;
    color: #6c7386;
}

@media (max-width: 900px) {
    .ai-agent-mobile-button{
        width: 100%;
    }

    ::v-deep(.ai-agent-mobile-button span){
        font-size: 18px;
    }
}
</style>
