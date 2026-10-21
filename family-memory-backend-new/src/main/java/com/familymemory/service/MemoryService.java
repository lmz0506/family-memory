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

/**
 * 家庭记忆业务，发布、改标题和删除同事务维护星海索引。
 */
@Service
public class MemoryService {
    /**
     * 家庭记忆持久层，对应表：fm_memory。
     */
    private final MemoryMapper memoryMapper;
    /**
     * 记忆图片与视频持久层，对应表：fm_memory_media。
     */
    private final MemoryMediaMapper mediaMapper;
    /**
     * 记忆标签持久层，对应表：fm_memory_tag。
     */
    private final MemoryTagMapper tagMapper;
    /**
     * 记忆点赞持久层，对应表：fm_memory_like。
     */
    private final MemoryLikeMapper likeMapper;
    /**
     * 记忆评论持久层，对应表：fm_memory_comment。
     */
    private final MemoryCommentMapper commentMapper;
    /**
     * 用户账号持久层，对应表：fm_user。
     */
    private final UserMapper userMapper;
    /**
     * 家庭成员权限、家庭资料、邀请码及加入家庭业务。
     */
    private final FamilyService familyService;
    /**
     * 星海索引同事务写入服务。
     */
    private final UniverseIndexService universeIndexService;

    /**
     * 初始化 MemoryService 的依赖。
     * @param m 家庭记忆持久层，对应表：fm_memory
     * @param media 记忆图片与视频持久层，对应表：fm_memory_media
     * @param tag 记忆标签持久层，对应表：fm_memory_tag
     * @param like 记忆点赞持久层，对应表：fm_memory_like
     * @param comment 记忆评论持久层，对应表：fm_memory_comment
     * @param user 用户账号持久层，对应表：fm_user
     * @param family 家庭成员权限、家庭资料、邀请码及加入家庭业务
     * @param universeIndexService 星海索引同事务写入服务
     */
    public MemoryService(MemoryMapper m, MemoryMediaMapper media, MemoryTagMapper tag, MemoryLikeMapper like,
                         MemoryCommentMapper comment, UserMapper user, FamilyService family, UniverseIndexService universeIndexService) {
        memoryMapper = m;
        mediaMapper = media;
        tagMapper = tag;
        likeMapper = like;
        commentMapper = comment;
        userMapper = user;
        familyService = family;
        this.universeIndexService = universeIndexService;
    }

    /**
     * 按家庭、年份及关键词分页查询传统记忆列表。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @param page 页码，从1开始
     * @param size 请求分页大小，服务端限制最大值
     * @param year 目标年份
     * @param keyword 搜索关键词，空值表示不限制关键词
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
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
        // 返回字段 total：匹配总条数。
        map.put("total", result.getTotal());
        // 返回字段 page：当前页码，从1开始。
        map.put("page", result.getCurrent());
        // 返回字段 size：实际分页大小。
        map.put("size", result.getSize());
        // 返回字段 list：当前页记录列表。
        map.put("list",
                result.getRecords().stream().map(memory -> view(memory, userId, false)).collect(Collectors.toList()));
        return map;
    }

    /**
     * 校验家庭权限后读取记忆详情及评论。
     * @param memoryId 记忆ID，来源表：fm_memory.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    public Map<String, Object> detail(Long memoryId, Long userId) {
        Memory memory = requireMemory(memoryId);
        familyService.requireMember(memory.getFamilyId(), userId);
        return view(memory, userId, true);
    }

    /**
     * 发布记忆并在同一事务中同步星海索引及媒体标签。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @param req 记忆发布请求，包含发生日期、正文及媒体资源地址
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
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
        universeIndexService.sync(familyId, "MEMORY", memory.getId(), memory.getMemoryDate(), memory.getTitle());
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

    /**
     * 切换当前用户点赞状态并维护点赞数。
     * @param memoryId 记忆ID，来源表：fm_memory.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
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
        // 返回字段 liked：当前用户点赞状态，true已点赞，false未点赞。
        map.put("liked", liked);
        // 返回字段 likesCount：累计点赞数。
        map.put("likesCount", memory.getLikesCount());
        return map;
    }

    /**
     * 发布评论并维护记忆评论数。
     * @param memoryId 记忆ID，来源表：fm_memory.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @param req 记忆评论发布请求
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
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

    /**
     * 查询当前家庭往年同月同日的记忆。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    public List<Map<String, Object>> today(Long familyId, Long userId) {
        familyService.requireMember(familyId, userId);
        LocalDate now = LocalDate.now();
        List<Memory> memories = memoryMapper.selectList(new LambdaQueryWrapper<Memory>().eq(Memory::getFamilyId,
                familyId).apply("MONTH(memory_date)={0} AND DAY(memory_date)={1}", now.getMonthValue(),
                now.getDayOfMonth()).lt(Memory::getMemoryYear, now.getYear()).orderByDesc(Memory::getMemoryYear));
        return memories.stream().map(m -> {
            Map<String, Object> map = view(m, userId, false);
            // 返回字段 yearsAgo：距记忆年份的年数。
            map.put("yearsAgo", now.getYear() - m.getMemoryYear());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 查询当前家庭有记忆的年份。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 有有效记忆的年份列表，按年份降序排列
     */
    public List<Integer> years(Long familyId, Long userId) {
        familyService.requireMember(familyId, userId);
        return memoryMapper.selectObjs(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Memory>().select("DISTINCT memory_year").eq("family_id", familyId).orderByDesc("memory_year")).stream().map(v -> Integer.valueOf(String.valueOf(v))).collect(Collectors.toList());
    }

    /**
     * 返回传统记忆时间轴年月计数，仅供兼容接口使用。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
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
                // 返回字段 year：年份。
                data.put("year", value);
                // 返回字段 count：当前时间层级的内容数量。
                data.put("count", 0L);
                // 返回字段 months：月份计数列表。
                data.put("months", new ArrayList<Map<String, Object>>());
                return data;
            });
            // 返回字段 count：当前时间层级的内容数量。
            yearData.put("count", (Long) yearData.get("count") + total);
            Map<String, Object> monthData = new LinkedHashMap<>();
            // 返回字段 month：月份，1至12。
            monthData.put("month", month);
            // 返回字段 count：当前时间层级的内容数量。
            monthData.put("count", total);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> months = (List<Map<String, Object>>) yearData.get("months");
            months.add(monthData);
        }
        return new ArrayList<>(years.values());
    }

    /**
     * 读取传统时间轴某月记忆列表，仅供兼容接口使用。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @param year 目标年份
     * @param month 月份，1至12
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
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

    /**
     * 读取旧版逐条记忆星点索引，新版星海不再调用。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    public List<Map<String, Object>> timelineStars(Long familyId, Long userId) {
        familyService.requireMember(familyId, userId);
        List<Memory> memories = memoryMapper.selectList(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getFamilyId, familyId)
                .select(Memory::getId, Memory::getMemoryYear, Memory::getMemoryDate,
                        Memory::getLikesCount, Memory::getCommentsCount)
                .orderByDesc(Memory::getMemoryDate));
        return memories.stream().map(memory -> {
            Map<String, Object> star = new LinkedHashMap<>();
            // 返回字段 id：记录ID，来源表：fm_memory.id。
            star.put("id", memory.getId());
            // 返回字段 year：年份。
            star.put("year", memory.getMemoryYear());
            // 返回字段 month：月份，1至12。
            star.put("month", memory.getMemoryDate().getMonthValue());
            // 返回字段 day：月内日期，1至31。
            star.put("day", memory.getMemoryDate().getDayOfMonth());
            // 返回字段 weight：旧星点权重，点赞数与评论数之和。
            star.put("weight", Math.max(0, memory.getLikesCount()) + Math.max(0, memory.getCommentsCount()));
            return star;
        }).collect(Collectors.toList());
    }

    /**
     * 校验发布者权限，同事务逻辑删除记忆和星海索引。
     * @param memoryId 记忆ID，来源表：fm_memory.id
     * @param userId 当前用户ID，来源表：fm_user.id
     */
    @Transactional
    public void delete(Long memoryId, Long userId) {
        Memory memory = requireMemory(memoryId);
        familyService.requireMember(memory.getFamilyId(), userId);
        if (!memory.getAuthorId().equals(userId)) throw BusinessException.forbidden("只能删除自己创建的记忆");
        memoryMapper.deleteById(memoryId);
        universeIndexService.delete(memory.getFamilyId(), "MEMORY", memoryId);
    }

    /**
     * 修改本人发布的记忆标题，并同步星海索引；不接受日期修改。
     * @param memoryId 记忆ID，来源表：fm_memory.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @param title 经请求校验的新标题
     */
    @Transactional
    public void updateTitle(Long memoryId, Long userId, String title) {
        Memory memory = requireMemory(memoryId);
        familyService.requireMember(memory.getFamilyId(), userId);
        if (!memory.getAuthorId().equals(userId)) throw BusinessException.forbidden("只能修改自己创建的记忆");
        memory.setTitle(title.trim());
        memory.setUpdatedAt(LocalDateTime.now());
        memoryMapper.updateById(memory);
        universeIndexService.sync(memory.getFamilyId(), "MEMORY", memoryId, memory.getMemoryDate(), memory.getTitle());
    }

    /**
     * 组装记忆展示字段，可选加载评论。
     * @param memory 家庭记忆对象，来源表：fm_memory
     * @param userId 当前用户ID，来源表：fm_user.id
     * @param includeComments 是否加载评论，true加载，false不加载
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    private Map<String, Object> view(Memory memory, Long userId, boolean includeComments) {
        User author = userMapper.selectById(memory.getAuthorId());
        List<String> media = mediaMapper.selectList(new LambdaQueryWrapper<MemoryMedia>().eq(MemoryMedia::getMemoryId
                , memory.getId()).orderByAsc(MemoryMedia::getSortOrder)).stream().map(MemoryMedia::getUrl).collect(Collectors.toList());
        List<String> tags = tagMapper.selectList(new LambdaQueryWrapper<MemoryTag>().eq(MemoryTag::getMemoryId,
                memory.getId())).stream().map(MemoryTag::getTagName).collect(Collectors.toList());
        Map<String, Object> map = new LinkedHashMap<>();
        // 返回字段 id：记录ID，来源表：fm_memory.id。
        map.put("id", memory.getId());
        // 返回字段 familyId：家庭ID，来源表：fm_family.id。
        map.put("familyId", memory.getFamilyId());
        // 返回字段 type：记忆类型，photo照片，video视频，note文字，audio声音。
        map.put("type", memory.getType());
        // 返回字段 title：业务标题。
        map.put("title", memory.getTitle());
        // 返回字段 content：正文内容。
        map.put("content", memory.getContent());
        // 返回字段 memoryDate：记忆发生日期，来源表：fm_memory.memory_date。
        map.put("memoryDate", memory.getMemoryDate());
        // 返回字段 memoryYear：记忆年份，来源表：fm_memory.memory_year。
        map.put("memoryYear", memory.getMemoryYear());
        // 返回字段 location：记忆发生地点。
        map.put("location", memory.getLocation());
        // 返回字段 audioUrl：音频资源地址。
        map.put("audioUrl", memory.getAudioUrl());
        // 返回字段 audioDuration：音频时长，单位秒。
        map.put("audioDuration", memory.getAudioDuration());
        // 返回字段 mediaUrls：按顺序排列的媒体资源地址。
        map.put("mediaUrls", media);
        // 返回字段 coverUrl：封面资源地址。
        map.put("coverUrl", media.isEmpty() ? null : media.get(0));
        // 返回字段 tags：标签名称列表。
        map.put("tags", tags);
        // 返回字段 likesCount：累计点赞数。
        map.put("likesCount", memory.getLikesCount());
        // 返回字段 commentsCount：累计评论数。
        map.put("commentsCount", memory.getCommentsCount());
        // 返回字段 liked：当前用户点赞状态，true已点赞，false未点赞。
        map.put("liked", likeMapper.selectCount(new LambdaQueryWrapper<MemoryLike>().eq(MemoryLike::getMemoryId,
                memory.getId()).eq(MemoryLike::getUserId, userId)) > 0);
        // 返回字段 authorId：发布者ID，来源表：fm_user.id。
        map.put("authorId", memory.getAuthorId());
        // 返回字段 authorName：发布者昵称，来源表：fm_user.nickname。
        map.put("authorName", author == null ? "家人" : author.getNickname());
        // 返回字段 authorAvatar：发布者头像地址，来源表：fm_user.avatar。
        map.put("authorAvatar", author == null ? null : author.getAvatar());
        // 返回字段 createdAt：业务记录创建时间。
        map.put("createdAt", memory.getCreatedAt());
        if (includeComments) {
            // 返回字段 comments：评论及评论者公开资料列表。
            map.put("comments",
                    commentMapper.selectList(new LambdaQueryWrapper<MemoryComment>().eq(MemoryComment::getMemoryId,
                            memory.getId()).orderByAsc(MemoryComment::getCreatedAt)).stream().map(this::commentView).collect(Collectors.toList()));
        }
        return map;
    }

    /**
     * 组装评论正文及评论者公开资料。
     * @param comment 记忆评论对象，来源表：fm_memory_comment
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    private Map<String, Object> commentView(MemoryComment comment) {
        User user = userMapper.selectById(comment.getUserId());
        Map<String, Object> map = new LinkedHashMap<>();
        // 返回字段 id：记录ID，来源表：fm_memory_comment.id。
        map.put("id", comment.getId());
        // 返回字段 content：正文内容。
        map.put("content", comment.getContent());
        // 返回字段 userId：用户ID，来源表：fm_user.id。
        map.put("userId", comment.getUserId());
        // 返回字段 userName：用户昵称，来源表：fm_user.nickname。
        map.put("userName", user == null ? "家人" : user.getNickname());
        // 返回字段 avatar：用户头像资源地址。
        map.put("avatar", user == null ? null : user.getAvatar());
        // 返回字段 createdAt：业务记录创建时间。
        map.put("createdAt", comment.getCreatedAt());
        return map;
    }

    /**
     * 读取未删除记忆，不存在时抛出404异常。
     * @param id 业务ID，来源表：fm_memory.id
     * @return 未删除的记忆记录，来源表：fm_memory
     */
    private Memory requireMemory(Long id) {
        Memory memory = memoryMapper.selectById(id);
        if (memory == null) {
            throw BusinessException.notFound("记忆不存在");
        }
        return memory;
    }

    /**
     * 按资源地址扩展名判断是否为视频。
     * @param url 资源地址，用于识别文件扩展名
     * @return true视频资源，false其他资源
     */
    private boolean isVideo(String url) {
        String value = url.toLowerCase();
        return value.endsWith(".mp4") || value.endsWith(".mov") || value.endsWith(".webm");
    }
}
