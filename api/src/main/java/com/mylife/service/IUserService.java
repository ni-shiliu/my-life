package com.mylife.service;

import com.mylife.dto.UserDTO;
import com.mylife.dto.UserLoginDTO;
import com.mylife.dto.UserSaveDTO;

public interface IUserService {

    UserDTO save(UserSaveDTO saveDTO);

    UserDTO login(UserLoginDTO loginDTO);

    UserDTO getUserInfo(Long userId);
}
