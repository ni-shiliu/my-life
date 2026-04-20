package com.mylife.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyBatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "gmtCreated", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "creator", String.class, "system");
        this.strictInsertFill(metaObject, "gmtModified", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "modifier", String.class, "system");
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "gmtModified", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "modifier", String.class, "system");
    }
}
