package com.agileboot.domain.social.clouddrive;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.agileboot.domain.social.post.db.SocialSyncPostEntity;
import com.agileboot.domain.social.post.db.SocialSyncPostService;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 阿里云盘文件同步——从 alist 获取目录文件列表，将新视频写入 social_sync_post。
 *
 * @author SocialMedia-Hub
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudDriveSyncService {

    private static final String PLATFORM = "aliyun";
    /** 视频文件扩展名 */
    private static final java.util.Set<String> VIDEO_EXTS = CollUtil.newHashSet(
        "mp4", "mov", "avi", "mkv", "flv", "wmv", "webm", "m4v", "ts", "rmvb"
    );

    private final AlistProxyService alistProxyService;
    private final SocialSyncPostService postService;

    /**
     * 同步指定 alist 路径下的新视频文件到 social_sync_post。
     *
     * @param alistPath alist 挂载路径，如 /aliyun/羽毛球/吊球
     * @return 新同步的视频数量
     */
    public int syncFromAlist(String alistPath) {
        log.info("开始同步阿里云盘目录: {}", alistPath);
        List<AlistFileInfo> allFiles = listAllFiles(alistPath);
        int synced = 0;
        for (AlistFileInfo file : allFiles) {
            if (file.getIsDir()) {
                continue;
            }
            String ext = getFileExt(file.getName());
            if (!VIDEO_EXTS.contains(ext)) {
                continue;
            }
            if (savePost(file, alistPath)) {
                synced++;
            }
        }
        log.info("阿里云盘目录 {} 同步完成，共 {} 个文件，新同步 {} 个视频",
            alistPath, allFiles.size(), synced);
        return synced;
    }

    /**
     * 同步勾选的指定文件路径到 social_sync_post（仅视频）。
     *
     * @param paths alist 文件完整路径列表
     * @return 新同步的视频数量
     */
    public int syncSelectedFiles(List<String> paths) {
        if (CollUtil.isEmpty(paths)) {
            return 0;
        }
        log.info("开始同步阿里云盘勾选文件: {} 个", paths.size());
        int synced = 0;
        for (String path : paths) {
            if (StrUtil.isBlank(path)) {
                continue;
            }
            AlistFileInfo file = alistProxyService.getFile(path);
            if (file == null || Boolean.TRUE.equals(file.getIsDir())) {
                continue;
            }
            String ext = getFileExt(file.getName());
            if (!VIDEO_EXTS.contains(ext)) {
                continue;
            }
            String basePath = path.substring(0, path.lastIndexOf('/'));
            if (savePost(file, basePath)) {
                synced++;
            }
        }
        log.info("阿里云盘勾选文件同步完成，选中 {} 个，新同步 {} 个视频", paths.size(), synced);
        return synced;
    }

    private List<AlistFileInfo> listAllFiles(String path) {
        List<AlistFileInfo> allFiles = new ArrayList<>();
        int page = 1;
        AlistListResult result;
        do {
            result = alistProxyService.listFiles(path, page, 50);
            if (result.getFiles() != null) {
                allFiles.addAll(result.getFiles());
            }
            page++;
        } while (result.isHasMore());
        return allFiles;
    }

    private boolean savePost(AlistFileInfo file, String basePath) {
        String platformPostId = file.getPath();
        if (StrUtil.isBlank(platformPostId)) {
            platformPostId = basePath + "/" + file.getName();
        }
        boolean exists = postService.lambdaQuery()
            .eq(SocialSyncPostEntity::getPlatform, PLATFORM)
            .eq(SocialSyncPostEntity::getPlatformPostId, platformPostId)
            .count() > 0;
        if (exists) {
            return false;
        }

        SocialSyncPostEntity post = new SocialSyncPostEntity();
        post.setPlatform(PLATFORM);
        post.setPlatformPostId(platformPostId);
        post.setTitle(stripExt(file.getName()));
        post.setPostType(2);  // 视频
        post.setAudioStatus(1);  // 待转写
        // video_url 存本地 alist 直链，供本机 worker 下载
        post.setVideoUrl(alistProxyService.getDownloadUrl(platformPostId));
        post.setCoverUrl(file.getThumb());
        if (StrUtil.isNotBlank(file.getModified())) {
            try {
                post.setPublishedAt(DateUtil.parse(file.getModified()));
            } catch (Exception ignored) {
                post.setPublishedAt(new Date());
            }
        } else {
            post.setPublishedAt(new Date());
        }
        post.setSyncedAt(new Date());
        post.setStatus(1);
        post.setCreatorId(1L);
        post.setUpdaterId(1L);
        postService.save(post);
        log.debug("新增阿里云盘视频: {}", post.getTitle());
        return true;
    }

    private String getFileExt(String name) {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
    }

    private String stripExt(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
