package com.familymemory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.familymemory.common.BusinessException;
import com.familymemory.dto.LoginRequest;
import com.familymemory.dto.RegisterRequest;
import com.familymemory.entity.Family;
import com.familymemory.entity.FamilyMember;
import com.familymemory.entity.User;
import com.familymemory.mapper.FamilyMapper;
import com.familymemory.mapper.FamilyMemberMapper;
import com.familymemory.mapper.UserMapper;
import com.familymemory.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 认证业务，校验密码并签发JWT，不向客户端返回密码摘要。
 */
@Service
public class AuthService {
    /**
     * 用户账号持久层，对应表：fm_user。
     */
    private final UserMapper userMapper;
    /**
     * 家庭成员关系持久层，对应表：fm_family_member。
     */
    private final FamilyMemberMapper memberMapper;
    /**
     * 家庭空间持久层，对应表：fm_family。
     */
    private final FamilyMapper familyMapper;
    /**
     * BCrypt密码编码器。
     */
    private final PasswordEncoder encoder;
    /**
     * JWT签发和验签服务，令牌主题保存用户ID。
     */
    private final JwtService jwtService;

    /**
     * 初始化 AuthService 的依赖。
     * @param u 用户账号持久层，对应表：fm_user
     * @param m 家庭成员关系持久层，对应表：fm_family_member
     * @param f 家庭空间持久层，对应表：fm_family
     * @param e 密码编码器
     * @param j JWT签发和验签服务，令牌主题保存用户ID
     */
    public AuthService(UserMapper u, FamilyMemberMapper m, FamilyMapper f, PasswordEncoder e, JwtService j) {
        userMapper = u;
        memberMapper = m;
        familyMapper = f;
        encoder = e;
        jwtService = j;
    }

    /**
     * 校验用户名唯一性，保存密码摘要并返回认证资料。
     * @param request 新用户注册请求
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    public Map<String, Object> register(RegisterRequest request) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername,
                request.getUsername()));
        if (count > 0) {
            throw BusinessException.badRequest("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(encoder.encode(request.getPassword()));
        user.setNickname(request.getNickname().trim());
        user.setPhone(request.getPhone());
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);
        userMapper.insert(user);
        return authPayload(user);
    }

    /**
     * 校验账号密码和账号状态，签发认证令牌。
     * @param request 账号密码登录请求
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    public Map<String, Object> login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null || !encoder.matches(request.getPassword(), user.getPassword())) {
            throw BusinessException.badRequest("用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw BusinessException.forbidden("账号已停用");
        }
        return authPayload(user);
    }

    /**
     * 加载用户资料及所属家庭，不返回密码。
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    public Map<String, Object> profile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        Map<String, Object> profile = userView(user);
        List<FamilyMember> memberships =
                memberMapper.selectList(new LambdaQueryWrapper<FamilyMember>().eq(FamilyMember::getUserId, userId).orderByAsc(FamilyMember::getJoinedAt));
        List<Map<String, Object>> families = memberships.stream().map(member -> {
            Family family = familyMapper.selectById(member.getFamilyId());
            Map<String, Object> item = new LinkedHashMap<>();
            // 返回字段 id：记录ID，来源表：fm_family.id。
            item.put("id", family.getId());
            // 返回字段 name：家庭名称。
            item.put("name", family.getName());
            // 返回字段 coverUrl：封面资源地址。
            item.put("coverUrl", family.getCoverUrl());
            // 返回字段 role：家庭角色名称，自由文本。
            item.put("role", member.getRole());
            // 返回字段 admin：管理员标志，true管理员，false普通成员。
            item.put("admin", member.getIsAdmin() == 1);
            return item;
        }).collect(Collectors.toList());
        // 返回字段 families：当前用户所属家庭列表。
        profile.put("families", families);
        // 返回字段 currentFamily：当前家庭，取最早加入的家庭，无家庭时为null。
        profile.put("currentFamily", families.isEmpty() ? null : families.get(0));
        return profile;
    }

    /**
     * 组装令牌和用户基本资料。
     * @param user 用户账号对象，来源表：fm_user
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    private Map<String, Object> authPayload(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        // 返回字段 token：登录JWT，后续请求通过Authorization Bearer携带。
        map.put("token", jwtService.create(user.getId()));
        // 返回字段 user：用户公开资料，不含密码摘要。
        map.put("user", userView(user));
        return map;
    }

    /**
     * 组装可公开的用户资料字段。
     * @param user 用户账号对象，来源表：fm_user
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    private Map<String, Object> userView(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        // 返回字段 id：记录ID，来源表：fm_user.id。
        map.put("id", user.getId());
        // 返回字段 username：登录用户名。
        map.put("username", user.getUsername());
        // 返回字段 nickname：用户显示昵称。
        map.put("nickname", user.getNickname());
        // 返回字段 avatar：用户头像资源地址。
        map.put("avatar", user.getAvatar());
        // 返回字段 phone：手机号码。
        map.put("phone", user.getPhone());
        // 返回字段 email：电子邮箱地址。
        map.put("email", user.getEmail());
        return map;
    }
}
