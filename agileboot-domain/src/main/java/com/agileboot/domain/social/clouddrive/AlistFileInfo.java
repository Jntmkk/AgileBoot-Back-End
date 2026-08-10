package com.agileboot.domain.social.clouddrive;

import lombok.Data;

/**
 * alist 文件/目录信息。
 */
@Data
public class AlistFileInfo {
    private String name;
    private String path;
    private Long size;
    private Boolean isDir;
    private String modified;
    /** alist 文件类型：1目录 2图片 3视频 4文档 5音频 6其他 */
    private Integer type;
    private String thumb;
    /** 下载直链 */
    private String rawUrl;
}
