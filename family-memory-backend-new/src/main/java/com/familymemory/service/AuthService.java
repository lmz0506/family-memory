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

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final FamilyMemberMapper memberMapper;
    private final FamilyMapper familyMapper;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(UserMapper u, FamilyMemberMapper m, FamilyMapper f, PasswordEncoder e, JwtService j) {
        userMapper = u;
        memberMapper = m;
        familyMapper = f;
        encoder = e;
        jwtService = j;
    }

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
            item.put("id", family.getId());
            item.put("name", family.getName());
            item.put("coverUrl", family.getCoverUrl());
            item.put("role", member.getRole());
            item.put("admin", member.getIsAdmin() == 1);
            return item;
        }).collect(Collectors.toList());
        profile.put("families", families);
        profile.put("currentFamily", families.isEmpty() ? null : families.get(0));
        return profile;
    }

    private Map<String, Object> authPayload(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("token", jwtService.create(user.getId()));
        map.put("user", userView(user));
        return map;
    }

    private Map<String, Object> userView(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("avatar", user.getAvatar());
        map.put("phone", user.getPhone());
        map.put("email", user.getEmail());
        return map;
    }
}
