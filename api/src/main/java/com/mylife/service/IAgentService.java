package com.mylife.service;

import com.mylife.dto.AgentDTO;
import com.mylife.dto.AgentSaveDTO;

import java.util.List;

public interface IAgentService {

    AgentDTO save(Long userId, AgentSaveDTO saveDTO);

    void delete(Long userId, Long agentId);

    AgentDTO get(Long userId, Long agentId);

    List<AgentDTO> list(Long userId);

    void publish(Long userId, Long agentId);
}
