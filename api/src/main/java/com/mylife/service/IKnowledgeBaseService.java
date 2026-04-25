package com.mylife.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mylife.dto.KnowledgeBaseDTO;
import com.mylife.dto.KnowledgeBasePageQueryDTO;
import com.mylife.dto.KnowledgeBaseSaveDTO;

import java.util.List;

public interface IKnowledgeBaseService {

    KnowledgeBaseDTO save(Long userId, KnowledgeBaseSaveDTO saveDTO);

    void delete(Long userId, String uuid);

    List<KnowledgeBaseDTO> list(Long userId);

    IPage<KnowledgeBaseDTO> listPage(Long userId, KnowledgeBasePageQueryDTO queryDTO);

    KnowledgeBaseDTO get(Long userId, String uuid);
}
