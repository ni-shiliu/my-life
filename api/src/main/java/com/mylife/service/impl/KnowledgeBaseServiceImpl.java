package com.mylife.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mylife.common.BizException;
import com.mylife.common.ErrorCode;
import com.mylife.dto.KnowledgeBaseDTO;
import com.mylife.dto.KnowledgeBaseSaveDTO;
import com.mylife.entity.KnowledgeBaseDO;
import com.mylife.enums.KbSourceEnum;
import com.mylife.mapper.KnowledgeBaseMapper;
import com.mylife.service.IKnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements IKnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    public KnowledgeBaseDTO save(Long userId, KnowledgeBaseSaveDTO saveDTO) {
        KnowledgeBaseDO kbDO;
        if (saveDTO.getUuid() != null) {
            kbDO = getAndCheckOwner(userId, saveDTO.getUuid());
            kbDO.setName(saveDTO.getName());
            kbDO.setExternalId(saveDTO.getExternalId());
            knowledgeBaseMapper.updateById(kbDO);
        } else {
            kbDO = new KnowledgeBaseDO();
            kbDO.setUuid(UUID.randomUUID().toString());
            kbDO.setUserId(userId);
            kbDO.setSource(KbSourceEnum.BAILIAN);
            kbDO.setName(saveDTO.getName());
            kbDO.setExternalId(saveDTO.getExternalId());
            knowledgeBaseMapper.insert(kbDO);
        }
        log.info("知识库保存成功：{}", com.alibaba.fastjson2.JSON.toJSONString(
                java.util.Map.of("uuid", kbDO.getUuid(), "userId", userId)
        ));
        return convertToDTO(kbDO);
    }

    @Override
    public void delete(Long userId, String uuid) {
        KnowledgeBaseDO kbDO = getAndCheckOwner(userId, uuid);
        knowledgeBaseMapper.deleteById(kbDO.getId());
        log.info("知识库删除成功：{}", com.alibaba.fastjson2.JSON.toJSONString(
                java.util.Map.of("uuid", uuid, "userId", userId)
        ));
    }

    @Override
    public List<KnowledgeBaseDTO> list(Long userId) {
        LambdaQueryWrapper<KnowledgeBaseDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBaseDO::getUserId, userId)
               .orderByDesc(KnowledgeBaseDO::getGmtModified);
        return knowledgeBaseMapper.selectList(wrapper).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public KnowledgeBaseDTO get(Long userId, String uuid) {
        return convertToDTO(getAndCheckOwner(userId, uuid));
    }

    private KnowledgeBaseDO getAndCheckOwner(Long userId, String uuid) {
        LambdaQueryWrapper<KnowledgeBaseDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBaseDO::getUuid, uuid)
               .last("LIMIT 1");
        KnowledgeBaseDO kbDO = knowledgeBaseMapper.selectOne(wrapper);
        if (kbDO == null) {
            throw new BizException(ErrorCode.PARAM_ILLEGAL.getCode(), "知识库不存在");
        }
        if (!kbDO.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.PARAM_ILLEGAL.getCode(), "无权操作此知识库");
        }
        return kbDO;
    }

    private KnowledgeBaseDTO convertToDTO(KnowledgeBaseDO kbDO) {
        KnowledgeBaseDTO dto = new KnowledgeBaseDTO();
        dto.setId(kbDO.getId());
        dto.setUuid(kbDO.getUuid());
        dto.setName(kbDO.getName());
        dto.setSource(kbDO.getSource().getValue());
        dto.setExternalId(kbDO.getExternalId());
        dto.setGmtModified(kbDO.getGmtModified() != null
                ? kbDO.getGmtModified().toString() : null);
        return dto;
    }
}
