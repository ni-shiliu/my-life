package com.mylife.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mylife.common.BizException;
import com.mylife.common.ErrorCode;
import com.mylife.dto.UserDTO;
import com.mylife.dto.UserLoginDTO;
import com.mylife.dto.UserSaveDTO;
import com.mylife.entity.UserDO;
import com.mylife.enums.UserStatusEnum;
import com.mylife.enums.YesNoEnum;
import com.mylife.mapper.UserMapper;
import com.mylife.security.JwtTokenProvider;
import com.mylife.service.IUserService;
import com.mylife.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${login.max-fail-count}")
    private int maxFailCount;

    @Value("${login.lock-minutes}")
    private int lockMinutes;

    // TODO: 后续替换为 Redis，当前仅单实例可用
    private final ConcurrentHashMap<String, LoginLockInfo> loginFailMap = new ConcurrentHashMap<>();

    @Override
    public UserDTO save(UserSaveDTO saveDTO) {
        checkPhoneUnique(saveDTO.getPhone());

        UserDO userDO = new UserDO();
        userDO.setPhone(saveDTO.getPhone());
        userDO.setPassword(passwordEncoder.encode(saveDTO.getPassword()));
        userDO.setStatus(UserStatusEnum.ACTIVE);
        userDO.setLoginFailCount(0);
        if (saveDTO.getNickName() == null || saveDTO.getNickName().isBlank()) {
            userDO.setNickName("用户" + saveDTO.getPhone().substring(7));
        } else {
            userDO.setNickName(saveDTO.getNickName());
        }

        userMapper.insert(userDO);
        String token = jwtTokenProvider.generateToken(userDO.getId());

        log.info("用户注册成功：{}", com.alibaba.fastjson2.JSON.toJSONString(
                java.util.Map.of("userId", userDO.getId(), "phone", saveDTO.getPhone())
        ));

        return buildUserDTO(userDO, token);
    }

    @Override
    public UserDTO login(UserLoginDTO loginDTO) {
        checkLoginLock(loginDTO.getPhone());

        UserDO userDO = getUserByPhone(loginDTO.getPhone());
        if (userDO == null) {
            throw new BizException(ErrorCode.USER_PASSWORD_ERROR.getCode(), ErrorCode.USER_PASSWORD_ERROR.getMessage());
        }

        checkUserStatus(userDO);
        verifyPassword(loginDTO.getPassword(), userDO);

        // 登录成功：清零失败次数，更新最后登录时间
        clearLoginFail(loginDTO.getPhone());
        userDO.setLoginFailCount(0);
        userDO.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(userDO);

        String token = jwtTokenProvider.generateToken(userDO.getId());

        log.info("用户登录成功：{}", com.alibaba.fastjson2.JSON.toJSONString(
                java.util.Map.of("userId", userDO.getId())
        ));

        return buildUserDTO(userDO, token);
    }

    private void checkPhoneUnique(String phone) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getPhone, phone)
               .eq(UserDO::getIsDeleted, YesNoEnum.NO.getValue());
        long count = userMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BizException(ErrorCode.USER_PHONE_EXISTS.getCode(), ErrorCode.USER_PHONE_EXISTS.getMessage());
        }
    }

    private UserDO getUserByPhone(String phone) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getPhone, phone)
               .eq(UserDO::getIsDeleted, YesNoEnum.NO.getValue());
        return userMapper.selectOne(wrapper);
    }

    private void checkUserStatus(UserDO userDO) {
        if (UserStatusEnum.DISABLED == userDO.getStatus()) {
            throw new BizException(ErrorCode.USER_DISABLED.getCode(), ErrorCode.USER_DISABLED.getMessage());
        }
    }

    private void verifyPassword(String rawPassword, UserDO userDO) {
        boolean matched = passwordEncoder.matches(rawPassword, userDO.getPassword());
        if (!matched) {
            handleLoginFail(userDO);
            throw new BizException(ErrorCode.USER_PASSWORD_ERROR.getCode(), ErrorCode.USER_PASSWORD_ERROR.getMessage());
        }
    }

    private void checkLoginLock(String phone) {
        LoginLockInfo info = loginFailMap.get(phone);
        if (info == null || info.lockTime == null) {
            return;
        }
        LocalDateTime unlockTime = info.lockTime.plusMinutes(lockMinutes);
        if (LocalDateTime.now().isBefore(unlockTime)) {
            long remainMin = java.time.Duration.between(LocalDateTime.now(), unlockTime).toMinutes() + 1;
            throw new BizException(ErrorCode.USER_LOCKED.getCode(),
                    "账户已锁定，请" + remainMin + "分钟后重试");
        }
        // 锁定已过期，清除记录
        loginFailMap.remove(phone);
    }

    private void handleLoginFail(UserDO userDO) {
        LoginLockInfo info = loginFailMap.computeIfAbsent(userDO.getPhone(), k -> new LoginLockInfo(0, null));
        int newCount = info.failCount + 1;
        if (newCount >= maxFailCount) {
            loginFailMap.put(userDO.getPhone(), new LoginLockInfo(newCount, LocalDateTime.now()));
            throw new BizException(ErrorCode.USER_LOCKED.getCode(),
                    "账户已锁定，请" + lockMinutes + "分钟后重试");
        }
        loginFailMap.put(userDO.getPhone(), new LoginLockInfo(newCount, null));
    }

    private void clearLoginFail(String phone) {
        loginFailMap.remove(phone);
    }

    @Override
    public UserDTO getUserInfo(Long userId) {
        UserDO userDO = userMapper.selectById(userId);
        if (userDO == null || YesNoEnum.YES.getValue().equals(userDO.getIsDeleted())) {
            throw new BizException(ErrorCode.PARAM_ILLEGAL.getCode(), "用户不存在");
        }
        UserDTO dto = new UserDTO();
        dto.setUserId(userDO.getId());
        dto.setPhone(PhoneUtils.desensitize(userDO.getPhone()));
        dto.setNickName(userDO.getNickName());
        return dto;
    }

    private UserDTO buildUserDTO(UserDO userDO, String token) {
        UserDTO dto = new UserDTO();
        dto.setUserId(userDO.getId());
        dto.setPhone(PhoneUtils.desensitize(userDO.getPhone()));
        dto.setNickName(userDO.getNickName());
        dto.setAccessToken(token);
        dto.setAccessExpireAt(LocalDateTime.now().plusSeconds(86400));
        return dto;
    }

    private record LoginLockInfo(int failCount, LocalDateTime lockTime) {
    }
}
