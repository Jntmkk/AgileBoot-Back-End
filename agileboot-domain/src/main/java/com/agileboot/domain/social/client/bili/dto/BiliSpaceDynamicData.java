package com.agileboot.domain.social.client.bili.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

/**
 * B站 polymer web-dynamic 空间动态响应。
 * 端点: x/polymer/web-dynamic/v1/feed/space
 *
 * @author SocialMedia-Hub
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BiliSpaceDynamicData {

    @JsonProperty("has_more")
    private Boolean hasMore;

    private List<DynamicItem> items;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DynamicItem {

        @JsonProperty("id_str")
        private String idStr;

        private DynamicModules modules;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DynamicModules {

        @JsonProperty("module_dynamic")
        private ModuleDynamic moduleDynamic;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModuleDynamic {

        private DynamicMajor major;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DynamicMajor {

        /** MAJOR_TYPE_ARCHIVE=视频, MAJOR_TYPE_OPUS=图文, MAJOR_TYPE_DRAW=相簿 */
        private String type;

        /** 视频投稿信息（type=MAJOR_TYPE_ARCHIVE 时存在） */
        private DynamicArchive archive;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DynamicArchive {

        private String bvid;

        private String title;

        private String cover;

        @JsonProperty("duration_text")
        private String durationText;

        private ArchiveStat stat;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ArchiveStat {

        private String play;

        @JsonProperty("danmaku")
        private String danmaku;
    }

}
