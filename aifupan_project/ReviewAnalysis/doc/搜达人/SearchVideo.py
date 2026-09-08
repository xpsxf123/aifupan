


import time
import json
import copy
import uuid
import secrets
import jmespath
import requests
from datetime import datetime
from urllib.parse import quote, unquote



class Spider(object):
    def __init__(self):

        self.keyword = '旺仔小乔'
        # self.keyword = '英雄联盟'
        self.url = f"https://www.douyin.com/search/{quote(self.keyword)}?type=video"

        # 4个浏览器环境
        self.browser_obj = {
            'sougou': {
                'ck_headers1': {
                    "authority": "www.douyin.com",
                    "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                    "accept-language": "zh-CN,zh;q=0.9",
                    "cache-control": "no-cache",
                    "pragma": "no-cache",
                    "sec-ch-ua": "\"Not)A;Brand\";v=\"24\", \"Chromium\";v=\"116\"",
                    "sec-ch-ua-mobile": "?0",
                    "sec-ch-ua-platform": "\"Windows\"",
                    "sec-fetch-dest": "document",
                    "sec-fetch-mode": "navigate",
                    "sec-fetch-site": "none",
                    "sec-fetch-user": "?1",
                    "upgrade-insecure-requests": "1",
                    "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.5845.97 Safari/537.36 SE 2.X MetaSr 1.0"
                },
                'ck_headers2': {
                    "authority": "www.douyin.com",
                    "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                    "accept-language": "zh-CN,zh;q=0.9",
                    "cache-control": "no-cache",
                    "pragma": "no-cache",
                    "referer": "https://www.douyin.com/search/%E6%97%BA%E4%BB%94%E5%B0%8F%E4%B9%94?type=video",
                    "sec-ch-ua": "\"Not)A;Brand\";v=\"24\", \"Chromium\";v=\"116\"",
                    "sec-ch-ua-mobile": "?0",
                    "sec-ch-ua-platform": "\"Windows\"",
                    "sec-fetch-dest": "document",
                    "sec-fetch-mode": "navigate",
                    "sec-fetch-site": "same-origin",
                    "upgrade-insecure-requests": "1",
                    "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.5845.97 Safari/537.36 SE 2.X MetaSr 1.0"
                },
                'headers': {
                    "authority": "www.douyin.com",
                    "accept": "application/json, text/plain, */*",
                    "accept-language": "zh-CN,zh;q=0.9",
                    "cache-control": "no-cache",
                    "pragma": "no-cache",
                    "referer": "https://www.douyin.com/search/%E6%97%BA%E4%BB%94%E5%B0%8F%E4%B9%94?type=video",
                    "sec-ch-ua": "\"Not)A;Brand\";v=\"24\", \"Chromium\";v=\"116\"",
                    "sec-ch-ua-mobile": "?0",
                    "sec-ch-ua-platform": "\"Windows\"",
                    "sec-fetch-dest": "empty",
                    "sec-fetch-mode": "cors",
                    "sec-fetch-site": "same-origin",
                    "uifid": "undefined",
                    "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.5845.97 Safari/537.36 SE 2.X MetaSr 1.0"
                },
                'params': {
                    "device_platform": "webapp",
                    "aid": "6383",
                    "channel": "channel_pc_web",
                    "search_channel": "aweme_video_web",
                    "enable_history": "1",
                    "keyword": "旺仔小乔",
                    "search_source": "normal_search",
                    "query_correct_type": "1",
                    "is_filter_search": "0",
                    "from_group_id": "",
                    "offset": "0",
                    "count": "10",
                    "need_filter_settings": "1",
                    "list_type": "single",
                    "update_version_code": "170400",
                    "pc_client_type": "1",
                    "pc_libra_divert": "Windows",
                    "support_h265": "1",
                    "support_dash": "0",
                    "cpu_core_num": "12",
                    "version_code": "170400",
                    "version_name": "17.4.0",
                    "cookie_enabled": "true",
                    "screen_width": "1920",
                    "screen_height": "1080",
                    "browser_language": "zh-CN",
                    "browser_platform": "Win32",
                    "browser_name": "Sogou Explorer",
                    "browser_version": "1.0",
                    "browser_online": "true",
                    "engine_name": "Blink",
                    "engine_version": "116.0.5845.97",
                    "os_name": "Windows",
                    "os_version": "10",
                    "device_memory": "8",
                    "platform": "PC",
                    "downlink": "10",
                    "effective_type": "4g",
                    "round_trip_time": "50",
                    # "webid": "7543453104595977769",
                    # "a_bogus": "d7UfkFW7QxQjOdFS8KaIy45Utq6/rsuyWTTdS73UtNqncZeTmbPbLxtfaxwVBXE8kmpihH379DzAYDVczstiZHrpqmpkug4bq0Q59uXL21r2YGigJr8hCTbxFJTnUWGYO/C3i5E1W0zwIoc3hrnOA5VaH5zOQOYDRrqjdMmcc9WNdSjH9oQ5eBvWM1f="
                },
                'cookies': {
                    "__ac_nonce": "068afbc9100f28561883f",
                    "__ac_signature": "_02B4Z6wo00f012bojsAAAIDCBeJOqdmF0o9myIpAALEgc4",
                    "SEARCH_RESULT_LIST_TYPE": "%22single%22",
                    "ttwid": "1%7CPNlY_HP9xJmS4I8p20J14S2ae_LMGg2_YqnrI89FU78%7C1756347538%7Cb4bfb4b006fc05220a7542a415450e625f58b7269ba48b5dcafcacc5ac27bac1",
                    "x-web-secsdk-uid": "372ff6d0-1715-4755-9eaa-bfaaa811822b",
                    "home_can_add_dy_2_desktop": "%220%22",
                    "csrf_session_id": "00e9b3ae0fe0cc1b8c3aaaa2405175cb",
                    "hevc_supported": "true"
                }
            },
            'edge': {
                'ck_headers1': {
                    "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                    "accept-language": "zh-CN,zh;q=0.9",
                    "cache-control": "no-cache",
                    "pragma": "no-cache",
                    "priority": "u=0, i",
                    "sec-ch-ua": "\"Chromium\";v=\"140\", \"Not=A?Brand\";v=\"24\", \"Microsoft Edge\";v=\"140\"",
                    "sec-ch-ua-mobile": "?0",
                    "sec-ch-ua-platform": "\"Windows\"",
                    "sec-fetch-dest": "document",
                    "sec-fetch-mode": "navigate",
                    "sec-fetch-site": "none",
                    "sec-fetch-user": "?1",
                    "upgrade-insecure-requests": "1",
                    "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36 Edg/140.0.0.0"
                },
                'ck_headers2': {
                    "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                    "accept-language": "zh-CN,zh;q=0.9",
                    "cache-control": "no-cache",
                    "pragma": "no-cache",
                    "priority": "u=0, i",
                    "referer": "https://www.douyin.com/search/%E6%97%BA%E4%BB%94%E5%B0%8F%E6%A1%A5?type=video",
                    "sec-ch-ua": "\"Chromium\";v=\"140\", \"Not=A?Brand\";v=\"24\", \"Microsoft Edge\";v=\"140\"",
                    "sec-ch-ua-mobile": "?0",
                    "sec-ch-ua-platform": "\"Windows\"",
                    "sec-fetch-dest": "document",
                    "sec-fetch-mode": "navigate",
                    "sec-fetch-site": "same-origin",
                    "upgrade-insecure-requests": "1",
                    "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36 Edg/140.0.0.0"
                },
                'headers': {
                    "accept": "application/json, text/plain, */*",
                    "accept-language": "zh-CN,zh;q=0.9",
                    "cache-control": "no-cache",
                    "pragma": "no-cache",
                    "priority": "u=1, i",
                    "referer": "https://www.douyin.com/search/%E6%97%BA%E4%BB%94%E5%B0%8F%E6%A1%A5?type=video",
                    "sec-ch-ua": "\"Chromium\";v=\"140\", \"Not=A?Brand\";v=\"24\", \"Microsoft Edge\";v=\"140\"",
                    "sec-ch-ua-mobile": "?0",
                    "sec-ch-ua-platform": "\"Windows\"",
                    "sec-fetch-dest": "empty",
                    "sec-fetch-mode": "cors",
                    "sec-fetch-site": "same-origin",
                    "uifid": "undefined",
                    "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36 Edg/140.0.0.0"
                },
                'params': {
                    "device_platform": "webapp",
                    "aid": "6383",
                    "channel": "channel_pc_web",
                    "search_channel": "aweme_video_web",
                    "enable_history": "1",
                    "keyword": "旺仔小桥",
                    "search_source": "normal_search",
                    "query_correct_type": "1",
                    "is_filter_search": "0",
                    "from_group_id": "",
                    "offset": "0",
                    "count": "10",
                    "need_filter_settings": "1",
                    "list_type": "single",
                    "update_version_code": "170400",
                    "pc_client_type": "1",
                    "pc_libra_divert": "Windows",
                    "support_h265": "0",
                    "support_dash": "0",
                    "cpu_core_num": "8",
                    "version_code": "170400",
                    "version_name": "17.4.0",
                    "cookie_enabled": "true",
                    "screen_width": "1536",
                    "screen_height": "864",
                    "browser_language": "zh-CN",
                    "browser_platform": "Win32",
                    "browser_name": "Edge",
                    "browser_version": "140.0.0.0",
                    "browser_online": "true",
                    "engine_name": "Blink",
                    "engine_version": "140.0.0.0",
                    "os_name": "Windows",
                    "os_version": "10",
                    "device_memory": "8",
                    "platform": "PC",
                    "downlink": "10",
                    "effective_type": "4g",
                    "round_trip_time": "250",
                    # "webid": "7543449142128838187",
                    # "a_bogus": "mJ0Rgw6Ld2mROdFbucjs7RCUwXolNB8yFXi/SueU9xqnOhMTKbNbLPSDrxujlIbWi8BihH3H8jzAbxVcs07kZH9pLmpfSDt6ps/A98sLgqq6GFvdErfie0mFLwBF0mJN-ACyiA0RWsMr2VnRVqVYABZGS5zH5RfgbqB5p2t9rDS8pByTno2CeryAr1y="
                },
                'cookies': {
                    "__ac_nonce": "068afb71500d70206bd4d",
                    "__ac_signature": "_02B4Z6wo00f01l7i70gAAIDD06KW2ERb28JewuvAAP8mdc",
                    "SEARCH_RESULT_LIST_TYPE": "%22single%22",
                    "ttwid": "1%7C3UtJYD4Z7dI6-NKHKpx-d3rhfKe_4R4xsqnop208N64%7C1756346134%7C62d60c8733a754177aca857099dcb6ea1321aad8b6dae6d7d0b0b0845ae15aeb",
                    "x-web-secsdk-uid": "c7d342ba-a609-47f4-99d8-1c6299b817da",
                    "home_can_add_dy_2_desktop": "%220%22",
                    "csrf_session_id": "00e9b3ae0fe0cc1b8c3aaaa2405175cb"
                }
            },
            'chrome': {
                'ck_headers1': {
                    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36",
                    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                    "Accept-Encoding": "gzip, deflate, br, zstd",
                    "sec-ch-ua": "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"",
                    "sec-ch-ua-mobile": "?0",
                    "sec-ch-ua-platform": "\"Windows\"",
                    "upgrade-insecure-requests": "1",
                    "sec-fetch-site": "none",
                    "sec-fetch-mode": "navigate",
                    "sec-fetch-user": "?1",
                    "sec-fetch-dest": "document",
                    "accept-language": "zh-CN,zh;q=0.9",
                    "priority": "u=0, i"
                },
                'ck_headers2': {
                    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36",
                    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                    "Accept-Encoding": "gzip, deflate, br, zstd",
                    "cache-control": "max-age=0",
                    "sec-ch-ua": "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"",
                    "sec-ch-ua-mobile": "?0",
                    "sec-ch-ua-platform": "\"Windows\"",
                    "upgrade-insecure-requests": "1",
                    "sec-fetch-site": "same-origin",
                    "sec-fetch-mode": "navigate",
                    "sec-fetch-dest": "document",
                    "referer": "https://www.douyin.com/search/%E6%97%BA%E4%BB%94%E5%B0%8F%E4%B9%94?type=video",
                    "accept-language": "zh-CN,zh;q=0.9",
                    "priority": "u=0, i"
                },
                'headers': {
                    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36",
                    "Accept": "application/json, text/plain, */*",
                    "Accept-Encoding": "gzip, deflate, br, zstd",
                    "uifid": "undefined",
                    "sec-ch-ua-platform": "\"Windows\"",
                    "sec-ch-ua": "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"",
                    "sec-ch-ua-mobile": "?0",
                    "sec-fetch-site": "same-origin",
                    "sec-fetch-mode": "cors",
                    "sec-fetch-dest": "empty",
                    "referer": "https://www.douyin.com/search/%E6%97%BA%E4%BB%94%E5%B0%8F%E4%B9%94?type=video",
                    "accept-language": "zh-CN,zh;q=0.9",
                    "priority": "u=1, i"
                },
                'params': {
                    "device_platform": "webapp",
                    "aid": "6383",
                    "channel": "channel_pc_web",
                    "search_channel": "aweme_video_web",
                    "enable_history": "1",
                    "keyword": "旺仔小乔",
                    "search_source": "normal_search",
                    "query_correct_type": "1",
                    "is_filter_search": "0",
                    "from_group_id": "",
                    "offset": "0",
                    "count": "10",
                    "need_filter_settings": "1",
                    "list_type": "single",
                    "update_version_code": "170400",
                    "pc_client_type": "1",
                    "pc_libra_divert": "Windows",
                    "support_h265": "1",
                    "support_dash": "0",
                    "cpu_core_num": "24",
                    "version_code": "170400",
                    "version_name": "17.4.0",
                    "cookie_enabled": "true",
                    "screen_width": "1920",
                    "screen_height": "1080",
                    "browser_language": "zh-CN",
                    "browser_platform": "Win32",
                    "browser_name": "Chrome",
                    "browser_version": "139.0.0.0",
                    "browser_online": "true",
                    "engine_name": "Blink",
                    "engine_version": "139.0.0.0",
                    "os_name": "Windows",
                    "os_version": "10",
                    "device_memory": "8",
                    "platform": "PC",
                    "downlink": "10",
                    "effective_type": "4g",
                    "round_trip_time": "150",
                    # "webid": "7543456503757276681",
                    # "a_bogus": "OvsfgztEDdRVOdMt8OcatJQlMqD/rs8ymsiObaAlyPoZchlYsbPjLcbUcxwzshLki8Bih91HbjUAbxVcs070ZCrpFmpfuQ4Sq4299wmogqwvbtJBDHDiCuXzLw0Ol5kEe5/4iHW5As0K2dcWVqC0AB3Hw/3xRREdFH-vVMuni9uRUASjwx/IaVSdihGqbj=="
                },
                'cookies': {
                    "__ac_nonce": "068afbdc4001711369402",
                    "__ac_signature": "_02B4Z6wo00f01766rBgAAIDDMPfaLethNie-mqiAAIcJ62",
                    "SEARCH_RESULT_LIST_TYPE": "%22single%22",
                    "ttwid": "1%7CZkkHdS7EFoZTpvr96C5CAvJ9je5r0pHNq-iH0U7pP0U%7C1756347845%7Cdbf33eeda6f4e924a5e1d1a2f31dbc0ba2ceefd611ff63dd7705321f8494d15b",
                    "x-web-secsdk-uid": "f3902c5d-53dc-4639-b91c-e358189fd9ed",
                    "home_can_add_dy_2_desktop": "%220%22",
                    "hevc_supported": "true",
                    "csrf_session_id": "b7e1bec1fbed5eb3a1f80dd4d33417a9"
                }
            }
        }

        self.browser_name = 'sougou'
        self.browser_change_count = 0  # 浏览器切换次数

        # self.proxies = {'http': 'http://25A52IC3G2:vu4gsusi1vktdao@218.95.39.94:12114', 'https': 'http://25A52IC3G2:vu4gsusi1vktdao@218.95.39.94:12114'}
        self.proxies = {'http': 'http://127.0.0.1:7897', 'https': 'http://127.0.0.1:7897'}

        self.cookies = self.get_cookies()

        self.x_web_secsdk_uid =  str(uuid.uuid4())
        self.csrf_session_id = str(secrets.token_hex(16))

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

    def get_cookies(self):
        headers = self.browser_obj[self.browser_name]['ck_headers1']

        # 第一次请求
        response = requests.get(self.url, headers=headers)
        cookies = response.cookies.get_dict()
        __ac_nonce = cookies.get('__ac_nonce')
        ua = headers.get('User-Agent', headers.get('user-agent'))
        __ac_signature = self.get_ac_signature(ua, __ac_nonce)
        cookies.update({
            '__ac_signature': __ac_signature,
            '__ac_referer': '__ac_blank'
        })

        # 第二次请求
        headers = self.browser_obj[self.browser_name]['ck_headers2']
        if 'referer' in headers:
            headers['referer'] = self.url
        elif 'Referer' in headers:
            headers['Referer'] = self.url

        response = requests.get(self.url, headers=headers, cookies=cookies)
        ttwid = response.cookies.get_dict().get('ttwid')
        cookies.update({'ttwid': ttwid})

        return cookies

    # 时间戳转日期
    def convert_timestamp(self, timestamp):
        if timestamp:
            if len(str(timestamp)) == 13:
                _timestamp = int(timestamp) / 1000
            else:
                _timestamp = int(timestamp)
            dt_object = datetime.fromtimestamp(_timestamp)
            formatted_time = dt_object.strftime('%Y-%m-%d %H:%M:%S')
            return formatted_time
        else:
            return None

    def get_current_time(self):
        now = datetime.now()
        format_time = now.strftime("%Y-%m-%d %H:%M:%S")
        return format_time

    def search_video_item(self, current_page, search_id, sort_type, publish_time, filter_duration):
        print(f'当前浏览器环境: {self.browser_name}')
        headers = self.browser_obj[self.browser_name]['headers']
        if 'referer' in headers:
            headers['referer'] = self.url
        elif 'Referer' in headers:
            headers['Referer'] = self.url

        cookies = self.browser_obj[self.browser_name]['cookies']

        cookies.update({
            "__ac_nonce": self.cookies.get('__ac_nonce'),
            "__ac_signature": self.cookies.get('__ac_signature'),
            "ttwid": self.cookies.get('ttwid'),
            "x-web-secsdk-uid": self.x_web_secsdk_uid,
            "csrf_session_id": self.csrf_session_id
        })
        print(f'初始化cookies信息: {cookies}')

        url = "https://www.douyin.com/aweme/v1/web/search/item/"

        params = self.browser_obj[self.browser_name]['params']
        params.update({
            "keyword": self.keyword,
            "offset": str((current_page - 1) * 25),
            "count": "25",
            "need_filter_settings": "1" if current_page == 1 else "0"
        })

        if current_page > 1:
            params['search_id'] = search_id

        # 第一遍请求：带上游标请求一次，不带筛选条件
        # requests.get(url, headers=headers, cookies=cookies, params=params)
        # time.sleep(1)
        # print("第一遍请求等待1秒")

        if sort_type == '0' and publish_time == '0' and filter_duration == '':
            params['is_filter_search'] = '0'
        else:
            # params['sort_type'] = sort_type
            params['publish_time'] = publish_time
            # params['filter_duration'] = filter_duration
            params['is_filter_search'] = '1'

        # 第二遍请求：带上游标请求一次，带上筛选条件

        try:
            # print("第二遍请求之后的结果👇👇👇")
            response = requests.get(url, headers=headers, cookies=cookies, params=params)
            response_text = response.text

            if len(response_text) < 10:
                print('被风控!!!!!!!!!!!!!!!!!!!!!!')
                print(response_text)
                return None

            else:
                json_data = response.json()
                if 'search_nil_info' in json_data:
                    print(f'出现风控！切换浏览器 {json_data}')
                    time.sleep(2)

                    print(f'self.browser_change_count---{self.browser_change_count}')


                    self.browser_change_count += 1

                    if self.browser_change_count == 1:
                        self.browser_name = 'sougou'
                    elif self.browser_change_count == 2:
                        self.browser_name = 'edge'
                    elif self.browser_change_count == 3:
                        return None

                    self.cookies = self.get_cookies()
                    print('同步更新cookie')

                    return self.search_video_item(current_page, search_id, sort_type, publish_time, filter_duration)

                # 成功正常返回数据
                elif json_data.get('status_code') == 0 and json_data.get('data'):
                    return json_data

                else:
                    print(json_data)
                    print('出现风控！切换浏览器')
                    return None

        except requests.RequestException as e:
            print(f"请求出错: {e}")
            return None

    # 搜索视频
    def search_item(self):

        aweme_id_list = []

        current_page = 1
        search_id = None
        items = []
        max_count = 300  # 爬取的最大作品数
        requests_count = 0
        should_exit = False  # 用于控制是否退出整个爬取循环

        digg_count_list = []

        while True:
            print(f"正在爬取第{current_page}页。。。。。")
            time.sleep(3)

            sort_type = "1"  # 排序方式 0:综合排序, 1:最多点赞, 2:最新发布
            publish_time = "180"  # 发布时间 0:不限, 1:一天内, 7:一周内, 180:半年内
            filter_duration = ""  # 视频时长 默认不限, 0-1:一分钟以下, 1-5:五分钟, 5-1000:五分钟以上


            json_data = self.search_video_item(current_page, search_id, sort_type, publish_time, filter_duration)
            requests_count += 1
            print(f'当前时间：{self.get_current_time()} 当前请求次数: {requests_count}')


            if json_data:
                if current_page == 1:
                    search_id = jmespath.search('extra.logid', json_data)
                    print(f'search_id: {search_id}')
                has_more = json_data.get('has_more')
                if has_more:
                    print('还有下一页')
                else:
                    print('没有下一页')
                    break

                for i in json_data.get('data'):
                    aweme_info = i.get('aweme_info', {})

                    if jmespath.search('video.dynamic_cover.url_list[0]', aweme_info):
                        digg_count_list.append(jmespath.search('statistics.digg_count', aweme_info))

                    aweme_id = aweme_info.get('aweme_id')
                    aweme_id_list.append(aweme_id)

                    video_duration = jmespath.search('video.duration', aweme_info)

                    item = {
                        'aweme_id': aweme_id,
                        'desc': aweme_info.get('desc'),
                        'create_time': self.convert_timestamp(aweme_info.get('create_time')),

                        'author': {
                            'uid': jmespath.search('author.uid', aweme_info),
                            'nickname': jmespath.search('author.nickname', aweme_info),
                            'short_id': jmespath.search('author.short_id', aweme_info),
                            'unique_id': jmespath.search('author.unique_id', aweme_info),
                            'signature': jmespath.search('author.signature', aweme_info),
                            'sec_uid': jmespath.search('author.sec_uid', aweme_info),
                            'avatar_thumb': jmespath.search('author.avatar_thumb.url_list[0]', aweme_info),
                            'aweme_count': jmespath.search('author.aweme_count', aweme_info),
                            'following_count': jmespath.search('author.following_count', aweme_info),
                            'follower_count': jmespath.search('author.follower_count', aweme_info),
                            'favoriting_count': jmespath.search('author.favoriting_count', aweme_info),
                            'total_favorited': jmespath.search('author.total_favorited', aweme_info),

                        },
                        'images': None,

                        'music': {
                            'title': jmespath.search('music.title', aweme_info),
                            'play_url': jmespath.search('music.play_url.uri', aweme_info),
                            'duration': jmespath.search('music.duration', aweme_info),
                        },

                        'video': {

                            'play_url': None,
                            'duration': int(
                                video_duration / 1000) if video_duration and video_duration > 1000 else video_duration,
                            'dynamic_cover': jmespath.search('video.dynamic_cover.url_list[0]', aweme_info),
                        },

                        'statistics': aweme_info.get('statistics')
                    }

                    # 判断是不是图文
                    images = i.get('images')
                    if images:
                        item['images'] = [img['url_list'][0] for img in images]
                        item['aweme_type'] = 'note'
                        item['aweme_url'] = f'https://www.douyin.com/note/{aweme_id}'
                    else:
                        item['aweme_type'] = 'video'
                        item['aweme_url'] = f'https://www.douyin.com/video/{aweme_id}'

                    # 获取视频下载地址
                    video_url_list = jmespath.search('video.play_addr.url_list', aweme_info)
                    if video_url_list:
                        item['video']['play_url'] = video_url_list
                        # for play_addr in video_url_list:
                            # if 'v3-web.douyinvod.com' in play_addr:
                            #     item['video']['play_url'] = play_addr

                    # print(json.dumps(item, ensure_ascii=False, indent=4))
                    print(item)

                    items.append(item)

                    if len(items) >= max_count:
                        print(f'满足{max_count}条')
                        should_exit = True
                        break
            else:
                should_exit = True

            # 检查是否需要退出整个爬取循环
            if should_exit:
                break
            current_page += 1

        print('======== 爬取完成 ==========')
        print(f"共{current_page - 1}页，一共{len(items)}个作品")

        # 检查去重
        print(f'一共{len(aweme_id_list)}')
        print(f'去重之后，一共{len(set(aweme_id_list))}')
        print(digg_count_list)

        print(len(digg_count_list))
        print(sorted(digg_count_list, reverse=True))

if __name__ == '__main__':
    s = Spider()
    s.search_item()

    # count = 0
    # while True:
    #     time.sleep(3)
    #     count += 1
    #     print(f'轮询{count}次')
    #     s = Spider()
    #     s.search_item()


    #
    #
    # b = [524442, 285193, 218927, 216861, 194495, 187138, 160321, 143678, 138865, 134175, 116785, 107563, 107277, 102096, 99512, 88620, 85353, 83015, 74202, 73512, 72618, 56543, 54775, 52075, 51980, 50332, 45850, 45337, 42931, 39344, 36828, 34582, 33582, 32387, 29935, 29785, 29762, 27965, 27651, 25365, 25365, 25221, 24557, 20591, 20034, 20034, 19286, 18033, 17789, 17482, 17401, 16839, 16145, 15361, 13852, 12507, 12488, 12239, 12231, 10100, 9974, 9328, 9328, 7735, 7073, 6826, 6268, 5539, 5251, 4803, 4540, 4360, 4106, 3681, 3362, 2672, 2611, 2540, 2360, 2204, 2113, 1891, 1841, 1432]
    # print(len(b))