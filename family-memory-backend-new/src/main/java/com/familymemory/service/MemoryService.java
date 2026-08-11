package com.familymemory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.familymemory.common.BusinessException;
import com.familymemory.dto.CommentRequest;
import com.familymemory.dto.MemoryRequest;
import com.familymemory.entity.Memory;
import com.familymemory.entity.MemoryComment;
import com.familymemory.entity.MemoryLike;
import com.familymemory.entity.MemoryMedia;
import com.familymemory.entity.MemoryTag;
import com.familymemory.entity.User;
import com.familymemory.mapper.MemoryCommentMapper;
import com.familymemory.mapper.MemoryLikeMapper;
import com.familymemory.mapper.MemoryMapper;
import com.familymemory.mapper.MemoryMediaMapper;
import com.familymemory.mapper.MemoryTagMapper;
import com.familymemory.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MemoryService {
    private final MemoryMapper memoryMapper;
    private final MemoryMediaMapper mediaMapper;
    private final MemoryTagMapper tagMapper;
    private final MemoryLikeMapper likeMapper;
    private final MemoryCommentMapper commentMapper;
    private final UserMapper userMapper;
    private final FamilyService familyService;

    public MemoryService(MemoryMapper m, MemoryMediaMapper media, MemoryTagMapper tag, MemoryLikeMapper like,
                         MemoryCommentMapper comment, UserMapper user, FamilyService family) {
        memoryMapper = m;
        mediaMapper = media;
        tagMapper = tag;
        likeMapper = like;
        commentMapper = comment;
        userMapper = user;
        familyService = family;
    }

    public Map<String, Object> list(Long familyId, Long userId, int page, int size, Integer year, String keyword) {
        familyService.requireMember(familyId, userId);
        LambdaQueryWrapper<Memory> query = new LambdaQueryWrapper<Memory>().eq(Memory::getFamilyId, familyId);
        if (year != null) query.eq(Memory::getMemoryYear, year);
        if (StringUtils.hasText(keyword))
            query.and(q -> q.like(Memory::getTitle, keyword).or().like(Memory::getContent, keyword).or().like(Memory::getLocation, keyword));
        query.orderByDesc(Memory::getMemoryDate).orderByDesc(Memory::getCreatedAt);
        Page<Memory> result = memoryMapper.selectPage(new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 50)),
                query);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total", result.getTotal());
        map.put("page", result.getCurrent());
        map.put("size", result.getSize());
        map.put("list",
                result.getRecords().stream().map(memory -> view(memory, userId, false)).collect(Collectors.toList()));
        return map;
    }

    public Map<String, Object> detail(Long memoryId, Long userId) {
        Memory memory = requireMemory(memoryId);
        familyService.requireMember(memory.getFamilyId(), userId);
        return view(memory, userId, true);
    }

    @Transactional
    public Map<String, Object> create(Long familyId, Long userId, MemoryRequest req) {
        familyService.requireMember(familyId, userId);
        Memory memory = new Memory();
        memory.setFamilyId(familyId);
        memory.setAuthorId(userId);
        memory.setType(StringUtils.hasText(req.getType()) ? req.getType() : "photo");
        memory.setTitle(req.getTitle().trim());
        memory.setContent(req.getContent());
        memory.setMemoryDate(req.getMemoryDate());
        memory.setMemoryYear(req.getMemoryDate().getYear());
        memory.setLocation(req.getLocation());
        memory.setAudioUrl(req.getAudioUrl());
        memory.setAudioDuration(req.getAudioDuration());
        memory.setLikesCount(0);
        memory.setCommentsCount(0);
        memory.setCreatedAt(LocalDateTime.now());
        memory.setUpdatedAt(LocalDateTime.now());
        memory.setDeleted(0);
        memoryMapper.insert(memory);
        if (req.getMediaUrls() != null) {
            int order = 0;
            for (String url : req.getMediaUrls()) {
                if (!StringUtils.hasText(url)) continue;
                MemoryMedia media = new MemoryMedia();
                media.setMemoryId(memory.getId());
                media.setMediaType(isVideo(url) ? "video" : "image");
                media.setUrl(url);
                media.setSortOrder(order++);
                media.setCreatedAt(LocalDateTime.now());
                mediaMapper.insert(media);
            }
        }
        if (req.getTags() != null) for (String name : req.getTags()) {
            if (!StringUtils.hasText(name)) continue;
            MemoryTag tag = new MemoryTag();
            tag.setMemoryId(memory.getId());
            tag.setTagName(name.trim());
            tag.setCreatedAt(LocalDateTime.now());
            tagMapper.insert(tag);
        }
        return view(memory, userId, true);
    }

    @Transactional
    public Map<String, Object> toggleLike(Long memoryId, Long userId) {
        Memory memory = requireMemory(memoryId);
        familyService.requireMember(memory.getFamilyId(), userId);
        MemoryLike like = likeMapper.selectOne(new LambdaQueryWrapper<MemoryLike>().eq(MemoryLike::getMemoryId,
                memoryId).eq(MemoryLike::getUserId, userId));
        boolean liked;
        if (like == null) {
            like = new MemoryLike();
            like.setMemoryId(memoryId);
            like.setUserId(userId);
            like.setCreatedAt(LocalDateTime.now());
            likeMapper.insert(like);
            memory.setLikesCount(memory.getLikesCount() + 1);
            liked = true;
        } else {
            likeMapper.deleteById(like.getId());
            memory.setLikesCount(Math.max(0, memory.getLikesCount() - 1));
            liked = false;
        }
        memoryMapper.updateById(memory);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("liked", liked);
        map.put("likesCount", memory.getLikesCount());
        return map;
    }

    @Transactional
    public Map<String, Object> addComment(Long memoryId, Long userId, CommentRequest req) {
        Memory memory = requireMemory(memoryId);
        familyService.requireMember(memory.getFamilyId(), userId);
        MemoryComment comment = new MemoryComment();
        comment.setMemoryId(memoryId);
        comment.setUserId(userId);
        comment.setContent(req.getContent().trim());
        comment.setCreatedAt(LocalDateTime.now());
        comment.setDeleted(0);
        commentMapper.insert(comment);
        memory.setCommentsCount(memory.getCommentsCount() + 1);
        memoryMapper.updateById(memory);
        return commentView(comment);
    }

    public List<Map<String, Object>> today(Long familyId, Long userId) {
        familyService.requireMember(familyId, userId);
        LocalDate now = LocalDate.now();
        List<Memory> memories = memoryMapper.selectList(new LambdaQueryWrapper<Memory>().eq(Memory::getFamilyId,
                familyId).apply("MONTH(memory_date)={0} AND DAY(memory_date)={1}", now.getMonthValue(),
                now.getDayOfMonth()).lt(Memory::getMemoryYear, now.getYear()).orderByDesc(Memory::getMemoryYear));
        return memories.stream().map(m -> {
            Map<String, Object> map = view(m, userId, false);
            map.put("yearsAgo", now.getYear() - m.getMemoryYear());
            return map;
        }).collect(Collectors.toList());
    }

    public List<Integer> years(Long familyId, Long userId) {
        familyService.requireMember(familyId, userId);
        return memoryMapper.selectObjs(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Memory>().select("DISTINCT memory_year").eq("family_id", familyId).orderByDesc("memory_year")).stream().map(v -> Integer.valueOf(String.valueOf(v))).collect(Collectors.toList());
    }

    public List<Map<String, Object>> timelineOverview(Long familyId, Long userId) {
        familyService.requireMember(familyId, userId);
        List<Map<String, Object>> rows = memoryMapper.selectMaps(new QueryWrapper<Memory>()
                .select("memory_year AS year", "MONTH(memory_date) AS month", "COUNT(*) AS total")
                .eq("family_id", familyId)
                .groupBy("memory_year", "MONTH(memory_date)")
                .orderByDesc("memory_year")
                .orderByAsc("MONTH(memory_date)"));
        Map<Integer, Map<String, Object>> years = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            int year = Integer.parseInt(String.valueOf(row.get("year")));
            int month = Integer.parseInt(String.valueOf(row.get("month")));
            long total = Long.parseLong(String.valueOf(row.get("total")));
            Map<String, Object> yearData = years.computeIfAbsent(year, value -> {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("year", value);
                data.put("count", 0L);
                data.put("months", new ArrayList<Map<String, Object>>());
                return data;
            });
            yearData.put("count", (Long) yearData.get("count") + total);
            Map<String, Object> monthData = new LinkedHashMap<>();
            monthData.put("month", month);
            monthData.put("count", total);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> months = (List<Map<String, Object>>) yearData.get("months");
            months.add(monthData);
        }
        return new ArrayList<>(years.values());
    }

    public List<Map<String, Object>> timelineMonth(Long familyId, Long userId, int year, int month) {
        familyService.requireMember(familyId, userId);
        if (month < 1 || month > 12) throw BusinessException.badRequest("月份必须在1到12之间");
        List<Memory> memories = memoryMapper.selectList(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getFamilyId, familyId)
                .eq(Memory::getMemoryYear, year)
                .apply("MONTH(memory_date)={0}", month)
                .orderByDesc(Memory::getMemoryDate)
                .orderByDesc(Memory::getCreatedAt));
        return memories.stream().map(memory -> view(memory, userId, false)).collect(Collectors.toList());
    }

    public List<Map<String, Object>> timelineStars(Long familyId, Long userId) {
        familyService.requireMember(familyId, userId);
        List<Memory> memories = memoryMapper.selectList(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getFamilyId, familyId)
                .select(Memory::getId, Memory::getMemoryYear, Memory::getMemoryDate,
                        Memory::getLikesCount, Memory::getCommentsCount)
                .orderByDesc(Memory::getMemoryDate));
        return memories.stream().map(memory -> {
            Map<String, Object> star = new LinkedHashMap<>();
            star.put("id", memory.getId());
            star.put("year", memory.getMemoryYear());
            star.put("month", memory.getMemoryDate().getMonthValue());
            star.put("day", memory.getMemoryDate().getDayOfMonth());
            star.put("weight", Math.max(0, memory.getLikesCount()) + Math.max(0, memory.getCommentsCount()));
            return star;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long memoryId, Long userId) {
        Memory memory = requireMemory(memoryId);
        familyService.requireMember(memory.getFamilyId(), userId);
        if (!memory.getAuthorId().equals(userId)) throw BusinessException.forbidden("只能删除自己创建的记忆");
        memoryMapper.deleteById(memoryId);
    }

    private Map<String, Object> view(Memory memory, Long userId, boolean includeComments) {
        User author = userMapper.selectById(memory.getAuthorId());
        List<String> media = mediaMapper.selectList(new LambdaQueryWrapper<MemoryMedia>().eq(MemoryMedia::getMemoryId
                , memory.getId()).orderByAsc(MemoryMedia::getSortOrder)).stream().map(MemoryMedia::getUrl).collect(Collectors.toList());
        List<String> tags = tagMapper.selectList(new LambdaQueryWrapper<MemoryTag>().eq(MemoryTag::getMemoryId,
                memory.getId())).stream().map(MemoryTag::getTagName).collect(Collectors.toList());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", memory.getId());
        map.put("familyId", memory.getFamilyId());
        map.put("type", memory.getType());
        map.put("title", memory.getTitle());
        map.put("content", memory.getContent());
        map.put("memoryDate", memory.getMemoryDate());
        map.put("memoryYear", memory.getMemoryYear());
        map.put("location", memory.getLocation());
        map.put("audioUrl", memory.getAudioUrl());
        map.put("audioDuration", memory.getAudioDuration());
        map.put("mediaUrls", media);
        map.put("coverUrl", media.isEmpty() ? null : media.get(0));
        map.put("tags", tags);
        map.put("likesCount", memory.getLikesCount());
        map.put("commentsCount", memory.getCommentsCount());
        map.put("liked", likeMapper.selectCount(new LambdaQueryWrapper<MemoryLike>().eq(MemoryLike::getMemoryId,
                memory.getId()).eq(MemoryLike::getUserId, userId)) > 0);
        map.put("authorId", memory.getAuthorId());
        map.put("authorName", author == null ? "家人" : author.getNickname());
        map.put("authorAvatar", author == null ? null : author.getAvatar());
        map.put("createdAt", memory.getCreatedAt());
        if (includeComments) {
            map.put("comments",
                    commentMapper.selectList(new LambdaQueryWrapper<MemoryComment>().eq(MemoryComment::getMemoryId,
                            memory.getId()).orderByAsc(MemoryComment::getCreatedAt)).stream().map(this::commentView).collect(Collectors.toList()));
        }
        return map;
    }

    private Map<String, Object> commentView(MemoryComment comment) {
        User user = userMapper.selectById(comment.getUserId());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", comment.getId());
        map.put("content", comment.getContent());
        map.put("userId", comment.getUserId());
        map.put("userName", user == null ? "家人" : user.getNickname());
        map.put("avatar", user == null ? null : user.getAvatar());
        map.put("createdAt", comment.getCreatedAt());
        return map;
    }

    private Memory requireMemory(Long id) {
        Memory memory = memoryMapper.selectById(id);
        if (memory == null) {
            throw BusinessException.notFound("记忆不存在");
        }
        return memory;
    }

    private boolean isVideo(String url) {
        String value = url.toLowerCase();
        return value.endsWith(".mp4") || value.endsWith(".mov") || value.endsWith(".webm");
    }
}
