package com.mylife.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mylife.common.BizException;
import com.mylife.common.ErrorCode;
import com.mylife.dto.AgentDTO;
import com.mylife.dto.AgentSaveDTO;
import com.mylife.entity.AgentDO;
import com.mylife.entity.KnowledgeBaseDO;
import com.mylife.enums.AgentStatusEnum;
import com.mylife.mapper.AgentMapper;
import com.mylife.mapper.KnowledgeBaseMapper;
import com.mylife.service.IAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements IAgentService {

    private final AgentMapper agentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

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
        Map<Long, String> kbNameMap = buildKbNameMap(List.of(agentDO));
        return convertToDTO(agentDO, kbNameMap);
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
        Map<Long, String> kbNameMap = buildKbNameMap(List.of(agentDO));
        return convertToDTO(agentDO, kbNameMap);
    }

    @Override
    public List<AgentDTO> list(Long userId) {
        LambdaQueryWrapper<AgentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentDO::getUserId, userId)
               .orderByDesc(AgentDO::getGmtModified);
        List<AgentDO> agents = agentMapper.selectList(wrapper);
        Map<Long, String> kbNameMap = buildKbNameMap(agents);
        return agents.stream()
                .map(a -> convertToDTO(a, kbNameMap))
                .collect(Collectors.toList());
    }

    @Override
    public List<AgentDTO> listPublished() {
        LambdaQueryWrapper<AgentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentDO::getStatus, AgentStatusEnum.PUBLISHED)
               .orderByDesc(AgentDO::getGmtModified);
        List<AgentDO> agents = agentMapper.selectList(wrapper);
        Map<Long, String> kbNameMap = buildKbNameMap(agents);
        return agents.stream()
                .map(a -> convertToDTO(a, kbNameMap))
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

    private Map<Long, String> buildKbNameMap(List<AgentDO> agents) {
        List<Long> kbIds = agents.stream()
                .map(AgentDO::getKnowledgeBaseId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (kbIds.isEmpty()) {
            return Map.of();
        }
        List<KnowledgeBaseDO> kbList = knowledgeBaseMapper.selectBatchIds(kbIds);
        return kbList.stream().collect(Collectors.toMap(KnowledgeBaseDO::getId, KnowledgeBaseDO::getName));
    }

    private AgentDTO convertToDTO(AgentDO agentDO, Map<Long, String> kbNameMap) {
        AgentDTO dto = new AgentDTO();
        dto.setUuid(agentDO.getUuid());
        dto.setName(agentDO.getName());
        dto.setDescription(agentDO.getDescription());
        dto.setIconIndex(agentDO.getIconIndex());
        dto.setColor(agentDO.getColor());
        dto.setSystemPrompt(agentDO.getSystemPrompt());
        dto.setKnowledgeBaseId(agentDO.getKnowledgeBaseId());
        dto.setKnowledgeBaseName(agentDO.getKnowledgeBaseId() != null
                ? kbNameMap.getOrDefault(agentDO.getKnowledgeBaseId(), null) : null);
        dto.setStatus(agentDO.getStatus().getValue());
        dto.setGmtModified(agentDO.getGmtModified() != null
                ? agentDO.getGmtModified().toString() : null);
        return dto;
    }
}
