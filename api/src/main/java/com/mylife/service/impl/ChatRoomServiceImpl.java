package com.mylife.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mylife.common.BizException;
import com.mylife.common.ErrorCode;
import com.mylife.entity.ChatMessageDO;
import com.mylife.entity.ChatRoomDO;
import com.mylife.entity.ContextMemoryDO;
import com.mylife.enums.ChatSceneEnum;
import com.mylife.mapper.ChatMessageMapper;
import com.mylife.mapper.ChatRoomMapper;
import com.mylife.mapper.ContextMemoryMapper;
import com.mylife.service.IChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements IChatRoomService {

    private final ChatRoomMapper chatRoomMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ContextMemoryMapper contextMemoryMapper;

    @Override
    public ChatRoomDO getOrCreate(Long userId, String agentUuid, ChatSceneEnum scene) {
        LambdaQueryWrapper<ChatRoomDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatRoomDO::getUserId, userId)
               .eq(ChatRoomDO::getAgentUuid, agentUuid)
               .eq(ChatRoomDO::getScene, scene)
               .last("LIMIT 1");
        ChatRoomDO room = chatRoomMapper.selectOne(wrapper);
        if (room != null) {
            return room;
        }
        room = new ChatRoomDO();
        room.setRoomId(UUID.randomUUID().toString());
        room.setUserId(userId);
        room.setAgentUuid(agentUuid);
        room.setScene(scene);
        chatRoomMapper.insert(room);
        log.info("创建聊天室：roomId={}, scene={}, agentUuid={}, userId={}",
                room.getId(), scene, agentUuid, userId);
        return room;
    }

    @Override
    public ChatRoomDO getByUserIdAndAgentUuid(Long userId, String agentUuid, ChatSceneEnum scene) {
        LambdaQueryWrapper<ChatRoomDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatRoomDO::getUserId, userId)
               .eq(ChatRoomDO::getAgentUuid, agentUuid)
               .eq(ChatRoomDO::getScene, scene)
               .last("LIMIT 1");
        return chatRoomMapper.selectOne(wrapper);
    }

    @Override
    public ChatRoomDO getAndCheckOwnerByRoomId(Long userId, String roomId) {
        LambdaQueryWrapper<ChatRoomDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatRoomDO::getRoomId, roomId)
               .last("LIMIT 1");
        ChatRoomDO room = chatRoomMapper.selectOne(wrapper);
        if (room == null) {
            throw new BizException(ErrorCode.PARAM_ILLEGAL.getCode(), "聊天室不存在");
        }
        if (!room.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.PARAM_ILLEGAL.getCode(), "无权操作此聊天室");
        }
        return room;
    }

    @Override
    public void clearByRoomId(Long roomId) {
        LambdaQueryWrapper<ChatMessageDO> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.eq(ChatMessageDO::getRoomId, roomId);
        chatMessageMapper.delete(msgWrapper);
        log.info("清空聊天记录：roomId={}", roomId);
    }

    @Override
    public void clearMemoryByRoomId(Long roomId) {
        LambdaQueryWrapper<ContextMemoryDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContextMemoryDO::getRoomId, roomId);
        contextMemoryMapper.delete(wrapper);
        log.info("清空上下文记忆：roomId={}", roomId);
    }
}
