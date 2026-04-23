package com.mylife.service;

import com.mylife.dto.AgentDTO;
import com.mylife.dto.AgentSaveDTO;

import java.util.List;

public interface IAgentService {

    AgentDTO save(Long userId, AgentSaveDTO saveDTO);

    void delete(Long userId, String uuid);

    AgentDTO get(Long userId, String uuid);

    List<AgentDTO> list(Long userId);

    void publish(Long userId, String uuid);
}
