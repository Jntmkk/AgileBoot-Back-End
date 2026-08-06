package com.agileboot.domain.social.prompt;

import cn.hutool.core.bean.BeanUtil;
import com.agileboot.common.core.page.PageDTO;
import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode;
import com.agileboot.domain.common.command.BulkOperationCommand;
import com.agileboot.domain.social.prompt.command.PromptAddCommand;
import com.agileboot.domain.social.prompt.command.PromptUpdateCommand;
import com.agileboot.domain.social.prompt.db.SocialSummaryPromptEntity;
import com.agileboot.domain.social.prompt.db.SocialSummaryPromptService;
import com.agileboot.domain.social.prompt.dto.PromptDTO;
import com.agileboot.domain.social.prompt.query.PromptQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptApplicationService {

    private final SocialSummaryPromptService promptService;

    public PageDTO<PromptDTO> getPromptList(PromptQuery query) {
        Page<SocialSummaryPromptEntity> page = promptService.page(query.toPage(), query.toQueryWrapper());
        List<PromptDTO> records = page.getRecords().stream()
            .map(PromptDTO::new)
            .collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }

    public PromptDTO getPromptInfo(Long id) {
        SocialSummaryPromptEntity entity = promptService.getById(id);
        if (entity == null) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "记录不存在: " + id);
        }
        return new PromptDTO(entity);
    }

    public void addPrompt(PromptAddCommand command) {
        SocialSummaryPromptEntity entity = new SocialSummaryPromptEntity();
        BeanUtil.copyProperties(command, entity);
        entity.setSortOrder(command.getSortOrder() != null ? command.getSortOrder() : 0);
        entity.setStatus(command.getStatus() != null ? command.getStatus() : 1);
        entity.insert();
    }

    public void updatePrompt(PromptUpdateCommand command) {
        SocialSummaryPromptEntity entity = promptService.getById(command.getId());
        if (entity == null) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "记录不存在: " + command.getId());
        }
        BeanUtil.copyProperties(command, entity);
        entity.setSortOrder(command.getSortOrder() != null ? command.getSortOrder() : 0);
        entity.setStatus(command.getStatus() != null ? command.getStatus() : 1);
        entity.updateById();
    }

    public void deletePrompt(BulkOperationCommand<Long> command) {
        promptService.removeBatchByIds(command.getIds());
    }

    /**
     * 匹配提示词：先按 upId 精确匹配，再兜底 up_id='*' 的默认规则。
     * 均按 sort_order 升序，首个 keyword contains title 即返回。
     *
     * @return 匹配到的 systemPrompt，未匹配返回 null
     */
    public String matchPrompt(String upId, String title) {
        // 1. UP 专属规则
        List<SocialSummaryPromptEntity> prompts = promptService.lambdaQuery()
            .eq(SocialSummaryPromptEntity::getUpId, upId)
            .eq(SocialSummaryPromptEntity::getStatus, 1)
            .orderByAsc(SocialSummaryPromptEntity::getSortOrder)
            .list();
        String result = findMatch(prompts, title);
        if (result != null) {
            return result;
        }
        // 2. 兜底默认规则 (up_id = '*')
        List<SocialSummaryPromptEntity> defaultPrompts = promptService.lambdaQuery()
            .eq(SocialSummaryPromptEntity::getUpId, "*")
            .eq(SocialSummaryPromptEntity::getStatus, 1)
            .orderByAsc(SocialSummaryPromptEntity::getSortOrder)
            .list();
        return findMatch(defaultPrompts, title);
    }

    private String findMatch(List<SocialSummaryPromptEntity> prompts, String title) {
        if (title == null) {
            return null;
        }
        for (SocialSummaryPromptEntity p : prompts) {
            if (title.contains(p.getKeyword())) {
                return p.getSystemPrompt();
            }
        }
        return null;
    }

}
