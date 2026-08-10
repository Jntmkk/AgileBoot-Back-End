package com.agileboot.domain.social.clouddrive;

import java.util.List;
import lombok.Data;

/**
 * alist 目录列表结果。
 */
@Data
public class AlistListResult {
    private List<AlistFileInfo> files;
    private int total;
    private boolean hasMore;
}
