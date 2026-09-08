package com.jiuyu.replay.api;

//@SpringBootTest
public class ReplayApiApplicationTest {

//    @Resource
//    private AnchorVideoService anchorVideoService;
//    @Resource
//    private AudioAnalysisService audioAnalysisService;
//    @Resource
//    private WordsProperties wordsProperties;
//    @Resource
//    private VideoAnalysisRecordProducer videoAnalysisRecordProducer;
//    @Resource
//    private UploadFileAnalysisService uploadFileAnalysisService;
//    @Resource
//    private UploadFileService uploadFileService;
//    @Resource
//    private UploadFileAnalysisRecordProducer uploadFileAnalysisRecordProducer;
//    @Resource
//    private VideoAnalysisRecordService videoAnalysisRecordService;
//    @Resource
//    private UploadFileAnalysisRecordService uploadFileAnalysisRecordService;
//    int totalCount = 0;
//    int existCount = 0;
//    int notDataCount = 0;
//    int writeCount = 0;
//
//    @Resource
//    private TencentCosUtils tencentCosUtils;
//    @Resource
//    private TencentAudioUtils tencentAudioUtils;
//

//    @Test
//    void test() {
//        TencentCosTokenVo cosTempTokenVo = TencentCosUtils.privateCosUploadTempToken();
//        BasicSessionCredentials cred = new BasicSessionCredentials(cosTempTokenVo.getTempSecretId(), cosTempTokenVo.getTempSecretKey(), cosTempTokenVo.getToken());
//        COSClient cosClient = new COSClient(cred, new ClientConfig(new Region(cosTempTokenVo.getRegion())));
//
//        Map<String, String> params = new HashMap<String, String>();
//        Map<String, String> headers = new HashMap<String, String>();
//
//        URL url = cosClient.generatePresignedUrl(cosTempTokenVo.getBucketName(), "analysis/video/2025-02-10/bcfbb8d9-3e4a-44dd-a173-1c48f351f621/21/0th-1-4409347900706914304.zip", null, HttpMethodName.GET, headers, params);
//        System.out.println(url.toString());
//
//        cosClient.shutdown();
//    }

//    @Test
//    void testAI(){
//    }

//    /**
//     * 测试雪花ID生成的唯一性
//     * 使用多线程并发生成千万级ID，检查是否有重复
//     */
//    @Test
//    void testSnowflakeIdUniqueness() throws InterruptedException {
//        // 线程数量
//        int threadCount = 50;
//        // 每个线程生成的ID数量
//        int idsPerThread = 200000; // 每个线程生成20万个ID，总共生成1000万个ID
//        // 总ID数量
//        int totalIds = threadCount * idsPerThread;
//        System.out.println("开始测试雪花ID生成唯一性，线程数: " + threadCount + ", 每线程ID数: " + idsPerThread + ", 总ID数: " + totalIds);
//
////    /**
////     * 更新文件
////     */
////    @Test
////    void test1() {
////        totalCount = 0;
////        existCount = 0;
////        notDataCount = 0;
////        writeCount = 0;
////
////        List<UploadFileEntity> uploadFileEntities = this.uploadFileService.list();
////        if(uploadFileEntities != null && uploadFileEntities.size() > 0) {
////
////            List<UploadFileAnalysisRecordEntity> analysisRecordEntityList = this.uploadFileAnalysisRecordService.list();
////
////            int i = 0;
////            for (UploadFileEntity uploadFileEntity : uploadFileEntities) {
////
////                boolean exist = false;
////
////                if(analysisRecordEntityList != null && analysisRecordEntityList.size() > 0) {
////                    for (UploadFileAnalysisRecordEntity uploadFileAnalysisRecordEntity : analysisRecordEntityList) {
////                        if(uploadFileAnalysisRecordEntity.getFileId().equals(uploadFileEntity.getFileId())) {
////                            exist = true;
////                            existCount ++;
////                            break;
////                        }
////                    }
////                }
////
////                if(!exist) {
////                    handleFileData(uploadFileEntity);
////                }
////            }
////        }
////
////        System.out.println("总文件数量：" + totalCount);
////        System.out.println("新表已存在文件数量：" + existCount);
////        System.out.println("没有分析记录，放弃的文件数量：" + notDataCount);
////        System.out.println("正常写入的文件数量：" + writeCount);
////    }
////
////    /**
////     * 更新视频
////     */
////    @Test
////    void test() {
////        totalCount = 0;
////        existCount = 0;
////        notDataCount = 0;
////        writeCount = 0;
////
////        // 获取所有视频
////        List<AnchorVideoEntity> anchorVideoEntities = anchorVideoService.list();
////        if(anchorVideoEntities != null && anchorVideoEntities.size() > 0) {
////
////            totalCount = anchorVideoEntities.size();
////
////            // 获取新的分析记录列表
////            List<VideoAnalysisRecordEntity> videoAnalysisRecordEntities = this.videoAnalysisRecordService.list();
////
////            for (AnchorVideoEntity anchorVideoEntity : anchorVideoEntities) {
////
////                boolean exist = false;
////                // 判断视频是否已经写入到新表
////                if(videoAnalysisRecordEntities != null && videoAnalysisRecordEntities.size() > 0) {
////                    for (VideoAnalysisRecordEntity videoAnalysisRecordEntity : videoAnalysisRecordEntities) {
////                        if(videoAnalysisRecordEntity.getVideoId().equals(anchorVideoEntity.getVideoId())) {
////                            exist = true;
////                            existCount ++;
////                            break;
////                        }
////                    }
////                }
////
////                if(!exist) {
////                    handleVideoData(anchorVideoEntity);
////                }
////
////            }
////        }
////
////        System.out.println("总视频数量：" + totalCount);
////        System.out.println("新表已存在视频数量：" + existCount);
////        System.out.println("没有分析记录，放弃的视频数量：" + notDataCount);
////        System.out.println("正常写入的视频数量：" + writeCount);
////    }
////
////    @Transactional(rollbackFor = Exception.class)
////    void handleFileData(UploadFileEntity uploadFile) {
////        List<UploadFileAnalysisEntity> analysisEntities = this.uploadFileAnalysisService.list(new QueryWrapper<UploadFileAnalysisEntity>().eq("file_id", uploadFile.getFileId()));
////        if(analysisEntities != null && analysisEntities.size() > 0) {
////            analysisEntities.sort(Comparator.comparingInt(UploadFileAnalysisEntity::getVersion));
////
////            // 取出所有版本号
////            Set<Integer> versionList = new HashSet<>();
////            for (UploadFileAnalysisEntity analysisEntity : analysisEntities) {
////                versionList.add(analysisEntity.getVersion());
////            }
////
////            for (int i = 0; i < versionList.size(); i++) {
////
////                UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo = uploadFileAnalysisRecordProducer.infoByVideoIdAndVersion(uploadFile.getFileId(), i);
////
////                if(uploadFileAnalysisRecordInfoVo == null) {
////                    // 当前版本号的分析记录
////                    List<UploadFileAnalysisEntity> currentVersionAnalysisList = new LinkedList<>();
////                    for (UploadFileAnalysisEntity analysisEntity : analysisEntities) {
////                        if(analysisEntity.getVersion() == i) {
////                            currentVersionAnalysisList.add(analysisEntity);
////                        }
////                    }
////
////                    List<SentenceMarkVo> sentenceMarkVoList = new LinkedList<>();
////                    for (UploadFileAnalysisEntity analysisEntity : currentVersionAnalysisList) {
////                        sentenceMarkVoList.add(JSON.parseObject(analysisEntity.getDataJson(), SentenceMarkVo.class));
////                    }
////
////                    String jsonString = JSON.toJSONString(sentenceMarkVoList);
////
////                    String dateStr = uploadFile.getUploadTime().substring(0, 10);
////
////                    String tradeId = currentVersionAnalysisList.get(0).getTradeId();
////                    Long id = SnowflakeManager.nextValue();
////                    String zipFileName = i + "th-" + tradeId + "-" + id + ".zip";
////                    String storePath = wordsProperties.getFileAnalysisStorePath() + dateStr + "/" + uploadFile.getFileId() + "/";
////
////
////                    File directory = new File(storePath);
////                    if(!directory.exists()) {
////                        directory.mkdirs();
////                    }
////
////                    try(FileOutputStream fos = new FileOutputStream(storePath + zipFileName);
////                        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {
////
////                        // 创建一个新的ZIP条目
////                        ZipEntry entry = new ZipEntry("analysis.txt");
////                        zos.putNextEntry(entry);
////
////                        // 将字符串转换为字节并写入当前ZIP条目
////                        byte[] data = jsonString.getBytes("UTF-8");
////                        zos.write(data, 0, data.length);
////
////                        // 完成当前条目的写入
////                        zos.closeEntry();
////
////                    } catch (IOException e) {
////                        e.printStackTrace();
////                    }
////
////                    // 保存分析记录
////                    UploadFileAnalysisRecordBo uploadFileAnalysisRecordBo = new UploadFileAnalysisRecordBo();
////                    uploadFileAnalysisRecordBo.setId(id);
////                    uploadFileAnalysisRecordBo.setUserId(uploadFile.getUserId());
////                    uploadFileAnalysisRecordBo.setFileId(uploadFile.getFileId());
////                    uploadFileAnalysisRecordBo.setTradeId(Long.valueOf(currentVersionAnalysisList.get(0).getTradeId()));
////                    uploadFileAnalysisRecordBo.setStoreFileName(dateStr + "/" + uploadFile.getFileId() + "/" + zipFileName);
////                    uploadFileAnalysisRecordBo.setVersion(i);
////                    uploadFileAnalysisRecordProducer.save(uploadFileAnalysisRecordBo);
////                }
////
////            }
////
////            writeCount ++;
////        }else {
////            notDataCount ++;
////        }
////    }
////
////    @Transactional(rollbackFor = Exception.class)
////    void handleVideoData(AnchorVideoEntity video) {
////        // 获取视频所有分析记录
////        List<AudioAnalysisEntity> audioAnalysisEntities = this.audioAnalysisService.list(new QueryWrapper<AudioAnalysisEntity>().eq("video_id", video.getVideoId()));
////        if(audioAnalysisEntities != null && audioAnalysisEntities.size() > 0) {
////            audioAnalysisEntities.sort(Comparator.comparingInt(AudioAnalysisEntity::getVersion));
////
////            // 取出所有版本号
////            Set<Integer> versionList = new HashSet<>();
////            for (AudioAnalysisEntity audioAnalysisEntity : audioAnalysisEntities) {
////                versionList.add(audioAnalysisEntity.getVersion());
////            }
////
////            for (int i = 0; i < versionList.size(); i++) {
////
////                VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = videoAnalysisRecordProducer.infoByVideoIdAndVersion(video.getVideoId(), i);
////
////                if(videoAnalysisRecordInfoVo == null) {
////                    // 当前版本号的分析记录
////                    List<AudioAnalysisEntity> currentVersionAnalysisList = new LinkedList<>();
////                    for (AudioAnalysisEntity audioAnalysisEntity : audioAnalysisEntities) {
////                        if(audioAnalysisEntity.getVersion() == i) {
////                            currentVersionAnalysisList.add(audioAnalysisEntity);
////                        }
////                    }
////
////                    List<SentenceMarkVo> sentenceMarkVoList = new LinkedList<>();
////                    for (AudioAnalysisEntity audioAnalysisEntity : currentVersionAnalysisList) {
////                        sentenceMarkVoList.add(JSON.parseObject(audioAnalysisEntity.getDataJson(), SentenceMarkVo.class));
////                    }
////
////                    String jsonString = JSON.toJSONString(sentenceMarkVoList);
////
////                    Date createDate = video.getCreateDate();
////                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
////                    String dateStr = sdf.format(createDate);
////
////                    String tradeId = currentVersionAnalysisList.get(0).getTradeId();
////                    Long id = SnowflakeManager.nextValue();
////                    String zipFileName = i + "th-" + tradeId + "-" + id + ".zip";
////                    String storePath = wordsProperties.getVideoAnalysisStorePath() + dateStr + "/" + video.getVideoId() + "/";
////
////
////                    File directory = new File(storePath);
////                    if(!directory.exists()) {
////                        directory.mkdirs();
////                    }
////
////                    try(FileOutputStream fos = new FileOutputStream(storePath + zipFileName);
////                        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {
////
////                        // 创建一个新的ZIP条目
////                        ZipEntry entry = new ZipEntry("analysis.txt");
////                        zos.putNextEntry(entry);
////
////                        // 将字符串转换为字节并写入当前ZIP条目
////                        byte[] data = jsonString.getBytes("UTF-8");
////                        zos.write(data, 0, data.length);
////
////                        // 完成当前条目的写入
////                        zos.closeEntry();
////
////                    } catch (IOException e) {
////                        e.printStackTrace();
////                    }
////
////                    // 保存分析记录
////                    VideoAnalysisRecordBo videoAnalysisRecordBo = new VideoAnalysisRecordBo();
////                    videoAnalysisRecordBo.setId(id);
////                    videoAnalysisRecordBo.setUserId(video.getUserId());
////                    videoAnalysisRecordBo.setVideoId(video.getVideoId());
////                    videoAnalysisRecordBo.setTradeId(Long.valueOf(currentVersionAnalysisList.get(0).getTradeId()));
////                    videoAnalysisRecordBo.setStoreFileName(dateStr + "/" + video.getVideoId() + "/" + zipFileName);
////                    videoAnalysisRecordBo.setVersion(i);
////                    videoAnalysisRecordProducer.save(videoAnalysisRecordBo);
////                }
////
////            }
////
////            writeCount ++;
////        }else {
////            notDataCount ++;
////        }
////    }
////    @Test
////    public void test() {
////        Set<Long> set = new HashSet<>();
////        for (int i = 0; i < 10000000; i++) {
////            Long nextValue = SnowflakeManager.nextValue();
////            if (!set.add(nextValue)) {
////                System.out.println("添加雪花id失败存在重复数据" + nextValue);
////            }
////        }
////        System.out.println("添加完成未存在重复雪花id");
////    }
//        // 使用线程安全的HashSet存储生成的ID
//        Set<Long> idSet = Collections.synchronizedSet(new HashSet<>(totalIds));
//        // 使用CountDownLatch等待所有线程完成
//        CountDownLatch latch = new CountDownLatch(threadCount);
//        // 标记是否有重复ID
//        AtomicBoolean hasDuplicate = new AtomicBoolean(false);
//        // 计数器，用于输出进度
//        AtomicInteger counter = new AtomicInteger(0);
//        //IdWorker.getId();
//
//        // 创建线程池
//        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
//
//        long startTime = System.currentTimeMillis();
//        // 启动多个线程并发生成ID
//        for (int i = 0; i < threadCount; i++) {
//            executor.execute(() -> {
//                try {
//                    for (int j = 0; j < idsPerThread; j++) {
//                        Long id = SnowflakeManager.nextValue();
//                        if (j == 0) {
//                            System.out.println(id);
//                        }
//                        if (!idSet.add(id)) {
//                            // 如果添加失败，说明ID重复
//                            System.err.println("发现重复ID: " + id);
//                            hasDuplicate.set(true);
//                        }
//
//                        // 每生成100万个ID输出一次进度
//                        int currentCount = counter.incrementAndGet();
//                        if (currentCount % 1000000 == 0) {
//                            System.out.println("已生成ID数量: " + currentCount + ", 当前Set大小: " + idSet.size());
//                        }
//                    }
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        // 等待所有线程完成
//        latch.await();
//
//        long endTime = System.currentTimeMillis();
//        long duration = endTime - startTime;
//
//        // 关闭线程池
//        executor.shutdown();
//
//        // 输出结果
//        System.out.println("ID生成完成，总耗时: " + duration + "ms");
//        System.out.println("预期生成ID数量: " + totalIds);
//        System.out.println("实际生成唯一ID数量: " + idSet.size());
//        System.out.println("是否存在重复ID: " + hasDuplicate.get());
//
//        if (hasDuplicate.get()) {
//            System.err.println("测试失败：存在重复ID");
//        } else {
//            System.out.println("测试通过：所有ID都是唯一的");
//        }
//
//        // 计算每秒生成ID数量
//        double idsPerSecond = (double) totalIds / (duration / 1000.0);
//        System.out.println("每秒生成ID数量: " + String.format("%.2f", idsPerSecond));
//    }

//    /**
//     * 使用批量生成方法测试雪花ID
//     * 对比单个生成和批量生成的性能
//     */
//    @Test
//    void testSnowflakeIdBatchGeneration() {
//        // 测试次数
//        int testCount = 10;
//        // 每次生成的ID数量
//        int batchSize = 10000000; // 1000万
//
//        System.out.println("开始测试雪花ID批量生成性能，批量大小: " + batchSize);
//
//        // 测试单个生成方法
//        long singleStartTime = System.currentTimeMillis();
//        Set<Long> singleIds = new HashSet<>(batchSize);
//
//        for (int i = 0; i < batchSize; i++) {
//            Long id = SnowflakeManager.nextValue();
//            if (!singleIds.add(id)) {
//                System.err.println("单个生成方法发现重复ID: " + id);
//            }
//        }
//
//        long singleEndTime = System.currentTimeMillis();
//        long singleDuration = singleEndTime - singleStartTime;
//
//        System.out.println("单个生成方法完成，耗时: " + singleDuration + "ms");
//        System.out.println("单个生成方法唯一ID数量: " + singleIds.size());
//        System.out.println("单个生成方法每秒生成速率: " + String.format("%.2f", singleIds.size() / (singleDuration / 1000.0)));
//
//        // 测试批量生成方法
//        long batchStartTime = System.currentTimeMillis();
//        Set<Long> batchIds = new HashSet<>(batchSize);
//
//        for (int i = 0; i < 2442; i++) {
//            List<Long> ids = SnowflakeManager.nextBatch(4096);
//            for (Long id : ids) {
//                if (!batchIds.add(id)) {
//                    System.err.println("批量生成方法发现重复ID: " + id);
//                }
//            }
//        }
//        long batchEndTime = System.currentTimeMillis();
//        long batchDuration = batchEndTime - batchStartTime;
//
//        System.out.println("批量生成方法完成，耗时: " + batchDuration + "ms");
//        System.out.println("批量生成方法唯一ID数量: " + batchIds.size());
//        System.out.println("批量生成方法每秒生成速率: " + String.format("%.2f", batchIds.size() / (batchDuration / 1000.0)));
//
//        // 对比性能提升
//        double speedup = (double) singleDuration / batchDuration;
//        System.out.println("批量生成相比单个生成性能提升: " + String.format("%.2f", speedup) + "倍");
//    }

}
