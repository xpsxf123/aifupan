


import time
import json
import jmespath
import requests
from datetime import datetime
from urllib.parse import quote, unquote



class Spider(object):
    def __init__(self):
        self.keyword = '旺仔小乔'

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
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
            "Accept-Encoding": "gzip, deflate, br, zstd",
            "upgrade-insecure-requests": "1",
            "sec-fetch-site": "none",
            "sec-fetch-mode": "navigate",
            "sec-fetch-user": "?1",
            "sec-fetch-dest": "document",
            "sec-ch-ua": "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\"",
            "sec-ch-ua-mobile": "?0",
            "sec-ch-ua-platform": "\"Windows\"",
            "accept-language": "zh-CN,zh;q=0.9",
            "priority": "u=0, i"
        }
        url = f"https://www.douyin.com/search/{quote(self.keyword)}?type=general"


        # 第一次请求
        response = requests.get(url, headers=headers)
        cookies = response.cookies.get_dict()
        __ac_nonce = cookies.get('__ac_nonce')
        ua = headers['User-Agent']
        __ac_signature = self.get_ac_signature(ua, __ac_nonce)
        cookies.update({
            '__ac_signature': __ac_signature,
            '__ac_referer': '__ac_blank'
        })

        # 第二次请求
        response = requests.get(url, headers=headers, cookies=cookies)
        ttwid = response.cookies.get_dict().get('ttwid')
        cookies.update({'ttwid': ttwid})
        print(f'初始化cookies信息: {cookies}')
        return cookies


    # 时间戳转日期
    def convert_timestamp(self, timestamp):
        if timestamp:
            dt_object = datetime.fromtimestamp(timestamp)
            formatted_time = dt_object.strftime('%Y-%m-%d %H:%M:%S')
            return formatted_time
        else:
            return None

    # 搜索
    def search_single(self):
        headers = {
            "accept": "application/json, text/plain, */*",
            "accept-language": "zh-CN,zh;q=0.9",
            "cache-control": "no-cache",
            "pragma": "no-cache",
            "priority": "u=1, i",
            "referer": f"https://www.douyin.com/search/{quote(self.keyword)}?type=general",
            "sec-ch-ua": "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Microsoft Edge\";v=\"138\"",
            "sec-ch-ua-mobile": "?0",
            "sec-ch-ua-platform": "\"Windows\"",
            "sec-fetch-dest": "empty",
            "sec-fetch-mode": "cors",
            "sec-fetch-site": "same-origin",
            # "uifid": "d4579b5b1721ffdd22d8a6ff378781159e3b4e5a1248a83fc8c708ec2606b7050e4993082b1dff81edbf2b9939cab28fa0dc98f4fa58f5788b57905af24c3917b6c7031b66bcbb5d87ed3ae29c0cb1921a3802450e20e49bb0d23d3fa4c81934ff0a3ca4f11eadbfb69349807e06004821cdc03b2248a39a5f08676232fca3dde6879543405f6c5ae77e83766a1e50329449d5c56eadd81a13ba1393cb56dfd1",
            "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 Edg/138.0.0.0"
        }

        # 初始化cookie
        cookies = self.get_cookies()
        # cookies = {"ttwid": "1%7CchiWBKI9pdw3Rr1wO7DNLgWvT5c9xORSmxaTE4-JDvk%7C1754393661%7C37520122cd4dbba38614834a5e4c491cc9aabc5a07350ed875666ba776381893"}


        url = "https://www.douyin.com/aweme/v1/web/general/search/single/"

        aweme_id_list = []

        current_page = 1
        search_id = None
        items = []
        max_count = 100  # 爬取的最大作品数

        should_exit = False  # 用于控制是否退出整个爬取循环
        while True:
            print(f"正在爬取第{current_page}页。。。。。")
            # time.sleep(1)

            params = {
                "device_platform": "webapp",
                "aid": "6383",
                "channel": "channel_pc_web",
                "search_channel": "aweme_general",
                "enable_history": "1",
                "filter_selected": "{\"sort_type\":\"1\",\"publish_time\":\"0\"}",
                "keyword": self.keyword,
                "search_source": "tab_search",
                "query_correct_type": "1",
                "is_filter_search": "1",
                "from_group_id": "",
                "offset": str((current_page - 1) * 10),
                "count": "10",
                "need_filter_settings": "1" if current_page == 1 else "0",
                "list_type": "single",
                # "search_id": "20250804173557632F913212E4DF717201",
                "update_version_code": "170400",
                "pc_client_type": "1",
                "pc_libra_divert": "Windows",
                "support_h265": "1",
                "support_dash": "1",
                "cpu_core_num": "12",
                "version_code": "190600",
                "version_name": "19.6.0",
                "cookie_enabled": "true",
                "screen_width": "1920",
                "screen_height": "1080",
                "browser_language": "zh-CN",
                "browser_platform": "Win32",
                "browser_name": "Edge",
                "browser_version": "138.0.0.0",
                "browser_online": "true",
                "engine_name": "Blink",
                "engine_version": "138.0.0.0",
                "os_name": "Windows",
                "os_version": "10",
                "device_memory": "8",
                "platform": "PC",
                "downlink": "10",
                "effective_type": "4g",
                "round_trip_time": "0",
                # "webid": "7534608759194600996",
                # "uifid": "d4579b5b1721ffdd22d8a6ff378781159e3b4e5a1248a83fc8c708ec2606b7050e4993082b1dff81edbf2b9939cab28fa0dc98f4fa58f5788b57905af24c3917b6c7031b66bcbb5d87ed3ae29c0cb1921a3802450e20e49bb0d23d3fa4c81934ff0a3ca4f11eadbfb69349807e06004821cdc03b2248a39a5f08676232fca3dde6879543405f6c5ae77e83766a1e50329449d5c56eadd81a13ba1393cb56dfd1",
                # "msToken": "Z3cvWkLasVoj-vDVW8LcMF-bHB28ilJYpfELkZ9LChMnzHqvJMgulQ8D_-oxCj7bCeGeeYKGSZSX8tVX-008hzCxFsvbzmZrhEo7OHvU0mniHuC5S6CoB3iyOuxdHIKZM6BKAYkZaQk3LpJ_ERjiS9pPN0uuPRuxQqaYl65Xqj4D2LOUxrx7JRA=",
                # "a_bogus": "xjsRDwyEEdmVKd/bYcxOSVPUzXxMNs8ykzTxSGrlyOx2cZlGpSPnwctvjxLIzh9WpupziHAHuV0AYEVb04tTZ9rpwmkkSEt6izVIV0mL/qwdPFw0DqbPCuWzzwBxUbsqaAVviIU60UJ9gVxAhHQg/plyCKLK5bWBMZOWk2zbY9Bh1zgAEpn3PQGpTwNP07AU"
            }

            if current_page > 1:
                params['search_id'] = search_id

            response = requests.get(url, headers=headers, cookies=cookies, params=params)

            json_data = response.json()
            if json_data.get('status_code') == 0 and json_data.get('data'):
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
                        for play_addr in video_url_list:
                            if 'v3-web.douyinvod.com' in play_addr:
                                item['video']['play_url'] = play_addr

                    # print(json.dumps(item, ensure_ascii=False, indent=4))
                    print(item)

                    items.append(item)

                    if len(items) >= max_count:
                        print(f'满足{max_count}条')
                        should_exit = True
                        break

            else:
                print('cookie信息无效!!!!!!!!!!!!!!!!!')
                should_exit = True

            # 检查是否需要退出整个爬取循环
            if should_exit:
                break
            current_page += 1

        print('======== 爬取完成 ==========')
        print(f"共{current_page - 1}页，一共{len(items)}个作品")

        # 检查去重
        print(len(aweme_id_list))
        print(len(set(aweme_id_list)))


if __name__ == '__main__':


    s = Spider()
    s.search_single()
