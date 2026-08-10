package com.agileboot.domain.social.clouddrive;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode;
import com.agileboot.domain.social.config.SocialMediaProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * alist API 代理——封装对 alist 的 HTTP 调用，供前端浏览目录和后端同步使用。
 *
 * @author SocialMedia-Hub
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlistProxyService {

    private final SocialMediaProperties properties;

    /**
     * 列出指定目录下的文件和子目录。
     */
    public AlistListResult listFiles(String path, int page, int perPage) {
        String alistUrl = properties.getCloudDrive().getAlistUrl();
        String url = alistUrl + "/api/fs/list";

        JSONObject body = JSONUtil.createObj()
            .set("path", path)
            .set("password", "")
            .set("page", page)
            .set("per_page", Math.min(perPage, 100));

        String respBody;
        try (HttpResponse response = HttpRequest.post(url)
            .body(body.toString())
            .header("Content-Type", "application/json")
            .header("Authorization", properties.getCloudDrive().getAlistToken())
            .timeout(properties.getCloudDrive().getTimeoutMs())
            .execute()) {
            respBody = response.body();
        }

        JSONObject resp = JSONUtil.parseObj(respBody);
        int code = resp.getInt("code", -1);
        if (code != 200) {
            String msg = resp.getStr("message", "unknown error");
            log.error("alist list files failed: path={}, code={}, msg={}", path, code, msg);
            throw new ApiException(ErrorCode.FAILED, "alist 列出目录失败: " + msg);
        }

        JSONObject data = resp.getJSONObject("data");
        JSONArray content = data.getJSONArray("content");
        int total = data.getInt("total", 0);
        boolean hasMore = data.getBool("has_more", false);

        List<AlistFileInfo> files = new ArrayList<>();
        if (content != null) {
            for (int i = 0; i < content.size(); i++) {
                JSONObject item = content.getJSONObject(i);
                AlistFileInfo info = new AlistFileInfo();
                info.setName(item.getStr("name"));
                info.setPath(item.getStr("virtual_path") != null
                    ? item.getStr("virtual_path") : path + "/" + item.getStr("name"));
                info.setSize(item.getLong("size", 0L));
                info.setIsDir(item.getBool("is_dir", false));
                info.setModified(item.getStr("modified"));
                info.setType(item.getInt("type", 0));
                info.setThumb(item.getStr("thumb"));
                files.add(info);
            }
        }

        AlistListResult result = new AlistListResult();
        result.setFiles(files);
        result.setTotal(total);
        result.setHasMore(hasMore);
        return result;
    }

    /**
     * 获取文件详情（含下载直链）。
     */
    public AlistFileInfo getFile(String path) {
        String alistUrl = properties.getCloudDrive().getAlistUrl();
        String url = alistUrl + "/api/fs/get";

        JSONObject body = JSONUtil.createObj()
            .set("path", path)
            .set("password", "");

        String respBody;
        try (HttpResponse response = HttpRequest.post(url)
            .body(body.toString())
            .header("Content-Type", "application/json")
            .header("Authorization", properties.getCloudDrive().getAlistToken())
            .timeout(properties.getCloudDrive().getTimeoutMs())
            .execute()) {
            respBody = response.body();
        }

        JSONObject resp = JSONUtil.parseObj(respBody);
        int code = resp.getInt("code", -1);
        if (code != 200) {
            String msg = resp.getStr("message", "unknown error");
            throw new ApiException(ErrorCode.FAILED, "alist 获取文件详情失败: " + msg);
        }

        JSONObject data = resp.getJSONObject("data");
        AlistFileInfo info = new AlistFileInfo();
        info.setName(data.getStr("name"));
        info.setPath(path);
        info.setSize(data.getLong("size", 0L));
        info.setIsDir(data.getBool("is_dir", false));
        info.setModified(data.getStr("modified"));
        info.setType(data.getInt("type", 0));
        info.setRawUrl(data.getStr("raw_url"));
        info.setThumb(data.getStr("thumb"));
        return info;
    }

    /**
     * 获取文件下载直链（供本机 worker 使用，返回 localhost 地址）。
     */
    public String getDownloadUrl(String path) {
        AlistFileInfo file = getFile(path);
        if (file.getRawUrl() != null && !file.getRawUrl().isEmpty()) {
            return file.getRawUrl();
        }
        String localAlistUrl = properties.getCloudDrive().getLocalAlistUrl();
        if (StrUtil.isBlank(localAlistUrl)) {
            localAlistUrl = properties.getCloudDrive().getAlistUrl();
        }
        return localAlistUrl + "/d" + path;
    }
}
