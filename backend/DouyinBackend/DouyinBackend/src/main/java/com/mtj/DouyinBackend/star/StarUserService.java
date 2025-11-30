package com.mtj.DouyinBackend.star;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class StarUserService {

    // 数据库mapper
    private final StarUserRepository repository;
    // redis工具
    private final StringRedisTemplate redisTemplate;
    // 序列化工具
    private final ObjectMapper objectMapper;

    // 实例化工具
    public StarUserService(StarUserRepository repository, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    // 调用封装好的数据库然后封装到响应体并返回
    public StarUserPageResponse getPage(int page, int size) {
        if (page < 0) {
            page = 0;
        }
        size = 10;

        String cacheKey = "star_page:" + page + ":" + size;
        StarUserPageResponse cached = readFromCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        long total = repository.countAll();
        List<StarUser> users;
        if (total == 0L) {
            users = new ArrayList<>();
        } else {
            users = repository.findPage(page, size);
        }

        StarUserPageResponse response = new StarUserPageResponse();
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        response.setContent(mapToDto(users));

        writeToCache(cacheKey, response);

        return response;
    }
    // 更新单个user
    public void updateUser(long id, UpdateStarUserRequestBody body) {
        repository.updateUser(id, body.getSpecialFollow(), body.getFollowed(), body.getRemark());
        evictPageCache();
    }
    // mock 1k 数据
    public void initDataIfNeeded() {
        long count = repository.countAll();
        if (count >= 1000L) {
            return;
        }
        List<StarUser> users = generateMockUsers(1000);
        repository.insertBatch(users);
        evictPageCache();
    }
    // 将实际的user类转为DTO
    private List<StarUserDto> mapToDto(List<StarUser> users) {
        List<StarUserDto> result = new ArrayList<>();
        for (StarUser user : users) {
            StarUserDto dto = new StarUserDto();
            dto.setId(user.getId());
            dto.setName(user.getName());
            dto.setAvatarIndex(user.getAvatarIndex());
            dto.setVerified(user.getVerified());
            dto.setFollowed(user.getFollowed());
            dto.setDouyinId(user.getDouyinId());
            dto.setSpecialFollow(user.getSpecialFollow());
            dto.setRemark(user.getRemark());
            result.add(dto);
        }
        return result;
    }
    // 生成mock数据函数并以List的形式返回
    private List<StarUser> generateMockUsers(int count) {
        List<String> baseNames = new ArrayList<>();
        baseNames.add("周杰伦");
        baseNames.add("王心凌");
        baseNames.add("张韶涵");
        baseNames.add("张靓颖");
        baseNames.add("五月天");
        baseNames.add("林俊杰");
        baseNames.add("薛之谦");
        baseNames.add("刘德华");
        baseNames.add("杨幂");
        baseNames.add("赵丽颖");
        baseNames.add("邓紫棋");
        baseNames.add("陈奕迅");
        baseNames.add("易烊千玺");
        baseNames.add("王俊凯");
        baseNames.add("蔡依林");

        int avatarCount = 15;
        List<StarUser> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            long id = i + 1L;
            String baseName = baseNames.get(i % baseNames.size());
            int round = (i / baseNames.size()) + 1;
            String name = baseName + " " + round;
            int avatarIndex = (i % avatarCount) + 1;
            boolean verified = (i % 3) != 0;
            boolean followed = true;
            String douyinId = "star" + String.format("%04d", id);
            boolean specialFollow = (i % 10) == 0;
            String remark = "";
            StarUser user = new StarUser(id, name, avatarIndex, verified, followed, douyinId, specialFollow, remark);
            users.add(user);
        }
        return users;
    }
// --------------------------------------------------------------------------------------------------
// redis方法
    private StarUserPageResponse readFromCache(String key) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            return objectMapper.readValue(value, StarUserPageResponse.class);
        } catch (DataAccessException | JsonProcessingException e) {
            return null;
        }
    }

    private void writeToCache(String key, StarUserPageResponse value) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json);
        } catch (DataAccessException | JsonProcessingException e) {
        }
    }

    private void evictPageCache() {
        if (redisTemplate == null) {
            return;
        }
        try {
            Set<String> keys = redisTemplate.keys("star_page:*");
            if (keys == null) {
                keys = new HashSet<>();
            }
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (DataAccessException e) {
        }
    }
}
