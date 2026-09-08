package com.jiuyu.replay.api.handle;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * @author lyw
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Long userId = getCurrentUserId();
        if (userId == null){
            userId=0L;
        }
        this.strictInsertFill(metaObject, "createId", Long.class, userId);
        this.strictInsertFill(metaObject, "updateId", Long.class, userId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long userId = getCurrentUserId();
        if (userId == null){
            userId=0L;
        }
        this.fillStrategy(metaObject, "updateId", userId);

        // 处理逻辑删除
        Object deleted = getFieldValByName("isDeleted", metaObject);
        if (deleted instanceof Boolean && (Boolean) deleted) {
            this.strictUpdateFill(metaObject, "updateId", Long.class, userId);
        }
    }

    private Long getCurrentUserId() {
        UserCacheVo localUser = GlobalObject.getLocalUser();
        if (localUser == null){
            return null;
        }
        return localUser.getId();
    }
}
