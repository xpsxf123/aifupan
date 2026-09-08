

# -*- coding: utf-8 -*-
# @Time    : 2026/7/10 11:54
# @Author  : shark
# @File    : 流地址切片下载.py

import os
import re
import shutil
import requests
from concurrent.futures import ThreadPoolExecutor

# 创建一个临时文件夹用来存放下载的 ts 分片
TEMP_DIR = "ts_temp"
if not os.path.exists(TEMP_DIR):
    os.makedirs(TEMP_DIR)

# 模拟的请求头
HEADERS = {
    'user-agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36',
    'referer': 'https://compass.jinritemai.com/',
}


# 1. 获取并解析 m3u8 内容，提取 ts 链接
def get_ts_urls():
    stream_url = "https://v6-compass.ecombdvod.com/8c24abe458b6deff08a327800637df38/6a509f8d/dash/hls-v02d2bg10003d972o12ljht9tt3l27tg/tos-cn-v-0d0172/df14abcdd2114da7b02e025752207713.m3u8?a=4499&ch=0&cr=0&dr=0&cd=0%7C0%7C0%7C0&br=0&bt=0&mime_type=video_mp4&qs=13&rc=ajpvdzxrbzQ1PDYzNGVnM0BpajpvdzxrbzQ1PDYzNGVnM0BxLzBxcWcvbDRhLS1kLy9zYSNxLzBxcWcvbDRhLS1kLy9zcw%3D%3D&btag=c0000e00038000&dy_q=1783655294&l=202607101148148DCD4D4DDA8F5A9C629F"

    try:
        response = requests.get(url=stream_url, headers=HEADERS, timeout=10)
        if response.status_code == 200:
            m3u8_text = response.text
            '''
            response.text 返回是这样的：
                #EXTM3U
                #EXT-X-VERSION:3
                #EXT-X-MEDIA-SEQUENCE:0
                #EXT-X-TARGETDURATION:6
                #EXT-X-DISCONTINUITY
                #EXTINF:5.590, no desc
                https://lf-record-tos.bvfcdn2.com/obj/fcdnlarge4-fcdn-dy/7207443526639315192/push-rtmp-t5.douyincdn.com/stage/2026-07-04/stream-119664280247206711/5590-20260704T114547.613-1-1783136747.ts
                #EXTINF:5.592, no desc
                https://lf-record-tos.bvfcdn2.com/obj/fcdnlarge4-fcdn-dy/7207443526639315192/push-rtmp-t5.douyincdn.com/stage/2026-07-04/stream-119664280247206711/5592-20260704T114552.997-0-1783136748.ts
                #EXTINF:5.591, no desc
                https://lf-record-tos.bvfcdn2.com/obj/fcdnlarge4-fcdn-dy/7207443526639315192/push-rtmp-t5.douyincdn.com/stage/2026-07-04/stream-119664280247206711/5591-20260704T114558.612-0-1783136749.ts
                #EXTINF:5.590, no desc
                https://lf-record-tos.bvfcdn2.com/obj/fcdnlarge4-fcdn-dy/7207443526639315192/push-rtmp-t5.douyincdn.com/stage/2026-07-04/stream-119664280247206711/5590-20260704T114604.153-0-1783136750.ts
                #EXTINF:5.592, no desc
                https://lf-record-tos.bvfcdn2.com/obj/fcdnlarge4-fcdn-dy/7207443526639315192/push-rtmp-t5.douyincdn.com/stage/2026-07-04/stream-119664280247206711/5592-20260704T114609.762-0-1783136751.ts
                #EXTINF:4.308, no desc
                https://lf-record-tos.bvfcdn2.com/obj/fcdnlarge4-fcdn-dy/7207443526639315192/push-rtmp-t5.douyincdn.com/stage/2026-07-04/stream-119664280247206711/4308-20260704T114615.344-0-1783136752.ts
                #EXTINF:5.592, no desc
                #EXT-X-ENDLIST
            
            '''

            # 使用正则匹配所有 http/https 开头的 ts 链接
            ts_urls = re.findall(r'(https?://[^\s]+\.ts)', m3u8_text)
            return ts_urls
    except Exception as e:
        print(f"获取 m3u8 失败: {e}")
    return []


# 2. 单个 ts 文件的下载任务
def download_single_ts(task):
    index, url = task
    # 为了防止顺序错乱，文件名用序号填充，如 00000.ts, 00001.ts
    file_name = f"{index:05d}.ts"
    file_path = os.path.join(TEMP_DIR, file_name)

    # 简单的重试机制
    for i in range(3):
        try:
            res = requests.get(url, headers=HEADERS, timeout=15)
            if res.status_code == 200:
                with open(file_path, 'wb') as f:
                    f.write(res.content)
                print(f" 成功下载: {file_name}")
                return True
        except Exception:
            pass
    print(f"❌ 下载失败: {url}")
    return False


# 3. 按顺序合并 ts 文件并保存为 mp4
def merge_ts_to_mp4(total_count, output_name="output.mp4"):
    print("\n 开始按顺序合并视频分片...")
    with open(output_name, 'wb') as outfile:
        for index in range(total_count):
            file_name = f"{index:05d}.ts"
            file_path = os.path.join(TEMP_DIR, file_name)

            if os.path.exists(file_path):
                with open(file_path, 'rb') as infile:
                    outfile.write(infile.read())
            else:
                print(f"⚠️ 警告: 缺失分片 {file_name}，合并可能不完整。")

    print(f" 合并完成！保存为: {output_name}")

    # 清理临时文件夹
    shutil.rmtree(TEMP_DIR)
    print(" 临时文件清理完毕。")


# 主逻辑
def main():
    # 获取所有分片链接
    ts_urls = get_ts_urls()
    if not ts_urls:
        print("未提取到任何 ts 链接，请检查网络或 m3u8 地址是否过期。")
        return

    total_files = len(ts_urls)
    print(f"共发现 {total_files} 个分片文件，开始多线程下载...")

    # 构建带序号的任务列表，例如: [(0, 'url1'), (1, 'url2'), ...]
    tasks = list(enumerate(ts_urls))

    # 开启线程池（根据需要调整 max_workers，一般 5-10 即可）
    with ThreadPoolExecutor(max_workers=8) as executor:
        executor.map(download_single_ts, tasks)

    # 下载完成后合并
    merge_ts_to_mp4(total_files)


if __name__ == '__main__':
    main()