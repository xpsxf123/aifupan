using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Ai.config;
using ReviewAnalysis.Ai.impl.diagnosis;
using ReviewAnalysis.Ai.impl;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.api;
using ReviewAnalysis.bo.diagnosis;
using ReviewAnalysis.Dto;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.diagnosis;
using System.Threading;
using ReviewAnalysis.Ai.auto;
using ReviewAnalysis.enums;
using ReviewAnalysis.vo.system;
using ReviewAnalysis.vo.video;

namespace ReviewAnalysis.Ai
{
    public class DiagnosisAuto
    {

        public static volatile ThreadSafeList<AiAutoTimerDto> dataQueue = new ThreadSafeList<AiAutoTimerDto>();

        // 最大并发线程数，默认值Default value
        private static int maxConcurrentTasks = 1;

        // 用于控制并发任务数的信号量
        private static SemaphoreSlim semaphore = new SemaphoreSlim(maxConcurrentTasks);
        private static readonly object _lockAdd = new object();
        private static readonly object _lockStartOne = new object();

        /// <summary>
        /// 把全部的待分析和分析中的诊断问题重新set到队列中
        /// </summary>
        /// <returns></returns>
        public static async Task initPut()
        {
            try
            {
                // 添加服务器
                List<DiagnosisCueVo> diagnosisCueList = await DiagnosisApi.handleDiagnosisByUser();

                // 在添加到队列中
                addAll(diagnosisCueList);
            }
            catch (Exception e)
            {
                FileUtils.LogError($"msg = {e.Message}", "诊断报告初始化添加队列失败");
            }
            
        }

        /// <summary>
        /// 视频分析完成后，自动添加诊断问答
        /// </summary>
        /// <param name="video"></param>
        /// <returns></returns>
        public static async Task generateDiagnosis(VideoEntity video)
        {
            try
            {
                List<DiagnosisCueVo> diagnosisCueList = await DiagnosisApi.getAutoDiagnosisQuestions(video.videoId);
                FileUtils.log(JsonConvert.SerializeObject(diagnosisCueList), "视频分析完成后，自动添加诊断问答");
                addAll(diagnosisCueList, true);
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"message={ex.Message}, 堆栈={ex.StackTrace}", "视频分析完成后添加自动分析队列失败");
            }
        }

        /// <summary>
        /// 诊断分析-提示词添加分析
        /// </summary>
        /// <param name="bo"></param>
        public static void addAutoAsk(SaveDiagnosisBo bo)
        {
            // 添加服务器
            List<DiagnosisCueVo> diagnosisCueList = DiagnosisApi.saveDiagnosis(bo);

            // 在添加到队列中
            addAll(diagnosisCueList);
        }

        /// <summary>
        /// 诊断分析-提示词添加分析
        /// </summary>
        /// <param name="diagnosisCueList"></param>
        private static void addAll(List<DiagnosisCueVo> diagnosisCueList, bool isAuto = false)
        {
            try
            {
                if (diagnosisCueList != null && diagnosisCueList.Count > 0)
                {
                    foreach (DiagnosisCueVo item in diagnosisCueList)
                    {
                        AiAutoTimerDto vo = new AiAutoTimerDto();
                        vo.id = item.id ?? 0;
                        vo.sourceId = item.sourceId;
                        vo.sourceType = 0;
                        vo.cueWordsId = (item.cueWordsId ?? 0).ToString();
                        vo.cueType = item.cueType ?? 0;
                        vo.isAuto = isAuto;
                        vo.diagnosisType = item.diagnosisType ?? 0;
                        vo.uploadScreenshot = item.selectDataScreenshot ?? 1;
                        vo.uploadBoard = item.selectBoard ?? 1;
                        dataQueue.Add(vo);
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"message={ex.Message}, 堆栈={ex.StackTrace}", "批量诊断问题添加自动分析队列失败");
            }
        }

        public static async Task startOne()
        {
            AiAutoTimerDto aiAutoTimer = null;
            // 没有并发的问题，就谁便锁下
            lock (_lockStartOne)
            {
                if (!dataQueue.IsNotEmpty())
                {
                    return;
                }

                // 判断是否有要执行的数据
                List<AiAutoTimerDto> aiAutoTimerList = dataQueue.GetAll();
                if (aiAutoTimerList == null || aiAutoTimerList.Count <= 0)
                {
                    return;
                }
                IEnumerable<AiAutoTimerDto> enumerable = aiAutoTimerList.Where(item => item.status == 0);
                if (enumerable == null || enumerable.Count() <= 0)
                {
                    return;
                }
                aiAutoTimer = enumerable.First();

                if (aiAutoTimer == null)
                {
                    return;
                }
            }

            if (aiAutoTimer == null)
            {
                return;
            }


            // 尝试等待信号量，确保不超过最大并发数。如果信号量已满，则直接返回，不等待
            bool acquired = await semaphore.WaitAsync(0);
            if (!acquired)
            {
                // 信号量已满，当前不处理，直接返回
                //FileUtils.LogError("信号量已满，当前并发数已达上限，跳过本次处理", "AI处理并发控制");
                return;
            }
            FileUtils.log($"aiAutoTimer = {JsonConvert.SerializeObject(aiAutoTimer)}", "执行诊断报告生成");

            try
            {
                // 启动一个任务处理项目
                await Task.Run(async () =>
                {
                    // 修改队列中对应元素的状态为1（处理中）
                    dataQueue.Update(v => v.id == aiAutoTimer.id, item => item.status = 1);
                    try
                    {
                        await DiagnosisAuto.runAsk(aiAutoTimer);
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError($"处理项目时出错: {ex.Message}, 堆栈={ex.StackTrace}", "AI处理错误");
                    }
                    finally
                    {
                        // 释放信号量，允许新的任务启动
                        semaphore.Release();
                    }
                });
            }
            catch (Exception ex)
            {
                semaphore.Release();
                FileUtils.LogError($"启动处理任务时出错: {ex.Message}, 堆栈={ex.StackTrace}", "AI处理错误");
            }
            FileUtils.log($"aiAutoTimer = {JsonConvert.SerializeObject(aiAutoTimer)}", "诊断报告启动完成");

        }

        /// <summary>
        /// 诊断提问
        /// </summary>
        /// <param name="dto"></param>
        /// <returns></returns>
        public static async Task runAsk(AiAutoTimerDto dto)
        {
            string errorStr = "";
            bool isSuccess = false;
            try
            {
                FileUtils.log(dto.sourceId, "开始提问");
                await DiagnosisApi.updateDiagnosisCueStatus(dto.id, 1, null);
                // 获取参数类
                AbstractDiagnosis diagnosis = AiFactory.GetDiagnosis(dto.cueType);
                AskRequestDto temp = await diagnosis.getParams(dto);

                AiTempTokenDto token = AiRelatedApi.getAnalysisTempToken(temp.aiModel);

                if (token == null)
                {
                    throw new CustomException("获取临时token失败", 7006);
                }

                temp.resourceType = token.resourceType;
                temp.useModelType = token.useModelWay;
                temp.token = token;

                Ask ask = AiFactory.GetAsk(temp.resourceType);
                temp.propertyDeductRemarks = "诊断报告";
                temp.uploadScreenshot = dto.uploadScreenshot;
                temp.uploadBoard = dto.uploadBoard;
                AskResponseDto result = await ask.AskStream(temp, null, null);
                FileUtils.log(dto.sourceId, "提问完成");
                
                // 纠正内容
                aiContentCorrect(result);

                if (result != null)
                {
                    await DiagnosisApi.updateDiagnosisCueStatus(dto.id, 2, null);
                    isSuccess = true;
                }
                else
                {
                    // 失败
                    await DiagnosisApi.updateDiagnosisCueStatus(dto.id, 3, AbstractAsk.errorReturnData);
                }
            }
            catch (CustomException ex)
            {
                FileUtils.LogError(ex.Message, "ai诊断自动问答报错");
                // 失败
                await DiagnosisApi.updateDiagnosisCueStatus(dto.id, 3, ex.Message);
            }
            catch (Exception ex)
            {
                FileUtils.LogError(ex.Message, "ai诊断自动问答报错");
                // 失败
                await DiagnosisApi.updateDiagnosisCueStatus(dto.id, 3, ex.Message);
            }
            finally
            {
                try
                {
                    if (isSuccess)
                    {
                        // 判断是否要自动生成诊断报告
                        List<AiAutoTimerDto> list = dataQueue.GetAll();
                        List<AiAutoTimerDto> enumerable = list.Where(x => x.sourceId.Equals(dto.sourceId) && x.diagnosisType == dto.diagnosisType).ToList();
                        if (dto.isAuto && (enumerable == null || enumerable.Count <= 1))
                        {
                            // 查看服务器是否有生成诊断报告
                            if (DiagnosisApi.isGenerateDiagnosisFile(dto))
                            {
                                FileUtils.log(dto.sourceId, "要自动生成报告");

                                VideoEntity videoEntity = VideoApi.GetVideoByVideoIdSync(dto.sourceId);

                                List<DiagnosisCueVo> diaList = DiagnosisApi.getHandelSuccessDiagnosis(dto);

                                List<string> list1 = diaList
                                    .Select(dia => dia.cueWordsId == null ? null : dia.cueWordsId.ToString())
                                    .Where(dia => !string.IsNullOrEmpty(dia))
                                    .ToList();

                                noticeFrontGenerateDiagnosis(dto.sourceId, videoEntity.videoName, list1, "1", dto.diagnosisType);
                            }
                        }
                    }
                    // 删除
                    dataQueue.DeleteIf(v => v.id == dto.id);
                }
                catch (Exception ex)
                {
                    FileUtils.LogError(ex.Message, "ai诊断自动问答完成后报错");
                }
            }
        }

        /// <summary>
        /// ai内容纠正
        /// </summary>
        /// <param name="result"></param>
        public static async Task aiContentCorrect(AskResponseDto result)
        {
            // 判断是否需要纠正内容
            if (result == null || result.checkAiContent || result.answer == null || result.answer.id == null || string.IsNullOrEmpty(result.answer.content))
            {
                return;
            }

            int status = 0;
            string error = "";
            string correctedContent = "";
            try
            {
                correctedContent = await CorrectAiContentAuto.generateAiContentSplice(result.answer.id, (int)AiUseSourceType.ai问答, result.answer.content, (int)ContentCorrectionSceneType.AI问答助手的纠正);
                status = 2;
            }
            catch (Exception ex)
            {
                status = 3;
                FileUtils.LogError($"run方法异常: {ex.Message}, 堆栈={ex.StackTrace}", "纠正AI内容处理错误");
                error = ex.Message;
            }
            finally
            {
                // 调用更新接口，传入纠正后的内容
                AiRelatedApi.updateCorrectStatus(result.answer.id, status, correctedContent, error);
                
                // 把纠正后的内容覆盖到result中
                result.answer.content =  correctedContent;
            }
        }

        /// <summary>
        /// 通知前端生成诊断报告
        /// </summary>
        /// <param name="videoId"></param>
        /// <param name="cueWordsIds"></param>
        /// <returns></returns>
        public static async Task noticeFrontGenerateDiagnosis(String videoId, string videoName, List<string> cueWordsIds, string tradeId, int? diagnosisType = 0)
        {

            // 通知前端弹窗
            try
            {
                Dictionary<string, object> data = new Dictionary<string, object>();
                data.Add("videoId", videoId);
                data.Add("diagnosisOssName", videoName);
                data.Add("cueWordsIds", cueWordsIds);
                data.Add("tradeId", tradeId);
                data.Add("diagnosisType", diagnosisType);
                FileUtils.log(JsonConvert.SerializeObject(data), "通知前端生成报告");
                await FrontNotice.NoticeFront("generateDiagnosis", data);
                FileUtils.log("通知前端成功");
            }
            catch (Exception e)
            {
                FileUtils.LogError($"{e}", $"测试通知前端打开更新弹窗发生异常");
            }
        }

        /// <summary>
        /// 更新最大并发任务数
        /// </summary>
        public static async Task UpdateMaxConcurrentTasks()
        {
            try
            {
                int maxConcurrentTasks = DiagnosisAuto.maxConcurrentTasks;
                SystemKvVo systemKvVo = await SystemApi.getSystenKvByKeyAsync("diagnosis_concurrency_level");
                if (systemKvVo != null)
                {
                    // 设置值
                    int.TryParse(systemKvVo.kvValue, out maxConcurrentTasks);
                }

                // 如果已经初始化了信号量，则更新它
                if (semaphore != null)
                {
                    int currentCount = DiagnosisAuto.maxConcurrentTasks - semaphore.CurrentCount;
                    var newSemaphore = new SemaphoreSlim(maxConcurrentTasks);

                    // 释放相同数量的许可
                    if (currentCount > 0)
                    {
                        for (int i = 0; i < currentCount; i++)
                        {
                            newSemaphore.Wait(0);
                        }
                    }
                    DiagnosisAuto.maxConcurrentTasks = maxConcurrentTasks;
                    var oldSemaphore = semaphore;
                    semaphore = newSemaphore;
                    oldSemaphore.Dispose();
                }
                else
                {
                    semaphore = new SemaphoreSlim(maxConcurrentTasks);
                }
            }
            catch(Exception ex)
            {
                FileUtils.LogError(ex.Message, "获取diagnosis_concurrency_level键失败");
            }
            
        }

    }
}
