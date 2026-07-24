package com.agileboot.domain.social.client.bili.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

/**
 * B站空间投稿视频列表。
 *
 * @author SocialMedia-Hub
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BiliSpaceVideoListData {

    private SpaceList list;

    private SpacePage page;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SpaceList {

        /** 视频条目列表 */
        @JsonProperty("vlist")
        private List<BiliSpaceVideoItem> vlist;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SpacePage {

        /** 当前页码 */
        private Integer pn;

        /** 每页条数 */
        private Integer ps;

        /** 总数 */
        private Integer count;
    }

}
