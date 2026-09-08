// 下载文件

const downloadFile = (url, params, fileName = 'file.xls', methods = 'get', rType = 'blob') => {
    url = window.SITE_CONFIG['backApiURL'] + url
    if (methods == 'get') {
        url += '?'
        let len = Object.keys(params).length
        Object.keys(params).forEach((key, idx) => {
            if (idx == len - 1) {
                url += key + '=' + params[key]
            } else {
                url += key + '=' + params[key] + '&'
            }

        })

    }


    return new Promise((resolve, reject) => {
        var xhr = new XMLHttpRequest();
        xhr.open(
            methods,
            url,
            true
        );

        xhr.responseType = rType;
        xhr.setRequestHeader("Content-Type", "application/json;charset=utf-8");

        let token = localStorage.getItem("yw_token") ? localStorage.getItem("yw_token") : ''
        xhr.setRequestHeader("Token", token);

        xhr.send(JSON.stringify(params));
        xhr.onload = function () {
            if (this.status == 200) {
                var blob = this.response;
                var a = document.createElement("a");
                var url = window.URL.createObjectURL(blob);
                a.href = url;
                a.download = fileName;
                a.click();

                resolve({ status: 0, msg: '下载成功' })
            } else {
                reject({ status: 1, msg: '下载失败' })
            }
        };
    })
}





module.exports = downloadFile