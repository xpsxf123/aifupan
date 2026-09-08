


import requests
import json

from urllib.parse import quote

sessionid = 'ee362944ff59baa7064cd8c1552c33c5'

def search_daren(keyword):
    headers = {
        "accept": "application/json, text/plain, */*",
        "accept-language": "zh-CN,zh;q=0.9",
        "appsource": "PC",
        "cache-control": "no-cache",
        "content-type": "application/json",
        "origin": "https://trendinsight.oceanengine.com",
        "pragma": "no-cache",
        "priority": "u=1, i",
        "referer": f"https://trendinsight.oceanengine.com/arithmetic-index/daren/search?keyword={quote(keyword)}",
        "sec-ch-ua": "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\"",
        "sec-ch-ua-mobile": "?0",
        "sec-ch-ua-platform": "\"Windows\"",
        "sec-fetch-dest": "empty",
        "sec-fetch-mode": "cors",
        "sec-fetch-site": "same-origin",
        "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
        "x-secsdk-csrf-token": "DOWNGRADE"
    }


    cookies = {
        "sessionid_count": sessionid
    }
    url = "https://trendinsight.oceanengine.com/api/v2/daren/get_sug_great_user_list"

    # params = {
    #     "msToken": "qj2-veHt28vlgyrjbICuax8pG9W6149_fLsVVRNNLf9NGjEbGwCJyDoCy8mj69hZC7hcd1dOgxwkfXTQLxy4D2Dg1bRmXPaNqMDBAmL70ZUSEx23zPxTXmPYLvtRbhY=",
    #     "X-Bogus": "DFSzswVLtEuVASXFCSJFkpYhjsNt",
    #     "_signature": "_02B4Z6wo00001q40omgAAIDBMFAC-5GxX56uNKbAAMQB8YCk--.jSJlcM8bCaU4Z2cC0PBNSj8.yn4pWxsoel0gPn-1LMbxmvX.ezDI5JOP0NKN3NGMxReJ-Exx5Li0XwMMYyLE51mlXd0Pq7f"
    # }
    data = {
        "total": "30",
        "keyword": keyword
    }
    data = json.dumps(data, separators=(',', ':'))
    response = requests.post(url, headers=headers, cookies=cookies, data=data)


    print(response)
    if len(response.text) > 1000:
        json_data = response.json()
        if json_data.get('status') == 0:
            for i in json_data['data']['userlist']:
                print(i)

    else:
        print(response.text)


# keyword: 达人名称或者抖音号
search_daren('sktb019')



