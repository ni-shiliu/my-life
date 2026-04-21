package com.mylife.controller;

import com.mylife.common.BaseResult;
import com.mylife.dto.AgentDTO;
import com.mylife.dto.AgentSaveDTO;
import com.mylife.service.IAgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/agent")
@RequiredArgsConstructor
public class AgentController {

    private final IAgentService agentService;

    @PostMapping("/save")
    public BaseResult<AgentDTO> save(@Valid @RequestBody AgentSaveDTO saveDTO,
                                     Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return BaseResult.success(agentService.save(userId, saveDTO));
    }

    @DeleteMapping("/delete")
    public BaseResult<Void> delete(@RequestParam Long id,
                                    Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        agentService.delete(userId, id);
        return BaseResult.success(null);
    }

    @GetMapping("/get/{id}")
    public BaseResult<AgentDTO> get(@PathVariable Long id,
                                     Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return BaseResult.success(agentService.get(userId, id));
    }

    @PostMapping("/list")
    public BaseResult<List<AgentDTO>> list(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return BaseResult.success(agentService.list(userId));
    }

    @PostMapping("/publish")
    public BaseResult<Void> publish(@RequestParam Long id,
                                     Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        agentService.publish(userId, id);
        return BaseResult.success(null);
    }
}
