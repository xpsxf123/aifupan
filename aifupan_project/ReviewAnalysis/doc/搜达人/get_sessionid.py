

import requests
import time
import json
import random

class Spider(object):
    def __init__(self):
        self.account = 'dengdingnan@jiuyujiaoyu.com'  # 邮箱
        self.password = 'Aa12345678'  # 密码

    def encrypt(self, e):
        if e is None:
            return ""
        # 将字符串转换为UTF-8字节列表（对应JavaScript中的字符编码处理）
        t = []
        for char in e:
            code = ord(char)
            if 0 <= code <= 127:
                t.append(code)
            elif 128 <= code <= 2047:
                t.append(192 | ((code >> 6) & 31))
                t.append(128 | (code & 63))
            else:
                if (2048 <= code <= 55295) or (57344 <= code <= 65535):
                    t.append(224 | ((code >> 12) & 15))
                    t.append(128 | ((code >> 6) & 63))
                    t.append(128 | (code & 63))

        # 对每个字节执行异或操作并转换为十六进制字符串
        n = []
        for byte in t:
            byte &= 255  # 确保字节范围在0-255
            n.append("{0:02x}".format(5 ^ byte))
        return ''.join(n)

    # 生成fp
    def get_fp(self):
        chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        e = list(chars)
        t = len(e)
        n = str(int(time.time()))  # 当前时间戳转为字符串

        # 使用 Date.now().toString(36) 的模拟，将时间戳转为 base36
        def base36encode(number):
            return format(number, 'x')  # 这里简化处理，Python 中使用 hex 类似于 JS 的 toString(36)

        n = base36encode(int(time.time()))

        r = [''] * 36
        r[8] = r[13] = r[18] = r[23] = "_"
        r[14] = "4"

        for o in range(36):
            if not r[o]:
                i = int(random.random() * t)
                if o == 19:
                    r[o] = e[(3 & i) | 8]
                else:
                    r[o] = e[i]
        fp = "verify_" + n + "_" + "".join(r)
        # print(f"生成的fp: {fp}")
        return fp

    # 登录
    def account_login(self):
        headers = {
            'accept': 'application/json, text/plain, */*',
            'accept-language': 'zh-CN,zh;q=0.9',
            'cache-control': 'no-cache',
            'content-type': 'application/x-www-form-urlencoded',
            'origin': 'https://www.lifexue.com',
            'pragma': 'no-cache',
            'priority': 'u=1, i',
            'referer': 'https://www.lifexue.com/',
            'sec-ch-ua': '"Not)A;Brand";v="8", "Chromium";v="138", "Google Chrome";v="138"',
            'sec-ch-ua-mobile': '?0',
            'sec-ch-ua-platform': '"Windows"',
            'sec-fetch-dest': 'empty',
            'sec-fetch-mode': 'cors',
            'sec-fetch-site': 'cross-site',
            'sec-fetch-storage-access': 'none',
            'user-agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36',
            'x-requested-with': 'XMLHttpRequest',
            'x-tt-passport-csrf-token': '',
        }

        # params = {
        #     'msToken': '',
        #     'X-Bogus': 'DFSzswVLQDGJ3SXFCtLY4rYhjsEI',
        #     '_signature': '_02B4Z6wo00001LvAw9gAAIDDJaRjSkc0gJC7wMdAAEaipojb1HFJXhAdoeY3d9msEYvVHZPhvyX0soU5SGqBUKvuW0kLpG5t2b-9fseWGJFSWjHO.mrmFt2SvxgVHo-I-sjsxYk2JmrCTKO-4c',
        # }

        data = {
            'fp': self.get_fp(),
            'aid': '405096',
            'language': 'zh',
            'account_sdk_source': 'web',
            'mix_mode': '1',
            'service': 'https://www.lifexue.com/',
            'account': self.encrypt(self.account),
            'password': self.encrypt(self.password),
            'captcha_key': '',
        }
        # data = json.dumps(data, separators=(',', ':'))

        response = requests.post('https://sso.oceanengine.com/account_login/v2/', headers=headers, data=data)
        json_data = response.json()
        if json_data['error_code'] == 0:
            user_id = json_data.get('user_id')
            print(f"学号user_id: {user_id}")
            redirect_url = json_data.get('redirect_url')
            print(f"回调接口：{redirect_url}")
            self.login_callback(redirect_url)

        else:
            print(response.text)

    # 登录回调返回cookie
    def login_callback(self, redirect_url):
        headers = {
            'accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7',
            'accept-language': 'zh-CN,zh;q=0.9',
            'cache-control': 'no-cache',
            'pragma': 'no-cache',
            'priority': 'u=0, i',
            'referer': 'https://www.lifexue.com/',
            'sec-ch-ua': '"Not)A;Brand";v="8", "Chromium";v="138", "Google Chrome";v="138"',
            'sec-ch-ua-mobile': '?0',
            'sec-ch-ua-platform': '"Windows"',
            'sec-fetch-dest': 'document',
            'sec-fetch-mode': 'navigate',
            'sec-fetch-site': 'same-origin',
            'sec-fetch-user': '?1',
            'upgrade-insecure-requests': '1',
            'user-agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36',
            # 'cookie': 'x-web-secsdk-uid=a68bc8e5-5c7f-4a1d-bcbc-03e63c69d965; s_v_web_id=verify_mcvetxvk_7BtqVCX7_9EnG_4D25_9okr_rVcw8EWnmYbW; csrf_session_id=00e9b3ae0fe0cc1b8c3aaaa2405175cb; ttcid=c089858b27ab4986912d7e4a8fced5e231; tt_scid=YdEoFh-n-24DSvwWuhq0rZfmOlT3PqUI3AaWklSWSzZID4f9nfl3r2cN01AUDHTb70e6',
        }
        response = requests.get(redirect_url, headers=headers, allow_redirects=False)
        print(response)
        response_cookie = response.cookies.get_dict()
        sessionid = response_cookie.get('sessionid')
        print(f'sessionid: {sessionid}')



if __name__ == '__main__':
    s = Spider()
    s.account_login()

