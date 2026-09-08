


import frida
import hashlib
import random
import uuid
import time
import gzip
import json
import jmespath
import requests
from urllib.parse import urlencode, quote, quote_plus

# 随机MAC地址
def generate_mac():
    mac = [random.randint(0x00, 0xff) for _ in range(6)]
    mac_address = ':'.join(f"{byte:02X}" for byte in mac)
    return mac_address


def generate_cdid():
    return str(uuid.uuid4())


# Luhn 算法
def luhn_check_digit(number):
    """计算Luhn校验位"""

    def digits_of(n):
        return [int(d) for d in str(n)]

    digits = digits_of(number)
    odd_digits = digits[-1::-2]
    even_digits = digits[-2::-2]
    checksum = sum(odd_digits)
    for d in even_digits:
        checksum += sum(digits_of(d * 2))
    return (10 - (checksum % 10)) % 10


# 随机IMEI
def generate_imei():
    # 前14位随机数字
    first_14_digits = str(random.randint(10 ** 13, 10 ** 14 - 1))
    # 计算第15位校验位
    check_digit = luhn_check_digit(first_14_digits)
    return first_14_digits + str(check_digit)


def generate_openudid():
    return "".join([hex(i)[2:] for i in [random.randint(1, 255) for i in range(8)]])



def parse_response_json(json_data):
    if json_data.get('status_code') == 0:
        user = json_data.get('user', {})

        item = {
            'nickname': user.get('nickname'),
            'uid': user.get('uid'),
            'unique_id': user.get('unique_id'),
            'sec_uid': user.get('sec_uid'),

            'aweme_count': user.get('aweme_count'),
            'follower_count': user.get('follower_count'),
            'following_count': user.get('following_count'),
            'total_favorited': user.get('total_favorited'),
            'signature': user.get('signature'),
            'avatar_larger': jmespath.search('avatar_larger.url_list[0]', user),

            'live_status': user.get('live_status'),
            'room_id_str': user.get('room_id_str'),

        }

        room_data = user.get('room_data')
        if room_data:
            room_data = json.loads(room_data)
            item['user_count'] = room_data.get('user_count')
            item['client_version'] = room_data.get('client_version')
            item['stream_id_str'] = room_data.get('stream_id_str')
            item['stream_url'] = {
                'resolution_name': jmespath.search('stream_url.resolution_name', room_data),
                'hls_pull_url': jmespath.search('stream_url.hls_pull_url', room_data),
                'hls_pull_url_map': jmespath.search('stream_url.hls_pull_url_map', room_data),
            }

        print(item)

    else:
        print('获取数据失败')
        print(json_data)


# 用户信息接口
def user_profile(sec_user_id):
    mac_addr = generate_mac()
    cdid = generate_cdid()
    imei = generate_imei()
    openudid = generate_openudid() # 手机 设备id
    _rticket = int(time.time() * 1000)
    ts = int(time.time())


    device_id = "4484249592557674"
    iid = '810786544184746'


    headers = {
        "User-Agent": "okhttp/3.10.0.1",
        "Accept-Encoding": "gzip",
        "x-ss-req-ticket": str(_rticket),
        "sdk-version": "1",
        # "x-khronos": "1753314829",
        # "x-gorgon": "0404b8034000cfe1079f83b36ec67c57728751b8f75ac96ba5d1"
        "x-khronos": str(ts),

    }

    url = "https://aweme.snssdk.com/aweme/v1/user/profile/other/"
    params = {
        "sec_user_id": sec_user_id,
        "address_book_access": "2",
        "from": "0",
        "publish_video_strategy_type": "2",
        "manifest_version_code": "110501",
        "_rticket": str(_rticket),
        "app_type": "normal",
        "iid": iid,
        "channel": "gdt_growth14_big_yybwz",
        "device_type": "V2307A",
        "language": "zh",
        "cpu_support64": "true",
        "host_abi": "armeabi-v7a",
        "uuid": imei,
        "resolution": "900*1600",
        "openudid": openudid,

        "update_version_code": "11509900",
        "cdid": cdid,
        "os_api": "28",
        "mac_address": mac_addr,
        "dpi": "240",
        "ac": "wifi",
        "device_id": device_id,
        "mcc_mnc": "46000",
        "os_version": "9",
        "version_code": "110500",
        "app_name": "aweme",
        "version_name": "11.5.0",
        "device_brand": "vivo",
        "ssmix": "a",
        "device_platform": "android",
        "aid": "1128",
        "ts": str(ts)
    }
    response = requests.get(url, headers=headers, params=params)
    print(response)

    try:
        json_data = response.json()
        parse_response_json(json_data)

    except Exception as e:
        print(e)




if __name__ == '__main__':
    # sec_user_id = "MS4wLjABAAAA8rM5xQ55m3Igx17bR9oGA91ZuKCycIvzEpXcX-sT_wU"
    sec_user_id = "MS4wLjABAAAA3RvXcPsgIChQPrrzC1tOhvZYFoUdgw-8ZpzVU0IZEmUhcKq-v8GVQKHce9iJovhn"

    user_profile(sec_user_id)