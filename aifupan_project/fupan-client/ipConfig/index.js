module.exports= {
    // window.SITE_CONFIG['backApiURL'] = 'https://api.aifupan.com.cn/replay';
    // window.SITE_CONFIG['backApiURL'] = 'https://testapi.aifupan.com.cn/replay';
    // 客户端请求地址
    // window.SITE_CONFIG['clientApiURL'] = 'http://127.0.0.1:5001/api';
    back:{
        test: 'https://testapi.aifupan.com.cn/replay',
        development: 'https://testapi.aifupan.com.cn/replay',
        production: 'https://api.aifupan.com.cn/replay',
        release: 'https://api-yz.aifupan.com.cn/replay',
        custom:'https://testapi.aifupan.com.cn/replay',
    },
    back2:{
        test: 'https://testapi.aifupan.com.cn/api',
        development: 'https://testapi.aifupan.com.cn/api',
        production: 'https://api.aifupan.com.cn/api',
        release: 'https://api-yz.aifupan.com.cn/api',
        custom:'',
    },
    //可能会失效，目前端口由客户端返回
    client: {
        test: 'http://127.0.0.1:10086/api',
        development: 'http://127.0.0.1:10086/api',
        production: 'http://127.0.0.1:10086/api',
        release: 'http://127.0.0.1:10086/api',
        custom:'http://127.0.0.1:10086/api',
    },
    type: {
        test: '测试',
        development: '开发',
        production: '生产',
        release: '预发布',
        custom:'自定义',
    },
    platform_code:{
        test: {
            client: 'aDjPSE8ySm',
            cloudSpace: 'beNjdKZbks',
            official: 'EaFeJzBwfI'
        },
        development: {
            client: 'aDjPSE8ySm',
            cloudSpace: 'beNjdKZbks',
            official: 'EaFeJzBwfI'
        },
        release: {
            client: 'sE3aMz4In7',
            cloudSpace: 'erl5WDRvFL',
            official: 'xUyLqPsVPt'
        },
        production: {
            client: 'vf406vwooE',
            cloudSpace: 'm0xW7Qx35M',
            official: 'wVkGMiAgl8'
        }
    },
    signatureConfig:{
        test:{
            appId:'replay_test',
            signature:{
                _k: {
                    _a: "MlhNcnNpMXo0Ug==",
                    _b: "ZjZvMGlYQ2NNTg==",
                    _c: "TXhoaEc5czNEeTNo"
                }
            }
        },
        development:{
            appId:'replay_test',
            signature:{
                _k: {
                    _a: "MlhNcnNpMXo0Ug==",
                    _b: "ZjZvMGlYQ2NNTg==",
                    _c: "TXhoaEc5czNEeTNo"
                }
            }
        },
        production:{
            appId:'replay_prod',
            signature:{
                _k: {
                    _a: "aWdGSHBtdUxLWVk=",
                    _b: "UWp3RFZrejRZVGI=",
                    _c: "UmN1enZ3UVpmWQ=="
                }
            }
        },
        release: {
            appId:'replay_yz',
            signature:{
                _k: {
                    _a: "a1VrZDBHQ2U2Zg==",
                    _b: "ejhBOFVnQ2dSRA==",
                    _c: "UWt4enpac2U2MnZF"
                }
            }
        },
    }
}
