package com.mylife.controller;

import com.mylife.common.BaseResult;
import com.mylife.dto.AgentDTO;
import com.mylife.dto.AgentSaveDTO;
import com.mylife.security.SecurityUtils;
import com.mylife.service.IAgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/agent")
@RequiredArgsConstructor
public class AgentController {

    private final IAgentService agentService;

    @PostMapping("/save")
    public BaseResult<AgentDTO> save(@Valid @RequestBody AgentSaveDTO saveDTO) {
        return BaseResult.success(agentService.save(SecurityUtils.getUserId(), saveDTO));
    }

    @DeleteMapping("/delete")
    public BaseResult<Void> delete(@RequestParam String uuid) {
        agentService.delete(SecurityUtils.getUserId(), uuid);
        return BaseResult.success(null);
    }

    @GetMapping("/get/{uuid}")
    public BaseResult<AgentDTO> get(@PathVariable String uuid) {
        return BaseResult.success(agentService.get(SecurityUtils.getUserId(), uuid));
    }

    @PostMapping("/list")
    public BaseResult<List<AgentDTO>> list() {
        return BaseResult.success(agentService.list(SecurityUtils.getUserId()));
    }

    @GetMapping("/published")
    public BaseResult<List<AgentDTO>> listPublished() {
        return BaseResult.success(agentService.listPublished());
    }

    @PostMapping("/publish")
    public BaseResult<Void> publish(@RequestParam String uuid) {
        agentService.publish(SecurityUtils.getUserId(), uuid);
        return BaseResult.success(null);
    }
}
