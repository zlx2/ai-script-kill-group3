package com.wn.service.impl.auth;

import com.wn.controller.auth.dto.UserinfoDTO;
import com.wn.controller.auth.vo.UserinfoVO;
import com.wn.entity.user.Userinfo;
import com.wn.mapper.auth.UserinfoMapper;
import com.wn.service.auth.UserService;
import com.wn.utils.JWTUtil;
import com.wn.utils.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务层实现类，包含：
 *  1.发送验证码
 *  2.用户注册
 *  3.用户登录
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    //随机验证码的字符池
    private static final String EMAIL_CODE_KEY = "ABCDEFGHJKLMNPQRSTUVWXYabcdefghjkmnopqrstuvwxyz123456789";
    private final UserinfoMapper userinfoMapper;
    //Redis 字段注入
    private final RedisTemplate redisTemplate;
    //Java发送邮箱
    private final JavaMailSender javaMailSender;
    //自定义邮箱变量
    @Value("${spring.mail.username}")
    private String sendEmailAddress;

    /**
     * 1.发送邮件主方法
     *  注册邮箱条件：
     *      a.邮箱未使用
     *      b.注册了但是已逻辑删除，允许建新号
     */
    @Override
    public void sendEmail(Map<String, String> params){
        String email = params.get("username");
        if (email == null || email.isEmpty()) {
            throw new  GlobalException(401,"邮箱不能为空");
        }
        Userinfo userinfo =userinfoMapper.findOneByEmail(email);
        //1.先判断邮箱是否存在 如果有邮箱，说明这个邮箱已经注册过账号了，同时还需要判断是否逻辑删除
        if (userinfo != null && userinfo.getDeleted() == 0) {
            throw new GlobalException(401, "邮箱已注册且有效");
        }
        // 其他情况（不存在或已删除）都允许发送验证码
        sendCode(email);
    }
    //发送验证码
    private void sendCode(String email){
        String code = this.generateCode();
        //发送验证码并且设置5分钟有效
        redisTemplate.opsForValue().set(email, code, 5, TimeUnit.MINUTES);
        this.sendEmailCode(email, code);
    }
    //生成随机四位验证码
    private String generateCode() {
        //安全随机字符
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        //生成四位数验证码，验证码字符来源于验证码池
        for (int i = 0; i < 4; i++) {
            sb.append(EMAIL_CODE_KEY.charAt(secureRandom.nextInt(EMAIL_CODE_KEY.length())));
        }
        return sb.toString();
    }
    //发送验证码
    private void sendEmailCode(String email, String code) {
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom(sendEmailAddress);
        simpleMailMessage.setTo(email);
        simpleMailMessage.setSubject("注册验证码");
        simpleMailMessage.setText("欢迎注册！您的验证码为：" + code + ",验证码5分钟内有效");
        javaMailSender.send(simpleMailMessage);
    }


    /**
     *  2.注册主方法
     */
    @Override
    public String register(UserinfoDTO userInfoDto) {
        if (userInfoDto.getEmail()==null){
            return "未输入验证码";
        }
        // 从redis中获取验证码，key是email
        Object redisCode = redisTemplate.opsForValue().get(userInfoDto.getEmail());
        if (redisCode == null){
            return "验证码已跟过期，请重新获取";
        }
        Userinfo existUser = userinfoMapper.findByUsername(userInfoDto.getUsername());
        if (existUser != null){
            return "用户名已存在，请重试";
        }
        String registerCode = redisCode.toString();
        if (userInfoDto.getCode().equals(registerCode)){
            Userinfo userinfo = dto2PO(userInfoDto);
            userinfoMapper.save(userinfo);
            return "注册成功";
        }
        return "验证码错误";
    }
    //DTO转UserinfoPO
    private Userinfo dto2PO(UserinfoDTO userInfoDto) {
        Userinfo userinfo = new Userinfo();
        //注入的copy
        BeanUtils.copyProperties(userInfoDto,userinfo);
        //密码加密加盐
        userinfo.setPassword(encryption(userInfoDto));
        return userinfo;
    }
    //密码加密加盐，使用密码+账户的hash的方式加密
    private String encryption(UserinfoDTO userInfoDTO) {
        String resource_password = userInfoDTO.getPassword();
        //密码加盐：密码+账户的hash
        String account = userInfoDTO.getUsername();
        int salt = (account != null) ? account.hashCode() : 0;
        return DigestUtils.md5DigestAsHex((resource_password + salt).getBytes());
    }


    /**
     * 3.登录主方法
     */
    @Override
    public UserinfoVO login(UserinfoDTO userinfoDTO){
        //1.参数校验
        if (userinfoDTO == null || userinfoDTO.getUsername() == null || userinfoDTO.getUsername().isEmpty()) {
            throw new GlobalException(400, "账号不能为空");
        }
        if (userinfoDTO.getPassword() == null || userinfoDTO.getPassword().isEmpty()) {
            throw new GlobalException(400, "密码不能为空");
        }
        //2.查询是否有该用户
        Userinfo userInfoPO = userinfoMapper.findByUsername(userinfoDTO.getUsername());
        if (userInfoPO == null) {
            throw new GlobalException(401, "账号或密码错误");
        }
        //2.1 判断用户是否注销
        if (userInfoPO.getDeleted().equals(1)){
            throw new GlobalException(401, "账号已注销");
        }
        //3.比较密码 encryption:密码加密方法
        String userInputPwd = encryption(userinfoDTO);
        if (!userInputPwd.equals(userInfoPO.getPassword())) {
            throw new GlobalException(401, "账号或密码错误");
        }
        //4.登录成功，返回前端 用户信息（VO）generateRefreshToken:创建返回的Token
        String refreshToken = generateRefreshToken(userInfoPO);
        //5.把token和用户信息转成VO层 toUserVO:PO转VO
        return toUserVO(userInfoPO, refreshToken);
    }

    @Override
    public Userinfo getById(Long hostId) {
        return userinfoMapper.findById(hostId).orElse(null);
    }

    @Override
    public List<Userinfo> listByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        return userinfoMapper.findAllById(userIds);
    }

    //创建返回的token
    private String generateRefreshToken(Userinfo userInfoPO) {
        //生成一个jwt，存入用户名和id
        String jwt = JWTUtil.generateToken(userInfoPO.getUsername(), userInfoPO.getUserId());
        //生成uuid作为redis存储的key
        String refreshToken = UUID.randomUUID().toString();
        //用String类型把返回的refreshToken作为key存到redis jwt当value
        Map<String, Object> map = new HashMap<>();
        map.put("token", jwt);
        map.put("userId", userInfoPO.getUserId());   // 只存 id
        // 如果后续需要用户名，也可以存 username
        // map.put("username", userInfoPO.getUsername());
        redisTemplate.opsForHash().putAll(refreshToken, map);
        redisTemplate.expire(refreshToken,60,TimeUnit.MINUTES);
        return refreshToken;
    }
    //封装vo信息返回给前端
    private UserinfoVO toUserVO(Userinfo userInfoPO, String refreshToken) {
        UserinfoVO userinfoVO = new UserinfoVO();
        userinfoVO.setUserId(userInfoPO.getUserId());
        userinfoVO.setUsername(userInfoPO.getUsername());
        userinfoVO.setNickname(userInfoPO.getNickname());
        userinfoVO.setAvatar(userInfoPO.getAvatar());
        userinfoVO.setRefreshToken(refreshToken);
        return userinfoVO;
    }
}
