package com.mylife.service;

import com.mylife.entity.ChatRoomDO;
import com.mylife.enums.ChatSceneEnum;

public interface IChatRoomService {

    ChatRoomDO getOrCreate(Long userId, String agentUuid, ChatSceneEnum scene);

    ChatRoomDO getByUserIdAndAgentUuid(Long userId, String agentUuid, ChatSceneEnum scene);

    ChatRoomDO getAndCheckOwnerByRoomId(Long userId, String roomId);

    void clearByRoomId(Long roomId);

    void clearMemoryByRoomId(Long roomId);
}
