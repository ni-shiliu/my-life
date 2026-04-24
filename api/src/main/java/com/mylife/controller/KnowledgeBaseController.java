package com.mylife.controller;

import com.mylife.common.BaseResult;
import com.mylife.dto.KnowledgeBaseDTO;
import com.mylife.dto.KnowledgeBaseSaveDTO;
import com.mylife.security.SecurityUtils;
import com.mylife.service.IKnowledgeBaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/knowledge-base")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final IKnowledgeBaseService knowledgeBaseService;

    @PostMapping("/save")
    public BaseResult<KnowledgeBaseDTO> save(@Valid @RequestBody KnowledgeBaseSaveDTO saveDTO) {
        return BaseResult.success(knowledgeBaseService.save(SecurityUtils.getUserId(), saveDTO));
    }

    @DeleteMapping("/delete")
    public BaseResult<Void> delete(@RequestParam String uuid) {
        knowledgeBaseService.delete(SecurityUtils.getUserId(), uuid);
        return BaseResult.success(null);
    }

    @PostMapping("/list")
    public BaseResult<List<KnowledgeBaseDTO>> list() {
        return BaseResult.success(knowledgeBaseService.list(SecurityUtils.getUserId()));
    }
}
