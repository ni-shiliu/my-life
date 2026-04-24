package com.mylife.service;

import com.mylife.dto.KnowledgeBaseDTO;
import com.mylife.dto.KnowledgeBaseSaveDTO;

import java.util.List;

public interface IKnowledgeBaseService {

    KnowledgeBaseDTO save(Long userId, KnowledgeBaseSaveDTO saveDTO);

    void delete(Long userId, String uuid);

    List<KnowledgeBaseDTO> list(Long userId);

    KnowledgeBaseDTO get(Long userId, String uuid);
}
