package com.unicom.netty809.common;


import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class SessionManager {

	private static volatile SessionManager instance = null;
	// netty生成的sessionID和Session的对应关系
	private Map<String, Session> sessionIdMap;
	// 终端手机号和netty生成的sessionID的对应关系
	private Map<String, String> phoneMap;

	public static SessionManager getInstance() {
		if (instance == null) {
			synchronized (SessionManager.class) {
				if (instance == null) {
					instance = new SessionManager();
				}
			}
		}
		return instance;
	}

	public SessionManager() {
		this.sessionIdMap = new ConcurrentHashMap<String, Session>();
		this.phoneMap = new ConcurrentHashMap<String, String>();
	}

	public boolean containsKey(String sessionId) {
		return sessionIdMap.containsKey(sessionId);
	}

	public boolean containsSession(Session session) {
		return sessionIdMap.containsValue(session);
	}

	public Session findBySessionId(String id) {
		return sessionIdMap.get(id);
	}

	public Session findByTerminalPhone(String phone) {
		String sessionId = this.phoneMap.get(phone);
		if (sessionId == null)
			return null;
		return this.findBySessionId(sessionId);
	}

	public synchronized Session put(String key, Session value) {
		if (value.getTerminalPhone() != null && !"".equals(value.getTerminalPhone().trim())) {
			this.phoneMap.put(value.getTerminalPhone(), value.getId());
		}
		return sessionIdMap.put(key, value);
	}

	public synchronized Session removeBySessionId(String sessionId) {
		if (sessionId == null)
			return null;
		Session session = sessionIdMap.remove(sessionId);
		if (session == null)
			return null;
		if (session.getTerminalPhone() != null)
			this.phoneMap.remove(session.getTerminalPhone());
		return session;
	}

	public Set<String> keySet() {
		return sessionIdMap.keySet();
	}

	public Set<Entry<String, Session>> entrySet() {
		return sessionIdMap.entrySet();
	}


}