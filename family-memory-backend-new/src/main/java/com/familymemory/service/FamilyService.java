package com.familymemory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.familymemory.common.BusinessException;
import com.familymemory.dto.FamilyRequest;
import com.familymemory.dto.InviteRequest;
import com.familymemory.dto.JoinFamilyRequest;
import com.familymemory.entity.Family;
import com.familymemory.entity.FamilyMember;
import com.familymemory.entity.InviteCode;
import com.familymemory.entity.Memory;
import com.familymemory.entity.MemoryMedia;
import com.familymemory.entity.TimeCapsule;
import com.familymemory.entity.User;
import com.familymemory.mapper.FamilyMapper;
import com.familymemory.mapper.FamilyMemberMapper;
import com.familymemory.mapper.InviteCodeMapper;
import com.familymemory.mapper.MemoryMapper;
import com.familymemory.mapper.MemoryMediaMapper;
import com.familymemory.mapper.TimeCapsuleMapper;
import com.familymemory.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 家庭成员权限、家庭资料、邀请码及加入家庭业务。
 */
@Service
public class FamilyService {
    /**
     * 家庭空间持久层，对应表：fm_family。
     */
    private final FamilyMapper familyMapper;
    /**
     * 家庭成员关系持久层，对应表：fm_family_member。
     */
    private final FamilyMemberMapper memberMapper;
    /**
     * 用户账号持久层，对应表：fm_user。
     */
    private final UserMapper userMapper;
    /**
     * 家庭邀请码持久层，对应表：fm_invite_code。
     */
    private final InviteCodeMapper inviteMapper;
    /**
     * 家庭记忆持久层，对应表：fm_memory。
     */
    private final MemoryMapper memoryMapper;
    /**
     * 记忆图片与视频持久层，对应表：fm_memory_media。
     */
    private final MemoryMediaMapper mediaMapper;
    /**
     * 珍藏时光胶囊持久层，对应表：fm_time_capsule。
     */
    private final TimeCapsuleMapper capsuleMapper;

    /**
     * 初始化 FamilyService 的依赖。
     * @param f 家庭空间持久层，对应表：fm_family
     * @param m 家庭成员关系持久层，对应表：fm_family_member
     * @param u 用户账号持久层，对应表：fm_user
     * @param i 家庭邀请码持久层，对应表：fm_invite_code
     * @param mm 家庭记忆持久层，对应表：fm_memory
     * @param media 记忆图片与视频持久层，对应表：fm_memory_media
     * @param c 珍藏时光胶囊持久层，对应表：fm_time_capsule
     */
    public FamilyService(FamilyMapper f, FamilyMemberMapper m, UserMapper u, InviteCodeMapper i, MemoryMapper mm,
                         MemoryMediaMapper media, TimeCapsuleMapper c) {
        familyMapper = f;
        memberMapper = m;
        userMapper = u;
        inviteMapper = i;
        memoryMapper = mm;
        mediaMapper = media;
        capsuleMapper = c;
    }

    /**
     * 校验当前用户属于指定家庭。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 当前家庭成员关系，来源表：fm_family_member
     */
    public FamilyMember requireMember(Long familyId, Long userId) {
        FamilyMember member =
                memberMapper.selectOne(new LambdaQueryWrapper<FamilyMember>().eq(FamilyMember::getFamilyId, familyId).eq(FamilyMember::getUserId, userId));
        if (member == null) throw BusinessException.forbidden("你不是该家庭成员");
        return member;
    }

    /**
     * 创建家庭并将创建者加入为管理员。
     * @param req 家庭空间创建请求
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @Transactional
    public Map<String, Object> create(FamilyRequest req, Long userId) {
        Family family = new Family();
        family.setName(req.getName().trim());
        family.setDescription(req.getDescription());
        family.setCoverUrl(req.getCoverUrl());
        family.setCreatorId(userId);
        family.setCreatedAt(LocalDateTime.now());
        family.setUpdatedAt(LocalDateTime.now());
        family.setDeleted(0);
        familyMapper.insert(family);
        User user = userMapper.selectById(userId);
        FamilyMember member = new FamilyMember();
        member.setFamilyId(family.getId());
        member.setUserId(userId);
        member.setRole("家庭管理员");
        member.setDisplayName(user.getNickname());
        member.setColor("peach");
        member.setIsAdmin(1);
        member.setJoinedAt(LocalDateTime.now());
        member.setCreatedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());
        member.setDeleted(0);
        memberMapper.insert(member);
        return detail(family.getId(), userId);
    }

    /**
     * 查询当前用户所属家庭列表。
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    public List<Map<String, Object>> list(Long userId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (FamilyMember member :
                memberMapper.selectList(new LambdaQueryWrapper<FamilyMember>().eq(FamilyMember::getUserId, userId))) {
            result.add(detail(member.getFamilyId(), userId));
        }
        return result;
    }

    /**
     * 查询家庭资料和有效内容数量。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    public Map<String, Object> detail(Long familyId, Long userId) {
        requireMember(familyId, userId);
        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            throw BusinessException.notFound("家庭不存在");
        }
        long memberCount =
                memberMapper.selectCount(new LambdaQueryWrapper<FamilyMember>().eq(FamilyMember::getFamilyId,
                        familyId));
        long memoryCount = memoryMapper.selectCount(new LambdaQueryWrapper<Memory>().eq(Memory::getFamilyId, familyId));
        long photoCount = 0;
        List<Memory> memories = memoryMapper.selectList(new LambdaQueryWrapper<Memory>().eq(Memory::getFamilyId,
                familyId).select(Memory::getId));
        if (!memories.isEmpty()) {
            photoCount = mediaMapper.selectCount(new LambdaQueryWrapper<MemoryMedia>().in(MemoryMedia::getMemoryId,
                    memories.stream().map(Memory::getId).toArray()));
        }
        long capsuleCount =
                capsuleMapper.selectCount(new LambdaQueryWrapper<TimeCapsule>().eq(TimeCapsule::getFamilyId,
                        familyId).eq(TimeCapsule::getIsOpened, 0));
        Map<String, Object> map = new LinkedHashMap<>();
        // 返回字段 id：记录ID，来源表：fm_family.id。
        map.put("id", family.getId());
        // 返回字段 name：家庭名称。
        map.put("name", family.getName());
        // 返回字段 description：家庭介绍。
        map.put("description", family.getDescription());
        // 返回字段 coverUrl：封面资源地址。
        map.put("coverUrl", family.getCoverUrl());
        // 返回字段 createdAt：业务记录创建时间。
        map.put("createdAt", family.getCreatedAt());
        // 返回字段 memberCount：有效家庭成员数量。
        map.put("memberCount", memberCount);
        // 返回字段 memoryCount：有效记忆数量。
        map.put("memoryCount", memoryCount);
        // 返回字段 photoCount：图片类型媒体数量。
        map.put("photoCount", photoCount);
        // 返回字段 capsuleCount：有效珍藏数量。
        map.put("capsuleCount", capsuleCount);
        // 返回字段 recordDays：家庭建立至今天数，建立当天计为1。
        map.put("recordDays", family.getCreatedAt() == null ? 0 :
                ChronoUnit.DAYS.between(family.getCreatedAt().toLocalDate(), LocalDate.now()) + 1);
        return map;
    }

    /**
     * 查询家庭成员称呼、角色及发布数量。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    public List<Map<String, Object>> members(Long familyId, Long userId) {
        requireMember(familyId, userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (FamilyMember member :
                memberMapper.selectList(new LambdaQueryWrapper<FamilyMember>().eq(FamilyMember::getFamilyId,
                        familyId).orderByAsc(FamilyMember::getJoinedAt))) {
            User user = userMapper.selectById(member.getUserId());
            Map<String, Object> map = new LinkedHashMap<>();
            // 返回字段 id：记录ID，来源表：fm_family_member.id。
            map.put("id", member.getId());
            // 返回字段 userId：用户ID，来源表：fm_user.id。
            map.put("userId", member.getUserId());
            // 返回字段 displayName：家庭内称呼。
            map.put("displayName", member.getDisplayName());
            // 返回字段 role：家庭角色名称，自由文本。
            map.put("role", member.getRole());
            // 返回字段 color：成员主题色，peach桃色，sky天蓝，amber琥珀，emerald翠绿，可扩展。
            map.put("color", member.getColor());
            // 返回字段 admin：管理员标志，true管理员，false普通成员。
            map.put("admin", member.getIsAdmin() == 1);
            // 返回字段 avatar：用户头像资源地址。
            map.put("avatar", user == null ? null : user.getAvatar());
            // 返回字段 memoryCount：有效记忆数量。
            map.put("memoryCount", memoryMapper.selectCount(new LambdaQueryWrapper<Memory>().eq(Memory::getFamilyId,
                    familyId).eq(Memory::getAuthorId, member.getUserId())));
            result.add(map);
        }
        return result;
    }

    /**
     * 仅允许家庭管理员生成有限期及使用次数的邀请码。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param req 家庭邀请码生成参数
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    public Map<String, Object> invite(Long familyId, InviteRequest req, Long userId) {
        FamilyMember member = requireMember(familyId, userId);
        if (member.getIsAdmin() != 1) throw BusinessException.forbidden("只有管理员可以邀请成员");
        InviteCode code = new InviteCode();
        code.setFamilyId(familyId);
        code.setCreatorId(userId);
        code.setCode(randomCode());
        code.setExpireAt(LocalDateTime.now().plusHours(Math.max(1, req.getExpireHours())));
        code.setMaxUses(Math.max(1, req.getMaxUses()));
        code.setUsedCount(0);
        code.setStatus(1);
        code.setCreatedAt(LocalDateTime.now());
        inviteMapper.insert(code);
        Map<String, Object> map = new LinkedHashMap<>();
        // 返回字段 code：家庭邀请码，来源表：fm_invite_code.code。
        map.put("code", code.getCode());
        // 返回字段 expireAt：邀请码到期时间。
        map.put("expireAt", code.getExpireAt());
        // 返回字段 maxUses：邀请码允许使用次数。
        map.put("maxUses", code.getMaxUses());
        return map;
    }

    /**
     * 校验邀请码有效性并加入家庭。
     * @param req 使用邀请码加入家庭的请求
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @Transactional
    public Map<String, Object> join(JoinFamilyRequest req, Long userId) {
        InviteCode code = inviteMapper.selectOne(new LambdaQueryWrapper<InviteCode>().eq(InviteCode::getCode,
                req.getInviteCode().trim().toUpperCase()));
        if (code == null || code.getStatus() != 1 || code.getExpireAt().isBefore(LocalDateTime.now()) || code.getUsedCount() >= code.getMaxUses())
            throw BusinessException.badRequest("邀请码无效或已过期");
        if (memberMapper.selectCount(new LambdaQueryWrapper<FamilyMember>().eq(FamilyMember::getFamilyId,
                code.getFamilyId()).eq(FamilyMember::getUserId, userId)) > 0)
            throw BusinessException.badRequest("你已经在这个家庭中");
        User user = userMapper.selectById(userId);
        FamilyMember member = new FamilyMember();
        member.setFamilyId(code.getFamilyId());
        member.setUserId(userId);
        member.setRole(req.getRole() == null ? "家庭成员" : req.getRole());
        member.setDisplayName(req.getDisplayName() == null || req.getDisplayName().trim().isEmpty() ?
                user.getNickname() : req.getDisplayName());
        member.setColor("sky");
        member.setIsAdmin(0);
        member.setJoinedAt(LocalDateTime.now());
        member.setCreatedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());
        member.setDeleted(0);
        memberMapper.insert(member);
        code.setUsedCount(code.getUsedCount() + 1);
        if (code.getUsedCount() >= code.getMaxUses()) code.setStatus(0);
        inviteMapper.updateById(code);
        return detail(code.getFamilyId(), userId);
    }

    /**
     * 生成8位家庭邀请码。
     * @return 随机8位邀请码字符串
     */
    private String randomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder s = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < 8; i++) s.append(chars.charAt(r.nextInt(chars.length())));
        return s.toString();
    }
}
