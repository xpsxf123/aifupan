const {CleanWebpackPlugin} = require('clean-webpack-plugin');
const ipConfig = require('./ipConfig');
const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const webpack = require('webpack');
const { updateAutoWebVersion } = require('./scripts/updateWebVersion');

function getGitVersion() {
  const safeExec = (command) => {
    try {
      return execSync(command, { stdio: ['ignore', 'pipe', 'ignore'] }).toString().trim();
    } catch (_) {
      return '';
    }
  };

  const branch = safeExec('git rev-parse --abbrev-ref HEAD');
  const lastCommitTime = safeExec('git log -1 --format=%%ai');
  const lastPushTime = branch && branch !== 'HEAD'
    ? safeExec(`git log --pretty=format:%%ai -1 origin/${branch}`)
    : '';

  return {
    name: safeExec('git config user.name'),
    email: safeExec('git config user.email'),
    version: branch,
    lastPushTime,
    lastCommitTime,
    date: new Date().toLocaleString(),
  };
}

const publicPath = process.env.BUILD_TARGET === 'website' ? '/client' : './';
const { version, name, email, date, lastPushTime, lastCommitTime } = getGitVersion(); 
const _version = process.env.VUE_APP_VERSION  || version || '未打入版本号';
const { back, client, type} = ipConfig;
const RUN_ENV = process.env.RUN_ENV;
const isProd = RUN_ENV === 'production';
const isBuild = process.argv.includes('build');
updateAutoWebVersion(_version);

// 获取当前时间戳
const timestamp = new Date().getTime();

module.exports = {
    publicPath,
    transpileDependencies: [/vod-js-sdk-v6/, /@aws-sdk/, /@smithy/, /@aws[\\/]lambda-invoke-store/],
    chainWebpack: (config) => {
        config.plugin("html").tap((args) => {
            let v = isProd ? _version : `${_version}.${RUN_ENV}`;
            args[0].ENV = RUN_ENV;
            args[0].BUILD_TARGET = process.env.BUILD_TARGET;
            args[0].title = "爱复盘";
            args[0].BCAK_API_URL = back[RUN_ENV];
            args[0].CLIENT_API_URL = client[RUN_ENV];
            args[0].BAIDU_HM_SRC = isProd
                ? 'https://hm.baidu.com/hm.js?6594cda6c1d15518299f9a60b7bcd528'
                : 'https://hm.baidu.com/hm.js?8c65f1c927dc2191efc5795f9d485740';
            args[0].buildInfo = `
                当前环境: ${type[RUN_ENV]};
                版本号: ${v};
                ${!isProd?'':`
                时间: ${date};
                邮箱: ${email};
                姓名: ${name};
                最后提交时间: ${lastCommitTime};
                最后推送时间: ${lastPushTime};
                `}
            `;
            return args;
        });

        if (isBuild) {
            // 添加时间戳查询参数
            config.output.filename(`[name].[contenthash:12].js?v=${timestamp}`);
            config.output.chunkFilename(`[name].[contenthash:12].js?v=${timestamp}`);

            // 处理CSS文件
            config.plugin('extract-css').tap(args => [{
                filename: `[name].[contenthash:12].css?v=${timestamp}`,
                chunkFilename: `[name].[contenthash:12].css?v=${timestamp}`,
                ...args[0]
            }]);
        }
    },
    configureWebpack: {
        resolve: {
            alias: {
                '@aws-sdk/middleware-sdk-s3/s3': path.resolve(__dirname, 'src/utils/emptyAwsSdkMiddleware.js'),
                '@aws-sdk/middleware-sdk-s3/s3-control': path.resolve(__dirname, 'src/utils/emptyAwsSdkMiddleware.js'),
                'node:crypto': 'crypto',
                'node:http': 'http',
                'node:os': 'os',
                'node:path': 'path',
                'node:process': 'process',
                'node:stream': 'stream',
                'node:zlib': 'zlib',
                'node:fs': path.resolve(__dirname, 'src/utils/emptyNodeModule.js'),
                'node:fs/promises': path.resolve(__dirname, 'src/utils/emptyNodeModule.js'),
                'node:async_hooks': path.resolve(__dirname, 'src/utils/emptyNodeModule.js')
            }
        },
        optimization: {
            runtimeChunk: 'single',
            splitChunks: {
                chunks: 'all',
            },
        },
        plugins: [
            new CleanWebpackPlugin(),
            new webpack.NormalModuleReplacementPlugin(/^node:fs\/promises$/, path.resolve(__dirname, 'src/utils/emptyNodeModule.js')),
            new webpack.NormalModuleReplacementPlugin(/^node:async_hooks$/, path.resolve(__dirname, 'src/utils/emptyNodeModule.js')),
            new webpack.DefinePlugin({
                'process.env.VUE_APP_LA_ID': JSON.stringify(
                    process.env.RUN_ENV === 'production'
                        ? '3PYCuCj70Rn9AEnZ' // 正式环境 ID
                        : '3PYDdzadFG2l33yw' // 测试/开发环境 ID
                ),
                'process.env.VUE_APP_LA_CK': JSON.stringify(
                    process.env.RUN_ENV === 'production'
                        ? '3PYCuCj70Rn9AEnZ' // 正式环境 CK
                        : '3PYDdzadFG2l33yw' // 测试/开发环境 CK
                )
            })
        ]
    },
    // 另一种CSS处理方式
    css: isBuild ? {
        extract: {
            filename: `[name].[contenthash:12].css?v=${timestamp}`,
            chunkFilename: `[name].[contenthash:12].css?v=${timestamp}`
        }
    } : {}
};
