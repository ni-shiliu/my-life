package com.mylife.controller;

import com.mylife.common.BaseResult;
import com.mylife.dto.UserDTO;
import com.mylife.dto.UserLoginDTO;
import com.mylife.dto.UserSaveDTO;
import com.mylife.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @PostMapping("/save")
    public BaseResult<UserDTO> save(@Valid @RequestBody UserSaveDTO saveDTO) {
        return BaseResult.success(userService.save(saveDTO));
    }

    @PostMapping("/login")
    public BaseResult<UserDTO> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        return BaseResult.success(userService.login(loginDTO));
    }

    @GetMapping("/info")
    public BaseResult<UserDTO> info(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return BaseResult.success(userService.getUserInfo(userId));
    }
}
