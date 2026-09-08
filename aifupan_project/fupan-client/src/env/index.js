const env = {
    dev: window.SITE_CONFIG.env ==='development',
    prod: window.SITE_CONFIG.env === 'production',
    test: window.SITE_CONFIG.env === 'test',
    release: window.SITE_CONFIG.env === 'release',
    custom: window.SITE_CONFIG.env === 'custom',
    devLog: function(name,...arg){
        if(env.dev|| env.test){
            console.groupCollapsed(`-------------观测 ${name || ''} 数据-------------`)
            console.log('')
            if( Array.isArray(name)){
                console.table(name,...arg);
            }else if(Array.isArray(arg[0])){
                console.table(...arg);
            }else{
                console.log(...arg);
            }
            console.log('')
            console.log('--------------------------')
            console.groupEnd('')
        }
    }
}

export default env