import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import path from 'path'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons' // vite-plugin-svg-icons 用于自动导入 svg 图标
const printInfo = (env, type) => {
    console.log(`\n================= ${type}信息 =================`)
    console.log(`✅ 当前${type}环境 : ${env.VITE_ENV_NAME}`)
    console.log(`✅ 接口基础地址       : ${env.VITE_BASE_URL}`)
    console.log(`✅ Node.js 版本   : ${process.version}`)
    console.log(`✅ 时间           : ${new Date().toLocaleString()}`)
    console.log('============================================\n')
}
export default defineConfig(({command, mode}) => {
    const env = loadEnv(mode, process.cwd())
    printInfo(env, command === 'serve' ? '运行' : '构建')
    return {
        plugins: [
            vue(),
            vueDevTools(),
            AutoImport({
                resolvers: [ElementPlusResolver()]
            }),
            Components({
                resolvers: [ElementPlusResolver()]
            }),
            createSvgIconsPlugin({
                // 需要自动导入的 svg 文件目录（可自行修改）
                iconDirs: [path.resolve(process.cwd(), 'src/assets/icons')],
                // 执行icon name的格式（可自行修改）
                symbolId: 'icon-[dir]-[name]'
                // 更多配置请参考：https://github.com/vbenjs/vite-plugin-svg-icons/blob/HEAD/README.zh_CN.md
            })
        ],
        resolve: {
            alias: {
                '@': fileURLToPath(new URL('./src', import.meta.url))
            }
        },
        css: {
            // css预处理器
            preprocessorOptions: {
                scss: {
                    javascriptEnabled: true,
                    additionalData: `@use "@/assets/scss/systemVariable" as *;`
                }
            }
        }
    }
})
