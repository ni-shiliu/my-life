package com.mylife.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mylife.common.BaseResult;
import com.mylife.dto.AgentDTO;
import com.mylife.dto.AgentPageQueryDTO;
import com.mylife.dto.AgentSaveDTO;
import com.mylife.security.SecurityUtils;
import com.mylife.service.IAgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @PostMapping("/published")
    public BaseResult<Map<String, Object>> queryPublishedPage(@Valid @RequestBody AgentPageQueryDTO queryDTO) {
        IPage<AgentDTO> mpPage = agentService.queryPublishedPage(SecurityUtils.getUserId(), queryDTO);
        return buildPageResult(mpPage);
    }

    @PostMapping("/queryPage")
    public BaseResult<Map<String, Object>> queryPage(@Valid @RequestBody AgentPageQueryDTO queryDTO) {
        IPage<AgentDTO> mpPage = agentService.listPage(SecurityUtils.getUserId(), queryDTO);
        return buildPageResult(mpPage);
    }

    @PostMapping("/add")
    public BaseResult<Void> add(@RequestParam String agentUuid) {
        agentService.addUserAgent(SecurityUtils.getUserId(), agentUuid);
        return BaseResult.success(null);
    }

    @DeleteMapping("/remove")
    public BaseResult<Void> remove(@RequestParam String agentUuid) {
        agentService.removeUserAgent(SecurityUtils.getUserId(), agentUuid);
        return BaseResult.success(null);
    }

    @PostMapping("/available")
    public BaseResult<Map<String, Object>> available(@Valid @RequestBody AgentPageQueryDTO queryDTO) {
        IPage<AgentDTO> mpPage = agentService.listAvailablePage(SecurityUtils.getUserId(), queryDTO);
        return buildPageResult(mpPage);
    }

    @PostMapping("/publish")
    public BaseResult<Void> publish(@RequestParam String uuid) {
        agentService.publish(SecurityUtils.getUserId(), uuid);
        return BaseResult.success(null);
    }

    private BaseResult<Map<String, Object>> buildPageResult(IPage<AgentDTO> mpPage) {
        Map<String, Object> result = new HashMap<>();
        result.put("records", mpPage.getRecords());
        result.put("total", mpPage.getTotal());
        result.put("pages", mpPage.getPages());
        result.put("current", mpPage.getCurrent());
        result.put("size", mpPage.getSize());
        return BaseResult.success(result);
    }
}
