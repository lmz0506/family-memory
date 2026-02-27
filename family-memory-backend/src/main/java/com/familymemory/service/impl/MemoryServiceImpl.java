package com.familymemory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.familymemory.common.BusinessException;
import com.familymemory.common.FileUrlHelper;
import com.familymemory.common.PageResult;
import com.familymemory.dto.req.CreateMemoryReq;
import com.familymemory.dto.req.MemoryQueryReq;
import com.familymemory.dto.resp.MemoryResp;
import com.familymemory.entity.*;
import com.familymemory.mapper.*;
import com.familymemory.service.MemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 记忆服务实现
 */
@Service
@RequiredArgsConstructor
public class MemoryServiceImpl implements MemoryService {

    private final MemoryMapper memoryMapper;
    private final MemoryMediaMapper memoryMediaMapper;
    private final MemoryTagMapper memoryTagMapper;
    private final MemoryLikeMapper memoryLikeMapper;
    private final UserMapper userMapper;
    private final FamilyMemberMapper familyMemberMapper;

    @Override
    public PageResult<MemoryResp> getMemories(Long familyId, MemoryQueryReq req, Long userId) {
        LambdaQueryWrapper<Memory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Memory::getFamilyId, familyId);

        // 年份筛选
        if (StringUtils.hasText(req.getYear())) {
            wrapper.eq(Memory::getMemoryYear, req.getYear());
        }
        // 类型筛选
        if (StringUtils.hasText(req.getType())) {
            wrapper.eq(Memory::getType, req.getType());
        }
        // 关键字搜索（标题、内容）
        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.and(w -> w.like(Memory::getTitle, req.getKeyword())
                    .or().like(Memory::getDescription, req.getKeyword()));
        }
        // 发布人筛选
        if (req.getAuthorId() != null) {
            wrapper.eq(Memory::getAuthorId, req.getAuthorId());
        }
        // 地点关键字筛选
        if (StringUtils.hasText(req.getLocation())) {
            wrapper.like(Memory::getLocation, req.getLocation());
        }
        // 标签筛选 - 需要关联查询
        if (StringUtils.hasText(req.getTag())) {
            // 先查询包含该标签的记忆ID
            LambdaQueryWrapper<MemoryTag> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.like(MemoryTag::getTagName, req.getTag());
            List<MemoryTag> tags = memoryTagMapper.selectList(tagWrapper);
            if (tags.isEmpty()) {
                // 没有匹配的标签，返回空结果
                return PageResult.of(0L, req.getPage(), req.getSize(), Collections.emptyList());
            }
            List<Long> memoryIds = tags.stream().map(MemoryTag::getMemoryId).distinct().collect(Collectors.toList());
            wrapper.in(Memory::getId, memoryIds);
        }

        wrapper.orderByDesc(Memory::getMemoryDate, Memory::getCreatedAt);

        Page<Memory> page = new Page<>(req.getPage(), req.getSize());
        Page<Memory> result = memoryMapper.selectPage(page, wrapper);

        List<MemoryResp> list = result.getRecords().stream()
                .map(memory -> convertToResp(memory, userId))
                .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), req.getPage(), req.getSize(), list);
    }

    @Override
    public MemoryResp getMemoryById(Long memoryId, Long userId) {
        Memory memory = memoryMapper.selectById(memoryId);
        if (memory == null) {
            throw new BusinessException(404, "记忆不存在");
        }
        return convertToResp(memory, userId);
    }

    @Override
    @Transactional
    public MemoryResp createMemory(Long familyId, CreateMemoryReq req, Long userId) {
        // 获取用户在家庭中的昵称
        LambdaQueryWrapper<FamilyMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(FamilyMember::getFamilyId, familyId);
        memberWrapper.eq(FamilyMember::getUserId, userId);
        FamilyMember member = familyMemberMapper.selectOne(memberWrapper);


        // 使用 LocalDateTime 解析包含时间的字符串
        LocalDateTime memoryDateTime = LocalDateTime.parse(req.getMemoryDate());

        // 提取日期部分
        LocalDate memoryDate = memoryDateTime.toLocalDate();

        // 创建记忆
        Memory memory = new Memory();
        memory.setFamilyId(familyId);
        memory.setAuthorId(userId);
        memory.setType(req.getType());
        memory.setTitle(req.getTitle());
        memory.setDescription(req.getDescription());
        memory.setMemoryDate(memoryDate);
        memory.setMemoryTime(memoryDateTime);
        memory.setMemoryYear(String.valueOf(memoryDate.getYear()));
        memory.setLocation(req.getLocation());
        memory.setAudioUrl(req.getAudioUrl());
        memory.setAudioDuration(req.getAudioDuration());
        memory.setLikesCount(0);
        memoryMapper.insert(memory);

        // 保存媒体文件
        if (req.getMediaUrls() != null && !req.getMediaUrls().isEmpty()) {
            int sortOrder = 0;
            for (String url : req.getMediaUrls()) {
                MemoryMedia media = new MemoryMedia();
                media.setMemoryId(memory.getId());
                media.setUrl(url);
                media.setMediaType(url.contains("video") ? "video" : "image");
                media.setSortOrder(sortOrder++);
                memoryMediaMapper.insert(media);
            }
        }

        // 保存标签
        if (req.getTags() != null && !req.getTags().isEmpty()) {
            for (String tagName : req.getTags()) {
                MemoryTag tag = new MemoryTag();
                tag.setMemoryId(memory.getId());
                tag.setTagName(tagName);
                memoryTagMapper.insert(tag);
            }
        }

        return convertToResp(memory, userId);
    }

    @Override
    @Transactional
    public MemoryResp likeMemory(Long memoryId, Long userId) {
        Memory memory = memoryMapper.selectById(memoryId);
        if (memory == null) {
            throw new BusinessException(404, "记忆不存在");
        }

        // 检查是否已点赞
        LambdaQueryWrapper<MemoryLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemoryLike::getMemoryId, memoryId);
        wrapper.eq(MemoryLike::getUserId, userId);
        if (memoryLikeMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("您已点赞过该记忆");
        }

        // 添加点赞记录
        MemoryLike like = new MemoryLike();
        like.setMemoryId(memoryId);
        like.setUserId(userId);
        memoryLikeMapper.insert(like);

        // 更新点赞数
        memory.setLikesCount(memory.getLikesCount() + 1);
        memoryMapper.updateById(memory);

        return convertToResp(memory, userId);
    }

    @Override
    @Transactional
    public MemoryResp unlikeMemory(Long memoryId, Long userId) {
        Memory memory = memoryMapper.selectById(memoryId);
        if (memory == null) {
            throw new BusinessException(404, "记忆不存在");
        }

        // 删除点赞记录
        LambdaQueryWrapper<MemoryLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemoryLike::getMemoryId, memoryId);
        wrapper.eq(MemoryLike::getUserId, userId);
        int deleted = memoryLikeMapper.delete(wrapper);

        if (deleted > 0) {
            // 更新点赞数
            memory.setLikesCount(Math.max(0, memory.getLikesCount() - 1));
            memoryMapper.updateById(memory);
        }

        return convertToResp(memory, userId);
    }

    @Override
    public List<MemoryResp> getTodayInHistory(Long familyId, Long userId) {
        LocalDate today = LocalDate.now();
        String monthDay = String.format("%02d-%02d", today.getMonthValue(), today.getDayOfMonth());

        LambdaQueryWrapper<Memory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Memory::getFamilyId, familyId);
//        wrapper.lt(Memory::getMemoryYear, today.getYear());
        wrapper.apply("DATE_FORMAT(memory_date, '%m-%d') = {0}", monthDay);
        wrapper.orderByDesc(Memory::getMemoryYear);

        List<Memory> memories = memoryMapper.selectList(wrapper);

        int currentYear = today.getYear();
        return memories.stream().map(memory -> {
            MemoryResp resp = convertToResp(memory, userId);
            int memoryYear = Integer.parseInt(memory.getMemoryYear());
            resp.setYearsAgo(currentYear - memoryYear);
            return resp;
        }).collect(Collectors.toList());
    }

    @Override
    public List<String> getYears(Long familyId) {
        return memoryMapper.selectYearsByFamilyId(familyId);
    }

    @Override
    @Transactional
    public void deleteMemory(Long memoryId, Long userId) {
        Memory memory = memoryMapper.selectById(memoryId);
        if (memory == null) {
            throw new BusinessException(404, "记忆不存在");
        }

        // 检查是否是作者本人
        if (!memory.getAuthorId().equals(userId)) {
            throw new BusinessException(403, "只能删除自己创建的记忆");
        }

        // 删除关联的媒体文件记录
        LambdaQueryWrapper<MemoryMedia> mediaWrapper = new LambdaQueryWrapper<>();
        mediaWrapper.eq(MemoryMedia::getMemoryId, memoryId);
        memoryMediaMapper.delete(mediaWrapper);

        // 删除关联的标签
        LambdaQueryWrapper<MemoryTag> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(MemoryTag::getMemoryId, memoryId);
        memoryTagMapper.delete(tagWrapper);

        // 删除关联的点赞
        LambdaQueryWrapper<MemoryLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(MemoryLike::getMemoryId, memoryId);
        memoryLikeMapper.delete(likeWrapper);

        // 删除记忆本身
        memoryMapper.deleteById(memoryId);
    }

    private MemoryResp convertToResp(Memory memory, Long userId) {
        MemoryResp resp = new MemoryResp();
        resp.setId(memory.getId());
        resp.setType(memory.getType());
        resp.setTitle(memory.getTitle());
        resp.setDescription(memory.getDescription());
        resp.setDate(memory.getMemoryDate().toString());
        resp.setYear(memory.getMemoryYear());
        resp.setLocation(memory.getLocation());
        // 音频URL拼接完整路径
        resp.setAudioUrl(FileUrlHelper.toFullUrl(memory.getAudioUrl()));
        resp.setAudioDuration(memory.getAudioDuration());
        resp.setLikes(memory.getLikesCount());
        resp.setCreatedAt(memory.getCreatedAt() != null ?
                memory.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        resp.setMemoryTime(memory.getMemoryTime() != null ?
                memory.getMemoryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);

        // 获取作者信息
        User author = userMapper.selectById(memory.getAuthorId());
        if (author != null) {
            resp.setAuthorId(author.getId());
            resp.setAvatar(FileUrlHelper.toFullUrl(author.getAvatar()));

            // 获取在家庭中的昵称
            LambdaQueryWrapper<FamilyMember> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.eq(FamilyMember::getFamilyId, memory.getFamilyId());
            memberWrapper.eq(FamilyMember::getUserId, memory.getAuthorId());
            FamilyMember member = familyMemberMapper.selectOne(memberWrapper);
            resp.setAuthor(member != null ? member.getNickname() : author.getNickname());
        }

        // 获取媒体文件，并拼接完整URL
        LambdaQueryWrapper<MemoryMedia> mediaWrapper = new LambdaQueryWrapper<>();
        mediaWrapper.eq(MemoryMedia::getMemoryId, memory.getId());
        mediaWrapper.orderByAsc(MemoryMedia::getSortOrder);
        List<MemoryMedia> mediaList = memoryMediaMapper.selectList(mediaWrapper);
        resp.setImages(mediaList.stream()
                .map(m -> FileUrlHelper.toFullUrl(m.getUrl()))
                .collect(Collectors.toList()));

        // 获取标签
        LambdaQueryWrapper<MemoryTag> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(MemoryTag::getMemoryId, memory.getId());
        List<MemoryTag> tagList = memoryTagMapper.selectList(tagWrapper);
        resp.setTags(tagList.stream().map(MemoryTag::getTagName).collect(Collectors.toList()));

        // 检查是否已点赞
        if (userId != null) {
            LambdaQueryWrapper<MemoryLike> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(MemoryLike::getMemoryId, memory.getId());
            likeWrapper.eq(MemoryLike::getUserId, userId);
            resp.setIsLiked(memoryLikeMapper.selectCount(likeWrapper) > 0);
        } else {
            resp.setIsLiked(false);
        }

        return resp;
    }
}
