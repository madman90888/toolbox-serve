package noob.toolbox.listener;

import lombok.extern.slf4j.Slf4j;
import noob.toolbox.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * 监听所有db的过期事件__keyevent@*__:expired"
 */
@Slf4j
@Component
public class RedisKeyExpirationListener extends KeyExpirationEventMessageListener {
    public static final String EXPIRATION_DELETE_FILE = "Expiration_delete_file:";

    public RedisKeyExpirationListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    @Override
    protected void doHandleMessage(Message message) {
        final String msg = message.toString();
        // 判断是文件删除 key
        if (msg.startsWith(EXPIRATION_DELETE_FILE)) {
            deleteFile(msg.replace(EXPIRATION_DELETE_FILE, ""));
        }
    }

    /**
     * 根据 UUID 查询Redis中文件实际存放路径，并删除该文件
     * @param realPath 文件路径
     */
    private void deleteFile(String realPath) {
        log.debug("定时删除文件：" + realPath);
        try {
            FileUtil.delFile(realPath);
        }catch (Exception e) {
            log.error("文件删除失败：", e);
        }
    }
}
