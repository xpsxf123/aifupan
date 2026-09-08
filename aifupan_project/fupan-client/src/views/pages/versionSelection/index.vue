<template>
    <div class="pricing-container">
        <div class="logoImg"></div>
        <h1 class="main-title">请选择您使用的客户端版本</h1>

        <div class="pricing-card-box">
            <div class="pricing-wrapper">
                <!-- 纯录制版 -->
                <div class="card pure-card">
                    <div class="title-header pure-header">
                        纯录制版
                    </div>
                    <div class="card-body pure-body">
                        <ul class="feature-list">
                            <li v-for="(item, index) in pureFeatures" :key="index">
                                <span class="dot pure-dot"></span>
                                <span class="text" v-html="item"></span>
                            </li>
                        </ul>
                        <div class="card-footer">
                            <div class="btn-wrap">
                                <afp-button class="action-button pure-button"
                                            @click="handleSelectVersion(VERSION_TYPE.PURE)">
                                    点我选择纯录制版
                                </afp-button>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- AI全能版 -->
                <div class="card ai-card">
                    <div class="title-header ai-header">
                        AI全能版
                    </div>
                    <div class="card-body ai-body">
                        <ul class="feature-list">
                            <li v-for="(item, index) in aiFeatures" :key="index" style="margin-top: 8px;">
                                <span class="dot ai-dot" v-if="index!==6"></span>
                                <span class="text" v-html="item" :style="{paddingLeft:index!==6?'0':'18px'}"></span>
                            </li>
                        </ul>
                        <div class="card-footer">
                            <div class="btn-wrap">
                                <afp-button class="action-button ai-button"
                                            @click="handleSelectVersion(VERSION_TYPE.AGENT)">
                                    点我选择AI全能版
                                </afp-button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>

import {VERSION_TYPE} from "@/enum";

export default {
    data() {
        return {
            pureFeatures: [
                '<span>自动录制抖音和快手</span>',
                '<span>可以录制在线情况</span>',
                '<span>可提取录屏文案</span>',
                '<span>蓝光、超清均可切换</span>',
                '<span>个性化设置直播间分段</span>',
                '<span>自动生成MP4文件</span>',
                '<span style="color:#1CA700">*适合初级运营和个人主播和剪辑</span>'
            ],
            aiFeatures: [
                '<span>包含所有纯录制版功能</span>',
                '<span>自动录制抖音、快手、视频号</span>',
                '<span>分钟级在线、销售、互动数据</span>',
                '<span>录制弹幕、福袋、在线情况</span>',
                '<span>企业排班、业绩数据看板</span>',
                '<span style="color:#853FFE">AI智能体：</span>',
                '<span>&lt一键诊断直播间&gt、&lt一键对比同行直播间&gt、&lt获取同行优质话术&gt、&lt解决直播间违规问题&gt、&lt拆解同行直播运营逻辑&gt</span>',
                '<span>数据切片功能、笔记、批注</span>',
                '<span>场观、停留、销售等全场数据</span>',
                '<span>监控爆款短视频、提取短视频文案</span>',
                '<span style="color:#853FFE">支持个性化定制AI智能体</span>',
                '<span style="color:#853FFE">提供风控专家人工服务</span>',
                '<span style="color:#444DFF">*适合高级运营、MCN和品牌用户</span>'
            ],
            VERSION_TYPE
        };
    },
    methods: {
        async handleSelectVersion(type) {
            try {
                const result = await this.$httpClient.setup.setClientVersionConfig({
                    clientVersion: type === VERSION_TYPE.AGENT ? 'replay' : 'record',
                    pageType: 1
                })

                if (result.code === 0) {
                    this.$store.commit('setVersionType', type);

                    this.$message.success(`已成功选择${type === VERSION_TYPE.PURE ? '纯录制版' : 'AI全能版'}！`);

                    this.$router.push({
                        path: "dataAnalysis", query: {
                            login: '1'
                        }
                    });
                    window.open('https://www.douyin.com/jingxuan/');
                }
            } catch (e) {

            }
        }
    }
};
</script>

<style scoped lang="scss">
// 变量定义
$color-pure-green: #18B622;
$color-ai-blue: #4759FF;
$color-text-main: #333;
$color-text-sub: #555;
$bg-gradient: linear-gradient(135deg, #e6f0fa 0%, #f4f8ff 50%, #eef5fc 100%);
$title-gradient: linear-gradient(to right, #6b8df8, #a87df2);

.pricing-container {
    min-height: 100vh;
    background: $bg-gradient;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;

    .logoImg {
        width: 95%;
        height: 56px;
        margin-top: 5px;
        display: inline-block;
        background-image: url("~@/assets/imgs/theme/icon.png");
        background-repeat: no-repeat;
        background-position: 14px -154px;
    }

    .main-title {
        font-size: 22px;
        text-align: center;
        color: transparent;
        background-image: $title-gradient;
        -webkit-background-clip: text;
        background-clip: text;
        margin-bottom: 16px;
        letter-spacing: 1px;
    }

    .pricing-card-box {
        background: #F4F9FF;
        border-radius: 24px;
        padding: 24px 190px;
        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.03), inset 0 0 0 1px rgba(255, 255, 255, 0.8);

        @media (max-width: 800px) {
            padding: 30px 20px;
        }
    }

    .pricing-wrapper {
        display: flex;
        flex-direction: row;
        gap: 40px;
        max-width: 900px;
        width: 100%;
        justify-content: center;

        @media (max-width: 800px) {
            flex-direction: column;
            align-items: center;
        }
    }

    .card {
        flex: 1;
        width: 308px;
        height: 593px;
        background-color: white;
        overflow: hidden;
        display: flex;
        flex-direction: column;
        position: relative;
        box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);

        .title-header {
            font-weight: 500;
            font-size: 18px;
            height: 56px;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #FFFFFF;
        }

        .card-body {
            flex: 1;
            padding: 30px 26px;
            display: flex;
            flex-direction: column;
            background-color: #ffffff;
            position: relative;
            z-index: 1;

            .feature-list {
                list-style: none;
                padding: 0;
                margin: 0;
                flex: 1;
                position: relative;
                z-index: 2;

                li {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    padding: 0;
                    margin-top: 16px;
                    color: $color-text-sub;

                    .dot {
                        width: 7px;
                        height: 7px;
                        border-radius: 50%;
                        flex-shrink: 0;
                    }

                    .text {
                        font-size: 15px;
                        color: $color-text-main;
                    }
                }
            }

            .card-footer {
                position: relative;
                padding-top: 0;
                z-index: 2;

                .suitability {
                    font-size: 13px;
                    text-align: center;
                    font-weight: 500;
                    padding: 0;
                    margin: 0 0 12px 0;
                }

                .btn-wrap {
                    text-align: center;
                    position: relative;
                    z-index: 5;

                    .action-button {
                        width: 210px;
                        height: 34px;
                        border: none;
                        border-radius: 30px;
                        padding: 0;
                        display: inline-flex;
                        justify-content: center;
                        align-items: center;
                        font-size: 16px;
                        font-weight: 500;
                        color: #ffffff !important;
                        transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
                        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

                        &:hover {
                            transform: translateY(-2px);
                            box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
                        }

                        &:active {
                            transform: translateY(0);
                        }
                    }
                }
            }
        }

        // 纯录制版特定样式
        &.pure-card {
            background-image: url('~@/assets/imgs/2_6_3/vs-p.png');
            background-size: cover;
            background-repeat: no-repeat;

            .card-body {
                background-color: transparent;
            }

            .pure-dot {
                background-color: $color-pure-green;
                box-shadow: 0 0 0 3px rgba($color-pure-green, 0.2);
            }

            .pure-suitability {
                color: $color-pure-green;
            }

            .pure-button {
                background-color: $color-pure-green !important;

                &:hover {
                    background-color: lighten($color-pure-green, 5%) !important;
                }
            }
        }

        // AI全能版特定样式
        &.ai-card {
            background-image: url('~@/assets/imgs/2_6_3/vs-a.png');
            background-size: cover;
            background-repeat: no-repeat;

            .card-body {
                background-color: transparent;
            }

            .ai-dot {
                background-color: $color-ai-blue;
                box-shadow: 0 0 0 3px rgba($color-ai-blue, 0.2);
            }

            .ai-suitability {
                color: $color-ai-blue;
            }

            .ai-button {
                background-color: $color-ai-blue !important;

                &:hover {
                    background-color: lighten($color-ai-blue, 5%) !important;
                }
            }
        }
    }
}
</style>
