package com.mylife.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mylife.common.BizException;
import com.mylife.common.ErrorCode;
import com.mylife.dto.AgentDTO;
import com.mylife.dto.AgentPageQueryDTO;
import com.mylife.dto.AgentSaveDTO;
import com.mylife.entity.AgentDO;
import com.mylife.entity.KnowledgeBaseDO;
import com.mylife.entity.UserAgentDO;
import com.mylife.enums.AgentStatusEnum;
import com.mylife.mapper.AgentMapper;
import com.mylife.mapper.KnowledgeBaseMapper;
import com.mylife.mapper.UserAgentMapper;
import com.mylife.service.IAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements IAgentService {

    private final AgentMapper agentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final UserAgentMapper userAgentMapper;

    @Override
    public AgentDTO save(Long userId, AgentSaveDTO saveDTO) {
        AgentDO agentDO;
        if (saveDTO.getUuid() != null) {
            agentDO = getAndCheckOwner(userId, saveDTO.getUuid());
            if (Boolean.TRUE.equals(saveDTO.getResetToDraft())) {
                agentDO.setStatus(AgentStatusEnum.DRAFT);
            }
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
        AgentDO agentDO = loadAgentByUuid(uuid);
        if (agentDO == null) {
            throw new BizException(ErrorCode.PARAM_ILLEGAL.getCode(), "智能体不存在");
        }
        if (!agentDO.getUserId().equals(userId) && agentDO.getStatus() != AgentStatusEnum.PUBLISHED) {
            throw new BizException(ErrorCode.PARAM_ILLEGAL.getCode(), "无权操作此智能体");
        }
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
    public IPage<AgentDTO> queryPublishedPage(Long userId, AgentPageQueryDTO queryDTO) {
        Page<AgentDO> page = new Page<>(queryDTO.getPage() + 1, queryDTO.getSize());
        LambdaQueryWrapper<AgentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentDO::getStatus, AgentStatusEnum.PUBLISHED);
        if (queryDTO.getName() != null && !queryDTO.getName().isBlank()) {
            wrapper.like(AgentDO::getName, queryDTO.getName());
        }
        wrapper.orderByDesc(AgentDO::getGmtModified);
        IPage<AgentDO> doPage = agentMapper.selectPage(page, wrapper);
        List<AgentDO> records = doPage.getRecords();
        Map<Long, String> kbNameMap = buildKbNameMap(records);
        Set<String> addedUuids = loadAddedAgentUuids(userId);
        IPage<AgentDTO> dtoPage = doPage.convert(a -> convertToDTO(a, kbNameMap, userId, addedUuids));
        return dtoPage;
    }

    @Override
    public IPage<AgentDTO> listPage(Long userId, AgentPageQueryDTO queryDTO) {
        Page<AgentDO> page = new Page<>(queryDTO.getPage() + 1, queryDTO.getSize());
        LambdaQueryWrapper<AgentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentDO::getUserId, userId);
        if (queryDTO.getName() != null && !queryDTO.getName().isBlank()) {
            wrapper.like(AgentDO::getName, queryDTO.getName());
        }
        wrapper.orderByDesc(AgentDO::getGmtModified);
        IPage<AgentDO> doPage = agentMapper.selectPage(page, wrapper);
        List<AgentDO> records = doPage.getRecords();
        Map<Long, String> kbNameMap = buildKbNameMap(records);
        IPage<AgentDTO> dtoPage = doPage.convert(a -> convertToDTO(a, kbNameMap, userId, null));
        return dtoPage;
    }

    @Override
    public void addUserAgent(Long userId, String agentUuid) {
        AgentDO agent = loadAgentByUuid(agentUuid);
        if (agent == null) {
            throw new BizException(ErrorCode.PARAM_ILLEGAL.getCode(), "智能体不存在");
        }
        if (agent.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.PARAM_ILLEGAL.getCode(), "不能添加自己创建的智能体");
        }
        LambdaQueryWrapper<UserAgentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAgentDO::getUserId, userId)
               .eq(UserAgentDO::getAgentUuid, agentUuid)
               .last("LIMIT 1");
        UserAgentDO existing = userAgentMapper.selectOne(wrapper);
        if (existing == null) {
            UserAgentDO ua = new UserAgentDO();
            ua.setUserId(userId);
            ua.setAgentUuid(agentUuid);
            userAgentMapper.insert(ua);
            log.info("添加智能体到可用列表：{}", com.alibaba.fastjson2.JSON.toJSONString(
                    java.util.Map.of("userId", userId, "agentUuid", agentUuid)
            ));
        }
    }

    @Override
    public void removeUserAgent(Long userId, String agentUuid) {
        LambdaQueryWrapper<UserAgentDO> query = new LambdaQueryWrapper<>();
        query.eq(UserAgentDO::getUserId, userId)
             .eq(UserAgentDO::getAgentUuid, agentUuid)
             .last("LIMIT 1");
        UserAgentDO record = userAgentMapper.selectOne(query);
        if (record == null) {
            return;
        }
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserAgentDO> update =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        update.eq(UserAgentDO::getId, record.getId())
              .set(UserAgentDO::getIsDeleted, String.valueOf(record.getId()));
        userAgentMapper.update(null, update);
        log.info("移除可用智能体：{}", com.alibaba.fastjson2.JSON.toJSONString(
                java.util.Map.of("userId", userId, "agentUuid", agentUuid)
        ));
    }

    @Override
    public IPage<AgentDTO> listAvailablePage(Long userId, AgentPageQueryDTO queryDTO) {
        Set<String> addedUuids = loadAddedAgentUuids(userId);
        Page<AgentDO> page = new Page<>(queryDTO.getPage() + 1, queryDTO.getSize());
        LambdaQueryWrapper<AgentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentDO::getUserId, userId)
               .or()
               .in(!addedUuids.isEmpty(), AgentDO::getUuid, addedUuids);
        if (queryDTO.getName() != null && !queryDTO.getName().isBlank()) {
            wrapper.like(AgentDO::getName, queryDTO.getName());
        }
        wrapper.orderByDesc(AgentDO::getGmtModified);
        IPage<AgentDO> doPage = agentMapper.selectPage(page, wrapper);
        List<AgentDO> records = doPage.getRecords();
        Map<Long, String> kbNameMap = buildKbNameMap(records);
        IPage<AgentDTO> dtoPage = doPage.convert(a -> convertToDTO(a, kbNameMap, userId, addedUuids));
        return dtoPage;
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

    private AgentDO loadAgentByUuid(String uuid) {
        LambdaQueryWrapper<AgentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentDO::getUuid, uuid)
               .last("LIMIT 1");
        return agentMapper.selectOne(wrapper);
    }

    private AgentDO getAndCheckOwner(Long userId, String uuid) {
        AgentDO agentDO = loadAgentByUuid(uuid);
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

    private Set<String> loadAddedAgentUuids(Long userId) {
        LambdaQueryWrapper<UserAgentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAgentDO::getUserId, userId)
               .select(UserAgentDO::getAgentUuid);
        List<UserAgentDO> list = userAgentMapper.selectList(wrapper);
        if (list.isEmpty()) {
            return Collections.emptySet();
        }
        return list.stream().map(UserAgentDO::getAgentUuid).collect(Collectors.toSet());
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
        return convertToDTO(agentDO, kbNameMap, null, null);
    }

    private AgentDTO convertToDTO(AgentDO agentDO, Map<Long, String> kbNameMap, Long currentUserId, Set<String> addedUuids) {
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
        if (currentUserId != null) {
            dto.setOwned(agentDO.getUserId().equals(currentUserId));
            dto.setAdded(addedUuids != null && addedUuids.contains(agentDO.getUuid()));
        }
        dto.setGmtModified(agentDO.getGmtModified() != null
                ? agentDO.getGmtModified().toString() : null);
        return dto;
    }
}
