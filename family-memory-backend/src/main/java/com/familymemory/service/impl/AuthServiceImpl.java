package com.familymemory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.familymemory.common.BusinessException;
import com.familymemory.common.FileUrlHelper;
import com.familymemory.dto.req.LoginReq;
import com.familymemory.dto.req.RegisterReq;
import com.familymemory.dto.req.UpdateProfileReq;
import com.familymemory.dto.resp.LoginResp;
import com.familymemory.dto.resp.UserResp;
import com.familymemory.entity.Family;
import com.familymemory.entity.FamilyMember;
import com.familymemory.entity.User;
import com.familymemory.mapper.FamilyMapper;
import com.familymemory.mapper.FamilyMemberMapper;
import com.familymemory.mapper.UserMapper;
import com.familymemory.service.AuthService;
import com.familymemory.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 认证服务实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final FamilyMapper familyMapper;
    private final FamilyMemberMapper familyMemberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public UserResp register(RegisterReq req) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, req.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 创建用户
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname());
        user.setPhone(req.getPhone());
        user.setAvatar(req.getAvatar() != null ? req.getAvatar() : "👤");
        user.setStatus(1);
        userMapper.insert(user);

        // 构建响应
        UserResp resp = new UserResp();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        return resp;
    }

    @Override
    public LoginResp login(LoginReq req) {
        // 查找用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, req.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        // 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 构建响应
        LoginResp resp = new LoginResp();
        resp.setToken(token);

        UserResp userResp = new UserResp();
        userResp.setId(user.getId());
        userResp.setUsername(user.getUsername());
        userResp.setNickname(user.getNickname());
        userResp.setAvatar(FileUrlHelper.toFullUrl(user.getAvatar()));

        // 获取当前家庭
        LambdaQueryWrapper<FamilyMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(FamilyMember::getUserId, user.getId());
        memberWrapper.last("LIMIT 1");
        FamilyMember member = familyMemberMapper.selectOne(memberWrapper);
        if (member != null) {
            Family family = familyMapper.selectById(member.getFamilyId());
            if (family != null) {
                UserResp.FamilySimpleResp familyResp = new UserResp.FamilySimpleResp();
                familyResp.setId(family.getId());
                familyResp.setName(family.getName());
                userResp.setCurrentFamily(familyResp);
            }
        }

        resp.setUser(userResp);
        return resp;
    }

    @Override
    public UserResp getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        UserResp resp = new UserResp();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setAvatar(FileUrlHelper.toFullUrl(user.getAvatar()));
        resp.setPhone(user.getPhone());

        // 获取当前家庭
        LambdaQueryWrapper<FamilyMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(FamilyMember::getUserId, userId);
        memberWrapper.last("LIMIT 1");
        FamilyMember member = familyMemberMapper.selectOne(memberWrapper);
        if (member != null) {
            Family family = familyMapper.selectById(member.getFamilyId());
            if (family != null) {
                UserResp.FamilySimpleResp familyResp = new UserResp.FamilySimpleResp();
                familyResp.setId(family.getId());
                familyResp.setName(family.getName());
                resp.setCurrentFamily(familyResp);
            }
        }

        return resp;
    }

    @Override
    public UserResp updateProfile(Long userId, UpdateProfileReq req) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        // 更新用户信息
        if (StringUtils.hasText(req.getNickname())) {
            user.setNickname(req.getNickname());
        }
        if (StringUtils.hasText(req.getAvatar())) {
            user.setAvatar(req.getAvatar());
        }
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone());
        }

        userMapper.updateById(user);

        // 同时更新家庭成员中的昵称
        if (StringUtils.hasText(req.getNickname())) {
            LambdaQueryWrapper<FamilyMember> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.eq(FamilyMember::getUserId, userId);
            FamilyMember member = familyMemberMapper.selectOne(memberWrapper);
            if (member != null) {
                member.setNickname(req.getNickname());
                familyMemberMapper.updateById(member);
            }
        }

        // 返回更新后的用户信息
        return getCurrentUser(userId);
    }
}
