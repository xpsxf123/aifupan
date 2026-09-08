
import re
import requests
import json
import time
from urllib.parse import unquote

class Spider(object):
    def __init__(self):
        self.headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0",
            # "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
            # "Accept-Encoding": "gzip, deflate, br, zstd",
            "sec-ch-ua": "\"Microsoft Edge\";v=\"137\", \"Chromium\";v=\"137\", \"Not/A)Brand\";v=\"24\"",
            "sec-ch-ua-mobile": "?0",
            "sec-ch-ua-platform": "\"Windows\"",
            "upgrade-insecure-requests": "1",
            "sec-fetch-site": "none",
            "sec-fetch-mode": "navigate",
            "sec-fetch-user": "?1",
            "sec-fetch-dest": "document",
            "accept-language": "zh-CN,zh;q=0.9",
            "priority": "u=0, i"
        }

    def get_ac_signature(self, ua, __ac_nonce):
        chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-./'

        def encrypt(hash_val, string):
            for ch in string:
                hash_val = ((hash_val ^ ord(ch)) * 65599) & 0xffffffff
            return hash_val

        def encrypt2(val, string):
            for ch in string:
                val = ((val * 65599 + ord(ch)) & 0xffffffff)
            return val

        def encrypt3(num):
            res = ''
            for i in range(4, -1, -1):
                res += chars[(num >> (i * 6)) & 63]
            return res

        timestamp_int = (int(time.time() * 1000) // 1000)
        timestamp_str = str(timestamp_int)
        timestamp_number = encrypt(0, timestamp_str)
        url_number = encrypt(timestamp_number, 'www.douyin.com')
        a1 = (timestamp_int ^ ((url_number % 65521) * 65521)) & 0xffffffff
        b_a1 = bin(a1)[2:]
        b_a2 = "10000000110000" + "0" * (32 - len(b_a1)) + b_a1
        a2 = int(b_a2, 2)
        a2_str = str(a2)
        a2_number = encrypt(0, a2_str)
        str1 = encrypt3(a2 >> 2)
        r1 = (a2 // 4294967296) & 0xffffffff
        r2 = ((a2 << 28) | (r1 >> 4)) & 0xffffffff
        str2 = encrypt3(r2)
        r3 = (2010578131 ^ a2)
        r4 = ((r1 << 26) | (r3 >> 6)) & 0xffffffff
        str3 = encrypt3(r4)
        str3_last = chars[r3 & 63]
        ua_number = encrypt(a2_number, ua)
        ac_nonce_number = encrypt(a2_number, __ac_nonce)
        r5 = ((ua_number % 65521) << 16) & 0xffffffff
        r6 = (r5 | (ac_nonce_number % 65521)) & 0xffffffff
        str4 = encrypt3(r6 >> 2)
        r7 = ((r6 << 28) | ((524576 ^ a2) >> 4)) & 0xffffffff
        str5 = encrypt3(r7)
        str6 = encrypt3(url_number % 65521)
        str7 = "_02B4Z6wo00f01" + str1 + str2 + str3 + str3_last + str4 + str5 + str6
        str8 = format(encrypt2(0, str7) & 0xffffffff, '08x')[-2:]
        __ac_signature = str7 + str8
        return __ac_signature

    def get_cookies(self, sec_uid):
        url = f"https://www.douyin.com/user/{sec_uid}"
        self.headers['referer'] = url

        # 第一次请求
        response = requests.get(url, headers=self.headers)
        cookies = response.cookies.get_dict()
        __ac_nonce = cookies.get('__ac_nonce')
        ua = self.headers['User-Agent']
        __ac_signature = self.get_ac_signature(ua, __ac_nonce)
        cookies.update({
            '__ac_signature': __ac_signature,
            '__ac_referer': '__ac_blank'
        })
        return cookies

    def parse_html(self, html_text):
        '''
        判断是否在直播
        :return:
        '''
        item = {}
        nickname = re.search(r'\\"nickname\\":\\"(.*?)\\"', html_text)
        if nickname:
            item['nickname'] = nickname.group(1)

        uniqueId = re.search(r'\\"uniqueId\\":\\"(.*?)\\"', html_text)
        if uniqueId:
            item['uniqueId'] = uniqueId.group(1)

        secUid = re.search(r'\\"secUid\\":\\"(.*?)\\"', html_text)
        if secUid:
            item['secUid'] = secUid.group(1)

        desc = re.search(r'\\"nickname\\":.*?\\"desc\\":\\"(.*?)\\",\\"descExtra', html_text)
        if desc:
            item['desc'] = desc.group(1)

        labelStyle = re.search(r'\\"labelStyle\\":(\d+)', html_text)
        if labelStyle:
            labelStyle_number = int(labelStyle.group(1))
            labelStyle_map = {
                7: '红V',
                5: '蓝V',
                3: '黄V',
            }
            item['labelStyle'] = labelStyle_map[labelStyle_number]

        uid = re.search(r'\\"uid\\":\\"(.*?)\\"', html_text)
        if uid:
            item['uid'] = uid.group(1)

        if "user-info-living" in html_text and "直播中" in html_text:
            item['live_satus'] = 1 # 正在直播

            web_room_id = re.search(r'"web_rid\\":\\"(.*?)\\"', html_text)
            if web_room_id:
                item['web_room_id'] = web_room_id.group(1)

            app_room_id = re.search(r'"roomIdStr\\":\\"(.*?)\\"', html_text)
            if app_room_id:
                item['app_room_id'] = app_room_id.group(1)

            # 定义正则表达式
            pattern = r'http://pull[^"]*?douyincdn\.com[^"]*?\.m3u8?[^"]*?",'

            pull_url_list = []
            # 匹配所有链接
            matches = re.findall(pattern, html_text)
            if matches:
                for match in matches:
                    pull_url = match[:-3]
                    pull_url_list.append(pull_url)
            else:

                pattern = r'http://pull[^"]*?douyinliving\.com[^"]*?\.m3u8?[^"]*?",'
                matches = re.findall(pattern, html_text)
                if matches:
                    for match in matches:
                        pull_url = match[:-3]
                        print(pull_url)
                        pull_url_list.append(pull_url)

            item['pull_url_list'] = pull_url_list





        else:
            item['live_satus'] = '未开播'

        return item

    def get_user_info(self, sec_uid, cookies):
        url = f"https://www.douyin.com/user/{sec_uid}"
        self.headers['referer'] = url

        # 第二次请求
        response = requests.get(url, headers=self.headers, cookies=cookies)
        html_text = unquote(response.text).replace('\\u0026', '&')
        return html_text

    def main(self, sec_uid):
        # 先初始化cookie
        cookies = self.get_cookies(sec_uid)
        # print(cookies)

        html_text = self.get_user_info(sec_uid, cookies)
        item = self.parse_html(html_text)
        print(item)


if __name__ == '__main__':

    # sec_uid = "MS4wLjABAAAA2BUeKYcvTIS9FHwvR8e3To8wpW_mIZHu41WXhZfTFWs"
    sec_uid = "MS4wLjABAAAA8U_l6rBzmy7bcy6xOJel4v0RzoR_wfAubGPeJimN__4"
    s = Spider()
    s.main(sec_uid)

