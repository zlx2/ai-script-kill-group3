package com.wn.config;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 15:50
 * @Component:
 **/

import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.State;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版 AgentStateStore
 * 用于开发测试，重启后数据丢失
 */
@Component
public class InMemoryAgentStateStore implements AgentStateStore {

    // 存储结构: userId -> sessionId -> key -> State
    private final Map<String, Map<String, Map<String, State>>> store = new ConcurrentHashMap<>();

    @Override
    public void save(String userId, String sessionId, String key, State value) {
        store.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                .put(key, value);
    }

    @Override
    public void save(String userId, String sessionId, String key, List<? extends State> values) {
        // 简单实现：把 List 包装成一个 State 存储
        // 或者分别存储每个元素
        ListStateWrapper wrapper = new ListStateWrapper(values);
        save(userId, sessionId, key, wrapper);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends State> Optional<T> get(String userId, String sessionId, String key, Class<T> type) {
        Map<String, Map<String, State>> userStore = store.get(userId);
        if (userStore == null) {
            return Optional.empty();
        }
        Map<String, State> sessionStore = userStore.get(sessionId);
        if (sessionStore == null) {
            return Optional.empty();
        }
        State value = sessionStore.get(key);
        if (value == null) {
            return Optional.empty();
        }
        // 如果类型匹配，返回；否则返回 empty
        if (type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends State> List<T> getList(String userId, String sessionId, String key, Class<T> itemType) {
        Optional<State> result = get(userId, sessionId, key, State.class);
        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        State value = result.get();
        if (value instanceof ListStateWrapper) {
            return ((ListStateWrapper) value).getItems(itemType);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean exists(String userId, String sessionId) {
        Map<String, Map<String, State>> userStore = store.get(userId);
        if (userStore == null) {
            return false;
        }
        return userStore.containsKey(sessionId);
    }

    @Override
    public void delete(String userId, String sessionId) {
        Map<String, Map<String, State>> userStore = store.get(userId);
        if (userStore != null) {
            userStore.remove(sessionId);
        }
    }

    @Override
    public void delete(String userId, String sessionId, String key) {
        Map<String, Map<String, State>> userStore = store.get(userId);
        if (userStore != null) {
            Map<String, State> sessionStore = userStore.get(sessionId);
            if (sessionStore != null) {
                sessionStore.remove(key);
            }
        }
    }

    @Override
    public Set<String> listSessionIds(String userId) {
        Map<String, Map<String, State>> userStore = store.get(userId);
        if (userStore == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(userStore.keySet());
    }

    @Override
    public void close() {
        store.clear();
    }

    /**
     * 包装 List<State> 的辅助类
     */
    private static class ListStateWrapper implements State {
        private final List<? extends State> items;

        ListStateWrapper(List<? extends State> items) {
            this.items = items;
        }

        @SuppressWarnings("unchecked")
        <T extends State> List<T> getItems(Class<T> itemType) {
            List<T> result = new ArrayList<>();
            for (State item : items) {
                if (itemType.isInstance(item)) {
                    result.add((T) item);
                }
            }
            return result;
        }
    }
}
