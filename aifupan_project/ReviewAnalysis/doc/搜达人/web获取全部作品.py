
import copy
import json
import requests
import jmespath

from datetime import datetime, timedelta
from urllib.parse import quote



class Spider(object):
    def __init__(self):
        # 4个浏览器环境
        self.browser_obj = {
            'sougou': {
                'headers': {
                    "authority": "www.douyin.com",
                    "accept": "application/json, text/plain, */*",
                    "accept-language": "zh-CN,zh;q=0.9",
                    "cache-control": "no-cache",
                    "pragma": "no-cache",
                    "referer": "https://www.douyin.com/search/%E4%BA%A4%E4%B8%AA%E6%9C%8B%E5%8F%8B?type=user",
                    "sec-ch-ua": "\"Not)A;Brand\";v=\"24\", \"Chromium\";v=\"116\"",
                    "sec-ch-ua-mobile": "?0",
                    "sec-ch-ua-platform": "\"Windows\"",
                    "sec-fetch-dest": "empty",
                    "sec-fetch-mode": "cors",
                    "sec-fetch-site": "same-origin",
                    # "uifid": "ec0746514434774ae4eddab31fb3c8fefcd9205b76567c8a3d84bacd841a105d0c5c732bb44d39ff645a7cd69ef3b9aee47cfcbd7652c300379b857b41f42d1521c6dd3db7a97608533c49a7c47810760a9bd4f33620a87182868aa6eada306a5702e845c6cab8c2010c42bc04ca97d65079cedfae0d76b46f0af9c920484fc8a1d0942696577b2483873a82404bc1cb19139e36d890a878166372dbb27b0102",
                    "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.5845.97 Safari/537.36 SE 2.X MetaSr 1.0"
                },
                'params': {
                    "update_version_code": "170400",
                    "pc_client_type": "1",
                    "pc_libra_divert": "Windows",
                    "support_h265": "1",
                    "support_dash": "1",
                    "cpu_core_num": "12",
                    # "version_code": "170400", # 每个接口不一样
                    # "version_name": "17.4.0",
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
                    "round_trip_time": "0",
                }
            },
            'firefox': {
                'headers': {
                    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/116.0",
                    "Accept": "application/json, text/plain, */*",
                    "Accept-Language": "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2",
                    "Accept-Encoding": "gzip, deflate, br",
                    "uifid": "undefined",
                    "Connection": "keep-alive",
                    "Referer": "https://www.douyin.com/search/%E4%BA%A4%E4%B8%AA%E6%9C%8B%E5%8F%8B?type=user",
                    "Sec-Fetch-Dest": "empty",
                    "Sec-Fetch-Mode": "cors",
                    "Sec-Fetch-Site": "same-origin",
                    "Pragma": "no-cache",
                    "Cache-Control": "no-cache"
                },
                'params': {
                    "update_version_code": "170400",
                    "pc_client_type": "1",
                    "pc_libra_divert": "Windows",
                    "support_h265": "0",
                    "support_dash": "0",
                    "cpu_core_num": "8",
                    # "version_code": "170400",
                    # "version_name": "17.4.0",
                    "cookie_enabled": "true",
                    "screen_width": "1536",
                    "screen_height": "864",
                    "browser_language": "zh-CN",
                    "browser_platform": "Win32",
                    "browser_name": "Firefox",
                    "browser_version": "116.0",
                    "browser_online": "true",
                    "engine_name": "Gecko",
                    "engine_version": "109.0",
                    "os_name": "Windows",
                    "os_version": "10",
                    "device_memory": "",
                    "platform": "PC",
                }
            },
            'chrome': {
                'headers': {
                    "accept": "application/json, text/plain, */*",
                    "accept-language": "zh-CN,zh;q=0.9",
                    "cache-control": "no-cache",
                    "pragma": "no-cache",
                    "priority": "u=1, i",
                    "referer": "https://www.douyin.com/search/%E4%BA%A4%E4%B8%AA%E6%9C%8B%E5%8F%8B?type=user",
                    "sec-ch-ua": "\"Google Chrome\";v=\"137\", \"Chromium\";v=\"137\", \"Not/A)Brand\";v=\"24\"",
                    "sec-ch-ua-mobile": "?0",
                    "sec-ch-ua-platform": "\"Windows\"",
                    "sec-fetch-dest": "empty",
                    "sec-fetch-mode": "cors",
                    "sec-fetch-site": "same-origin",
                    # "uifid": "e71d819f1cb72e7166823ce125547a3e5a83b631a52f7c0b3c34cd9714dd602dd116995b92bf8b8f9d22fde1d4fcf3eae2188d7aecb862129afbcffbd626d3b6feb128a2d884eadf7cbff1e1e2f705f6fb012dc9821e872b08766a81e435190f60c16b747cbb97a43beac765c4825a42c387e7fc3f58583860cca3e34c7080ee64a599fb4808adc55d34d01ae1ab21f200f7b1e900d858b12aea3f870be7e85a",
                    "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36"
                },
                'params': {
                    "update_version_code": "170400",
                    "pc_client_type": "1",
                    "pc_libra_divert": "Windows",
                    "support_h265": "1",
                    "support_dash": "1",
                    "cpu_core_num": "8",
                    # "version_code": "170400",
                    # "version_name": "17.4.0",
                    "cookie_enabled": "true",
                    "screen_width": "1536",
                    "screen_height": "864",
                    "browser_language": "zh-CN",
                    "browser_platform": "Win32",
                    "browser_name": "Chrome",
                    "browser_version": "137.0.0.0",
                    "browser_online": "true",
                    "engine_name": "Blink",
                    "engine_version": "137.0.0.0",
                    "os_name": "Windows",
                    "os_version": "10",
                    "device_memory": "8",
                    "platform": "PC",
                }
            },
            'edge': {
                'headers': {
                    "accept": "application/json, text/plain, */*",
                    "accept-language": "zh-CN,zh;q=0.9",
                    "cache-control": "no-cache",
                    "pragma": "no-cache",
                    "priority": "u=1, i",
                    "referer": "https://www.douyin.com/search/%E4%BA%A4%E4%B8%AA%E6%9C%8B%E5%8F%8B?type=user",
                    "sec-ch-ua": "\"Microsoft Edge\";v=\"137\", \"Chromium\";v=\"137\", \"Not/A)Brand\";v=\"24\"",
                    "sec-ch-ua-mobile": "?0",
                    "sec-ch-ua-platform": "\"Windows\"",
                    "sec-fetch-dest": "empty",
                    "sec-fetch-mode": "cors",
                    "sec-fetch-site": "same-origin",
                    # "uifid": "8f584679f13361d7674396b25a46f1c75dd7736730eef2bca7f1b3cfea8b216d91caa048d9446194f26db003eb0097fc895565c811b33bb580062212173e3d9e907a52842a792e043a7baa00a8eb7ae0f21ec2811a0205393f153a3ba979d3708e29f74ba53e314159594251fc2d762bf83091e033ab801afc91be1fcea3312996675278b51042ff53b7c30c0c4c01ccb4c173f036d2cc7bf1eb2e6d5193c8e6",
                    "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0"
                },
                'params': {
                    "update_version_code": "170400",
                    "pc_client_type": "1",
                    "pc_libra_divert": "Windows",
                    "support_h265": "1",
                    "support_dash": "1",
                    "cpu_core_num": "12",
                    # "version_code": "170400",
                    # "version_name": "17.4.0",
                    "cookie_enabled": "true",
                    "screen_width": "1920",
                    "screen_height": "1080",
                    "browser_language": "zh-CN",
                    "browser_platform": "Win32",
                    "browser_name": "Edge",
                    "browser_version": "137.0.0.0",
                    "browser_online": "true",
                    "engine_name": "Blink",
                    "engine_version": "137.0.0.0",
                    "os_name": "Windows",
                    "os_version": "10",
                    "device_memory": "8",
                    "platform": "PC",
                }
            }
        }
        self.sessionid = "ee362944ff59baa7064cd8c1552c33c5"
        # self.keyword = '王者荣耀'
        self.keyword = 'honorofkings' # 王者荣耀抖音号

    # 时间戳转日期
    def convert_timestamp(self, timestamp):
        if timestamp:
            dt_object = datetime.fromtimestamp(timestamp)
            formatted_time = dt_object.strftime('%Y-%m-%d %H:%M:%S')
            return formatted_time
        else:
            return None

    # 巨量算数搜索达人
    def search_daren(self):
        '''
        keyword: 达人名称或者达人抖音号
        '''
        headers = {
            "accept": "application/json, text/plain, */*",
            "accept-language": "zh-CN,zh;q=0.9",
            "appsource": "PC",
            "cache-control": "no-cache",
            "content-type": "application/json",
            "origin": "https://trendinsight.oceanengine.com",
            "pragma": "no-cache",
            "priority": "u=1, i",
            "referer": f"https://trendinsight.oceanengine.com/arithmetic-index/daren/search?keyword={quote(self.keyword)}",
            "sec-ch-ua": "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\"",
            "sec-ch-ua-mobile": "?0",
            "sec-ch-ua-platform": "\"Windows\"",
            "sec-fetch-dest": "empty",
            "sec-fetch-mode": "cors",
            "sec-fetch-site": "same-origin",
            "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
            "x-secsdk-csrf-token": "DOWNGRADE"
        }

        url = "https://trendinsight.oceanengine.com/api/v2/daren/get_sug_great_user_list"

        # params = {
        #     "msToken": "qj2-veHt28vlgyrjbICuax8pG9W6149_fLsVVRNNLf9NGjEbGwCJyDoCy8mj69hZC7hcd1dOgxwkfXTQLxy4D2Dg1bRmXPaNqMDBAmL70ZUSEx23zPxTXmPYLvtRbhY=",
        #     "X-Bogus": "DFSzswVLtEuVASXFCSJFkpYhjsNt",
        #     "_signature": "_02B4Z6wo00001q40omgAAIDBMFAC-5GxX56uNKbAAMQB8YCk--.jSJlcM8bCaU4Z2cC0PBNSj8.yn4pWxsoel0gPn-1LMbxmvX.ezDI5JOP0NKN3NGMxReJ-Exx5Li0XwMMYyLE51mlXd0Pq7f"
        # }
        data = {
            "total": "30",
            "keyword": self.keyword
        }

        cookies = {'sessionid_count': self.sessionid}

        data = json.dumps(data, separators=(',', ':'))

        try:
            response = requests.post(url, headers=headers, cookies=cookies, data=data)
            json_data = response.json()
            if json_data.get('status') == 0:
                userlist = json_data['data']['userlist']
                if userlist:
                    result = []
                    for user in userlist:
                        aweme_id = user['aweme_id']
                        if aweme_id == self.keyword:
                            result.append(user)
                            return result
                    return userlist

                else:
                    print('没有结果')

            else:
                print(json_data)
                return None
        except:
            print('请求异常')
            return None

    def get_aweme_list(self, sec_uid, current_page, max_cursor, browser_name):
        url = "https://www.douyin.com/aweme/v1/web/aweme/post/" if current_page == 1 else "https://www-hj.douyin.com/aweme/v1/web/aweme/post/"

        headers = self.browser_obj[browser_name]['headers']
        _params = self.browser_obj[browser_name]['params']

        if "user-agent" in headers:
            ua = headers["user-agent"]
            headers['referer'] = f"https://www.douyin.com/user/{sec_uid}"
        else:
            ua = headers['User-Agent']
            headers['Referer'] = f"https://www.douyin.com/user/{sec_uid}"

        params = {
            "device_platform": "webapp",
            "aid": "6383",
            "channel": "channel_pc_web",
            "sec_user_id": sec_uid,
            "max_cursor": str(max_cursor),
            "locate_query": "false",
            "show_live_replay_strategy": "1",
            "need_time_list": "1" if current_page == 1 else "0",
            "time_list_query": "0",
            "whale_cut_token": "",
            "cut_version": "1",
            "count": "25",  # 默认18,可调到41
            "publish_video_strategy_type": "2",
            "from_user_page": "1",
            "update_version_code": "170400",
            "pc_client_type": "1",
            "pc_libra_divert": "Windows",
            "support_h265": "1",
            "support_dash": "1",
            "cpu_core_num": "12",
            "version_code": "290100",
            "version_name": "29.1.0",
            "cookie_enabled": "true",
            "screen_width": "1920",
            "screen_height": "1080",
            "browser_language": "zh-CN",
            "browser_platform": "Win32",
            "browser_name": "Edge",
            "browser_version": "137.0.0.0",
            "browser_online": "true",
            "engine_name": "Blink",
            "engine_version": "137.0.0.0",
            "os_name": "Windows",
            "os_version": "10",
            "device_memory": "8",
            "platform": "PC",
            # "downlink": "10",
            # "effective_type": "4g",
            # "round_trip_time": "0",
            # "webid": "7523110540073059868",
            # "uifid": "8f584679f13361d7674396b25a46f1c75dd7736730eef2bca7f1b3cfea8b216d91caa048d9446194f26db003eb0097fc895565c811b33bb580062212173e3d9e907a52842a792e043a7baa00a8eb7ae0f21ec2811a0205393f153a3ba979d3708e29f74ba53e314159594251fc2d762bf83091e033ab801afc91be1fcea3312996675278b51042ff53b7c30c0c4c01ccb4c173f036d2cc7bf1eb2e6d5193c8e6",
            # "verifyFp": "verify_mcofrtm3_bGkhJvSl_MkcG_4nLm_8nKI_ESlVWlRkHlvi",
            # "fp": "verify_mcofrtm3_bGkhJvSl_MkcG_4nLm_8nKI_ESlVWlRkHlvi",
            # "msToken": "mBQTAjA582jiQjTBSXwr6zbkC4OI-DWIH4JUMizk46mC12z77F2gp2H5y4k03g6AYshTVOpJmYZat38UhOxjkZBj3AyMlzSeJtQP7DofIbBsqqNsXOpqtIvUrmc7i71E0GqzgQHv_dAevDdkVk5ay1Dc4BRBBw5SDWzrx6Cr4X9QSQ==",
            # "a_bogus": "Yv0jkHXiOoA5cdFSYOaEt4lUn02MNTWyPUT/bTlPtOOHG1Ma8bNRhNernxKR3M8zmuBkkKIHCEUAbxncOzXzZKnpKmpkSqkWOT5An6vLhqiRGtGmDrb8eLRzuwsx0cTq-5VXil4I/UrH6VnAhqQu/Ql99KoCQRSBB3xjkZYbE9sgZzgAE1nHPpShThiqIf=="
        }

        params.update(_params)

        cookies = {'sessionid': self.sessionid}

        try:
            response = requests.get(url, headers=headers, cookies=cookies, params=params, timeout=10)
            response_text = response.text

            if len(response_text) < 10:
                print('更换浏览器环境!!!!!!!!!!!!!!!!!!!!!!')
                return self.get_aweme_list(sec_uid, current_page, max_cursor, 'chrome')

            else:
                json_data = response.json()
                return json_data

        except requests.RequestException as e:
            print(f"请求出错: {e}")

    def get_user_aweme_list(self, sec_uid):
        current_page = 1
        max_cursor = 0
        items = []
        max_count = 100 # 爬取的最大作品数
        browser_name = 'edge'
        should_exit = False  # 用于控制是否退出整个爬取循环
        while True:
            print(f"正在爬取第{current_page}页。。。。。")
            # time.sleep(1)

            json_data = self.get_aweme_list(sec_uid, current_page, max_cursor, browser_name)

            if json_data.get('status_code') == 0:
                # 更新max_cursor为当前页面返回的值
                max_cursor = json_data.get('max_cursor')
                print(f'翻页游标: {max_cursor}')
                has_more = json_data.get('has_more')
                if has_more:
                    print('还有下一页')
                else:
                    print('没有下一页')
                    print(f'全部作品也没有{max_count}条')
                    break

                aweme_list = json_data.get('aweme_list')

                if aweme_list:
                    for i in aweme_list:
                        is_top = i.get("is_top") # 1 就是置顶
                        create_time = i.get("create_time")
                        aweme_id = i.get("aweme_id")
                        statistics = i.get("statistics")
                        item = {
                            "is_top": is_top,
                            "aweme_id": aweme_id,
                            'aweme_type': None,
                            "aweme_url": None,
                            "desc": i.get("desc"),
                            "images": None,
                            "create_time": self.convert_timestamp(create_time),
                            'music_play_url': jmespath.search('music.play_url.uri', i),
                            "video_play_addr": None,
                            'duration': int(i.get('duration') / 1000) if i.get('duration') else None,  # 秒
                            'cover_url': jmespath.search('video.cover.url_list[-1]', i) # 封面

                        }
                        item.update(statistics)


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
                        url_list = jmespath.search('video.play_addr.url_list', i)
                        if url_list:
                            for play_addr in url_list:
                                if 'v3-web.douyinvod.com' in play_addr:
                                    item['video_play_addr'] = play_addr

                        print(item)
                        items.append(item)

                        if len(items) >= max_count:
                            print(f'满足{max_count}条')
                            should_exit = True
                            break

            # 检查是否需要退出整个爬取循环
            if should_exit:
                break
            current_page += 1

        print('======== 爬取完成 ==========')
        print(f"共{current_page - 1}页，一共{len(items)}个作品")

    def main(self):
        result = self.search_daren()
        if len(result) == 1:
            user = result[0]
            print(f'精准搜索结果: {user}')

            user_name = user.get("user_name")
            aweme_url = user.get("aweme_url")

            sec_uid = aweme_url.split('user/')[-1]
            print(f'开始采集 --- {user_name} --- 前100个作品')

            self.get_user_aweme_list(sec_uid)


        else:
            print('模糊搜索结果')
            for i in result:
                print(i)

if __name__ == '__main__':
    s = Spider()
    s.main()

