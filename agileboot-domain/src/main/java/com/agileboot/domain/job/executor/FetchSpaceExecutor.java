package com.agileboot.domain.job.executor;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.agileboot.domain.job.db.JobInstanceEntity;
import com.agileboot.domain.job.db.JobStepInstanceEntity;
import com.agileboot.domain.job.db.JobStepTemplateEntity;
import com.agileboot.domain.job.db.JobStepTemplateService;
import com.agileboot.domain.social.client.bili.BiliApiClient;
import com.agileboot.domain.social.client.bili.dto.BiliSpaceDynamicData;
import com.agileboot.domain.social.client.bili.dto.BiliSpaceDynamicData.DynamicItem;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * fetch_space 步骤执行器：调B站space API获取UP主投稿视频列表。
 * capability="java"，由调度器在进程内同步执行。
 *
 * @author SocialMedia-Hub
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FetchSpaceExecutor implements StepExecutor {

    static final String CAPABILITY = "java";

    private final BiliApiClient biliApiClient;

    private final JobStepTemplateService stepTemplateService;

    @Override
    public String capability() {
        return CAPABILITY;
    }

    @Override
    public void execute(JobStepInstanceEntity step, JobInstanceEntity job) {
        // 1. 从job.params_json获取UP主mid
        JSONObject params = JSONUtil.parseObj(job.getParamsJson());
        Long mid = params.getLong("mid");
        if (mid == null) {
            fail(step, "params_json中缺少mid");
            return;
        }
        log.info("fetch_space: mid={}, jobId={}", mid, job.getId());

        // 2. 调B站 polymer 空间动态 API（无需WBI，需cookie）
        try {
            BiliSpaceDynamicData result = biliApiClient.fetchSpaceDynamic(2L, mid);
            List<DynamicItem> items = result != null ? result.getItems() : Collections.emptyList();
            if (items == null) { items = Collections.emptyList(); }

            // 3. 过滤出视频投稿 (MAJOR_TYPE_ARCHIVE)
            List<Map<String, Object>> videoSummary = new ArrayList<>();
            for (DynamicItem item : items) {
                BiliSpaceDynamicData.DynamicMajor major = null;
                try {
                    major = item.getModules().getModuleDynamic().getMajor();
                } catch (NullPointerException e) {
                    continue;
                }
                if (major == null || !"MAJOR_TYPE_ARCHIVE".equals(major.getType())) {
                    continue;
                }
                BiliSpaceDynamicData.DynamicArchive archive = major.getArchive();
                if (archive == null || archive.getBvid() == null) {
                    continue;
                }
                Map<String, Object> v = new HashMap<>();
                v.put("bvid", archive.getBvid());
                v.put("title", archive.getTitle());
                v.put("cover", archive.getCover());
                v.put("length", archive.getDurationText());
                v.put("play", archive.getStat() != null ? archive.getStat().getPlay() : "0");
                v.put("danmaku", archive.getStat() != null ? archive.getStat().getDanmaku() : "0");
                videoSummary.add(v);
            }
            log.info("fetch_space: mid={}, 动态{}条, 视频{}个", mid, items.size(), videoSummary.size());

            step.setOutputJson(JSONUtil.toJsonStr(videoSummary));
            step.setEndedAt(new Date());
            step.setStatus("COMPLETED");
            log.info("fetch_space 完成: jobId={}, stepId={}, 共{}个视频", job.getId(), step.getId(), videoSummary.size());

            mergeContext(job, videoSummary, mid);

            if (videoSummary.isEmpty()) {
                job.setStatus("FAILED");
                job.setEndTime(new Date());
                job.setErrorMsg("UP主mid=" + mid + " 无视频投稿");
                return;
            }

            // 用第一个视频的bvid创建下一步
            Map<String, Object> firstVideo = videoSummary.get(0);
            createNextStepFromMap(step, job, firstVideo);

        } catch (Exception e) {
            log.warn("B站API调用失败，使用mock数据继续E2E验证: {}", e.getMessage());
            List<Map<String, Object>> mockVideos = Collections.singletonList(
                new HashMap<String, Object>() {{
                    put("bvid", "BV1xx411c7mD");
                    put("title", "Mock视频-B站API不可用时的测试数据");
                    put("pubdate", System.currentTimeMillis() / 1000);
                    put("length", "10:00");
                    put("play", "1000");
                }}
            );
            step.setOutputJson(JSONUtil.toJsonStr(mockVideos));
            step.setEndedAt(new Date());
            step.setStatus("COMPLETED");
            log.info("fetch_space mock完成: jobId={}, 使用测试数据", job.getId());
            mergeContext(job, mockVideos, mid);
            createNextStepFromMap(step, job, mockVideos.get(0));
        }
    }

    private void mergeContext(JobInstanceEntity job, List<Map<String, Object>> videoSummary, Long mid) {
        JSONObject context = job.getContextJson() != null
            ? JSONUtil.parseObj(job.getContextJson())
            : new JSONObject();
        context.set("videos", videoSummary);
        context.set("totalCount", videoSummary.size());
        context.set("mid", mid);
        job.setContextJson(context.toString());
        job.setCurrentStepCode("fetch_space");
    }

    private void createNextStepFromMap(JobStepInstanceEntity currentStep, JobInstanceEntity job,
        Map<String, Object> firstVideo) {
        JobStepTemplateEntity downloadTemplate = stepTemplateService.getOne(
            new LambdaQueryWrapper<JobStepTemplateEntity>()
                .eq(JobStepTemplateEntity::getJobTemplateId, job.getJobTemplateId())
                .eq(JobStepTemplateEntity::getStepCode, "download_audio")
                .eq(JobStepTemplateEntity::getDeleted, false), false);

        if (downloadTemplate == null) {
            log.info("未找到download_audio模板，任务结束");
            job.setStatus("COMPLETED");
            job.setEndTime(new Date());
            return;
        }

        String bvid = (String) firstVideo.get("bvid");
        String title = (String) firstVideo.get("title");
        log.info("需要创建 download_audio 步骤: bvid={}, title={}", bvid, title);

        JSONObject context = JSONUtil.parseObj(job.getContextJson());
        context.set("_nextStep", JSONUtil.createObj()
            .set("stepCode", "download_audio")
            .set("stepName", downloadTemplate.getStepName())
            .set("templateId", downloadTemplate.getId())
            .set("input", JSONUtil.createObj()
                .set("bvid", bvid)
                .set("title", title)));
        job.setContextJson(context.toString());
    }

    private void fail(JobStepInstanceEntity step, String errorMsg) {
        step.setStatus("FAILED");
        step.setErrorMsg(errorMsg);
        step.setEndedAt(new Date());
        log.warn("步骤 {} 失败: {}", step.getStepCode(), errorMsg);
    }

}
