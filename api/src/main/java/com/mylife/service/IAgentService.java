package com.mylife.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mylife.dto.AgentDTO;
import com.mylife.dto.AgentPageQueryDTO;
import com.mylife.dto.AgentSaveDTO;

import java.util.List;

public interface IAgentService {

    AgentDTO save(Long userId, AgentSaveDTO saveDTO);

    void delete(Long userId, String uuid);

    AgentDTO get(Long userId, String uuid);

    List<AgentDTO> list(Long userId);

    IPage<AgentDTO> queryPublishedPage(Long userId, AgentPageQueryDTO queryDTO);

    IPage<AgentDTO> listPage(Long userId, AgentPageQueryDTO queryDTO);

    void addUserAgent(Long userId, String agentUuid);

    void removeUserAgent(Long userId, String agentUuid);

    IPage<AgentDTO> listAvailablePage(Long userId, AgentPageQueryDTO queryDTO);

    void publish(Long userId, String uuid);
}
