package com.mylife.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mylife.common.BizException;
import com.mylife.common.ErrorCode;
import com.mylife.dto.AgentDTO;
import com.mylife.dto.AgentSaveDTO;
import com.mylife.entity.AgentDO;
import com.mylife.enums.AgentStatusEnum;
import com.mylife.mapper.AgentMapper;
import com.mylife.service.IAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements IAgentService {

    private final AgentMapper agentMapper;

    @Override
    public AgentDTO save(Long userId, AgentSaveDTO saveDTO) {
        AgentDO agentDO;
        if (saveDTO.getUuid() != null) {
            agentDO = getAndCheckOwner(userId, saveDTO.getUuid());
            updateFields(agentDO, saveDTO);
            agentMapper.updateById(agentDO);
        } else {
            agentDO = new AgentDO();
            agentDO.setUuid(UUID.randomUUID().toString());
            agentDO.setUserId(userId);
            agentDO.setStatus(AgentStatusEnum.DRAFT);
            updateFields(agentDO, saveDTO);
            agentMapper.insert(agentDO);
        }
        log.info("智能体保存成功：{}", com.alibaba.fastjson2.JSON.toJSONString(
                java.util.Map.of("agentId", agentDO.getId(), "uuid", agentDO.getUuid(), "userId", userId)
        ));
        return convertToDTO(agentDO);
    }

    @Override
    public void delete(Long userId, String uuid) {
        AgentDO agentDO = getAndCheckOwner(userId, uuid);
        agentMapper.deleteById(agentDO.getId());
        log.info("智能体删除成功：{}", com.alibaba.fastjson2.JSON.toJSONString(
                java.util.Map.of("uuid", uuid, "userId", userId)
        ));
    }

    @Override
    public AgentDTO get(Long userId, String uuid) {
        AgentDO agentDO = getAndCheckOwner(userId, uuid);
        return convertToDTO(agentDO);
    }

    @Override
    public List<AgentDTO> list(Long userId) {
        LambdaQueryWrapper<AgentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentDO::getUserId, userId)
               .orderByDesc(AgentDO::getGmtModified);
        return agentMapper.selectList(wrapper).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void publish(Long userId, String uuid) {
        AgentDO agentDO = getAndCheckOwner(userId, uuid);
        agentDO.setStatus(AgentStatusEnum.PUBLISHED);
        agentMapper.updateById(agentDO);
        log.info("智能体发布成功：{}", com.alibaba.fastjson2.JSON.toJSONString(
                java.util.Map.of("uuid", uuid, "userId", userId)
        ));
    }

    private AgentDO getAndCheckOwner(Long userId, String uuid) {
        LambdaQueryWrapper<AgentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentDO::getUuid, uuid)
               .last("LIMIT 1");
        AgentDO agentDO = agentMapper.selectOne(wrapper);
        if (agentDO == null) {
            throw new BizException(ErrorCode.PARAM_ILLEGAL.getCode(), "智能体不存在");
        }
        if (!agentDO.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.PARAM_ILLEGAL.getCode(), "无权操作此智能体");
        }
        return agentDO;
    }

    private void updateFields(AgentDO agentDO, AgentSaveDTO saveDTO) {
        agentDO.setName(saveDTO.getName());
        agentDO.setDescription(saveDTO.getDescription());
        if (saveDTO.getIconIndex() != null) {
            agentDO.setIconIndex(saveDTO.getIconIndex());
        }
        if (saveDTO.getColor() != null) {
            agentDO.setColor(saveDTO.getColor());
        }
        agentDO.setSystemPrompt(saveDTO.getSystemPrompt());
        agentDO.setKnowledgeBaseId(saveDTO.getKnowledgeBaseId());
    }

    private AgentDTO convertToDTO(AgentDO agentDO) {
        AgentDTO dto = new AgentDTO();
        dto.setUuid(agentDO.getUuid());
        dto.setName(agentDO.getName());
        dto.setDescription(agentDO.getDescription());
        dto.setIconIndex(agentDO.getIconIndex());
        dto.setColor(agentDO.getColor());
        dto.setSystemPrompt(agentDO.getSystemPrompt());
        dto.setKnowledgeBaseId(agentDO.getKnowledgeBaseId());
        dto.setStatus(agentDO.getStatus().getValue());
        dto.setGmtModified(agentDO.getGmtModified() != null
                ? agentDO.getGmtModified().toString() : null);
        return dto;
    }
}
