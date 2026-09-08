// 任务收集容器：action -> { alias: handler }
// 同一 action 下可挂载多个监听（funMap），key 为别名，value 为处理函数
const TaskCollection = {};

// 默认别名：未显式传入 alias 时使用，兼容旧写法（单监听覆盖 def）
const DEFAULT_ALIAS = 'def';

/**
 * @description 注册一个 C# 推送任务监听（支持同一 action 多监听）
 * @param {string} nameOrCode 推送指令 action 名称
 * @param {Function} callback 成功回调
 * @param {Function} [errCallback] 失败回调
 * @param {string} [alias] 多注册别名（funMap 的 key），用于保证同一 action 下多个监听互不覆盖；不传则默认使用 def
 */
const addTask = (nameOrCode, callback, errCallback, alias) => {
    if (typeof callback !== 'function') return;
    const key = alias || DEFAULT_ALIAS;
    const handler = (requestData, resFn, rejFn) => {
        let data = typeof requestData?.data === 'string'
            ? JSON.parse(requestData?.data)
            : (requestData?.data || {});
        if (requestData.code === 0 && requestData.status === 200) {
            return callback(data, resFn);
        }
        if (typeof errCallback === 'function') {
            return errCallback(requestData, rejFn);
        }
    };
    if (!TaskCollection[nameOrCode]) {
        TaskCollection[nameOrCode] = {};
    }
    // 使用对象(而非数组)存储，重复注册同一 alias 会覆盖而非叠加，避免永久监听被重复添加
    TaskCollection[nameOrCode][key] = handler;
};

/**
 * @description 移除 C# 推送任务监听
 * @param {string} nameOrCode 推送指令 action 名称
 * @param {string} [alias] 多注册别名；不传时删除该 action 下所有监听，传了则仅删除对应 alias 的监听
 */
const removeTask = (nameOrCode, alias) => {
    const funMap = TaskCollection[nameOrCode];
    if (!funMap) return;
    if (alias) {
        delete funMap[alias];
        // 该 action 下已无监听时，清理空对象
        if (Object.keys(funMap).length === 0) {
            delete TaskCollection[nameOrCode];
        }
    } else {
        delete TaskCollection[nameOrCode];
    }
};

const logTask = function () {
    console.log('logTask');
    console.log(TaskCollection);
};

// 注册到全局
// C#通知功能
window.notifyFromCSharp = function (requestData) {
    // console.log("C#通知:"+ requestData);
    try {
        requestData = JSON.parse(requestData);
    } catch (e) {
        console.error(e);
    }

    /*
    {
        "code":0,
        "status":200,
        "action":"autoUploadCloud",
        "data":{
            "filePath":"D:\\workSpace\\visualStudio\\ReviewAnalysis-new\\bin\\Debug\\download\\东方甄选\\20250208\\mp4\\东方甄选_2025年02月08日15时47分00秒_第1段.ts.mp4",
            "fileSize":21,
            "duration":83.201,
            "videId":"ef1f2705-0d5d-42a1-beab-f0293e2bef24"
        }
    }
    */
    console.log("C#通知:", requestData);

    return new Promise((resolve, reject) => {
        const reqResolve = (o) => {
            // 返回值
            if (o) {
                if (typeof o.code === 'undefined') {
                    // 返回C#的值，检查是否有code
                    return resolve(JSON.stringify({ code: 0, data: o }));
                } else {
                    // 返回C#的值
                    return resolve(JSON.stringify(o));
                }
            } else {
                console.error('对应任务没有指定返回值');
            }
        };
        const reqReject = (o) => {
            // 返回C#的值
            return reject(JSON.stringify(o));
        };
        // 执行任务：遍历该 action 下 funMap 的所有处理函数，按注册顺序执行
        const funMap = TaskCollection[requestData?.action];
        if (funMap) {
            Object.values(funMap).forEach((task) => {
                task?.(requestData, reqResolve, reqReject);
            });
        } else {
            console.error('未找到对应任务', requestData?.action);
        }
    });
};

export default {
    allTask: TaskCollection,
    addTask,
    removeTask,
    logTask
};
