/*******************************************************************************
 * Copyright 2016 General Electric Company.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.ge.predix.acs.policy.evaluation.cache;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.DefaultStringRedisConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

@Component
@Profile({ "cloud-redis", "redis" })
public class RedisPolicyEvaluationCache extends AbstractPolicyEvaluationCache implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisPolicyEvaluationCache.class);

    @Value("${CACHED_EVAL_TTL_SECONDS:600}")
    private long cachedEvalTimeToLiveSeconds;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public void afterPropertiesSet() {
        LOGGER.info("Starting Redis policy evaluation cache.");
        try {
            LOGGER.info("Redis server ping: " + this.redisTemplate.getConnectionFactory().getConnection().ping());
        } catch (RedisConnectionFailureException ex) {
            LOGGER.error("Redis server ping failed.", ex);
        }
    }

    @Override
    void delete(final String key) {
        this.redisTemplate.delete(key);
    }

    @Override
    void delete(final Collection<String> keys) {
        this.redisTemplate.delete(keys);
    }

    @Override
    void flushAll() {
        this.redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Override
    Set<String> getResourceTranslations(final String fromKey) {
        return this.redisTemplate.opsForSet().members(fromKey);
    }

    @Override
    Set<String> keys(final String key) {
        return this.redisTemplate.keys(key);
    }

    @Override
    List<String> multiGet(final List<String> keys) {
        return this.redisTemplate.opsForValue().multiGet(keys);
    }

    @Override
    List<Object> multiGetResourceTranslations(final List<String> fromKeys) {
        // Pipelining makes sure we don't pay excessive RTT penalties.
        return this.redisTemplate.executePipelined(new RedisCallback<List<Object>>() {
            @Override
            public List<Object> doInRedis(final RedisConnection connection) throws DataAccessException {
                StringRedisConnection stringRedisConn = new DefaultStringRedisConnection(connection);
                for (String fromKey : fromKeys) {
                    stringRedisConn.sMembers(fromKey);
                }
                return null;
            }
        }, new StringRedisSerializer());
    }

    @Override
    void multiSet(final Map<String, String> map) {
        this.redisTemplate.opsForValue().multiSet(map);
    }

    @Override
    void set(final String key, final String value) {
        if (isPolicyEvalResultKey(key)) {
            this.redisTemplate.opsForValue().set(key, value, this.cachedEvalTimeToLiveSeconds, TimeUnit.SECONDS);
        } else {
            this.redisTemplate.opsForValue().set(key, value);
        }
    }

    public void setCachedEvalTimeToLiveSeconds(final long cachedEvalTimeToLiveSeconds) {
        this.cachedEvalTimeToLiveSeconds = cachedEvalTimeToLiveSeconds;
    }

    @Override
    void setResourceTranslation(final String fromKey, final String toKey) {
        this.redisTemplate.opsForSet().add(fromKey, toKey);
    }

    @Override
    void setResourceTranslations(final Set<String> fromKeys, final String toKey) {
        this.redisTemplate.execute(new RedisCallback<List<Object>>() {
            @Override
            public List<Object> doInRedis(final RedisConnection connection) throws DataAccessException {
                StringRedisConnection stringRedisConn = new DefaultStringRedisConnection(connection);
                for (String fromKey : fromKeys) {
                    stringRedisConn.sAdd(fromKey, toKey);
                }
                return null;
            }
        });
    }
}
