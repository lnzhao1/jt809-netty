package com.unicom.netty809.common;


import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话map 根据厂家developer储存 CommonSession （连接信道channel）
 */
public class SessionUtil {

    private static volatile SessionUtil instance = null;

    public static Map<String,CommonSession> commonSessionMap;

    public static SessionUtil getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionUtil();
                }
            }
        }
        return instance;
    }

    public SessionUtil() {
        this.commonSessionMap = new ConcurrentHashMap<String,CommonSession>();
    }

    public boolean containsKey(String sessionId) {
        return commonSessionMap.containsKey(sessionId);
    }

    public boolean containsSession(Session session) {
        return commonSessionMap.containsValue(session);
    }

    public CommonSession findBySessionId(String id) {
        return commonSessionMap.get(id);
    }

    public synchronized CommonSession put(String sessionId, CommonSession value) {
        return commonSessionMap.put(sessionId, value);
    }

    public synchronized CommonSession removeBySessionId(String sessionId) {
        if (sessionId == null)
            return null;
        CommonSession commonSession = commonSessionMap.remove(sessionId);
        if (commonSession == null)
            return null;
        return commonSession;
    }

    public Set<String> keySet() {
        return commonSessionMap.keySet();
    }


    public Set<Map.Entry<String, CommonSession>> entrySet() {
        return commonSessionMap.entrySet();
    }
}
