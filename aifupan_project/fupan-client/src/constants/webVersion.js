/**
 * @file Web 版本号常量。
 * @description 用于给所有接口请求统一携带 webVersion（在 request-back/request-client 中读取）。
 */

/**
 * 自动版本号（只读）。
 * @description 由构建脚本在每次运行/打包时从 git 分支名中解析并自动写入。
 * 命名约定示例：xxx-2.5.8-xxx / release-2.5.8 / dev-2.5.8-h5，最终提取 2.5.8。
 *
 * 注意：
 * - 当构建时无法获取到 git 版本号，构建脚本不会改动此字段，避免覆盖你已有的版本号。
 * - 请勿手动修改该字段，避免与构建脚本产生冲突。
 */
export const AUTO_WEB_VERSION = "2.6.2.4";

/**
 * 手动版本号（可编辑）。
 * @description 当你需要临时指定版本号时，填写这里（仅支持数字与小数点）。
 * 版本选择逻辑在 src/utils/webVersion.js：默认优先自动版本；若手动版本更大则使用手动版本。
 *
 * 注意：构建脚本更新 AUTO_WEB_VERSION 时不会修改该字段。
 */
export const MANUAL_WEB_VERSION = "2.5.8.8";

