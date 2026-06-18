package com.wn.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.wn.utils.enmu.TokenEnum;

import java.util.Date;

/**
 * @Author: 马宇航
 * @Description: JWT工具类，用于生成、验证、获取token中的用户名
 * @DateTime: 26/03/10/星期二 13:47
 * @Component: 成都蜗牛学苑
 **/
public class JWTUtil {
    public static final String SECRET_KEY = "myh123456"; //秘钥
    public static final long TOKEN_EXPIRE_TIME = 30 * 60 * 1000*24; //token过期时间
    private static final String ISSUER = "woniuxy"; //签发人
    /**    生成签名     */
    public static String generateToken(String uname,Long userId){
        Date now = new Date();        //创建签名算法对象
        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY); //算法
        String token = JWT.create()
                .withIssuer(ISSUER) //签发人
                .withIssuedAt(now)  //签发时间
                .withExpiresAt(new Date(now.getTime() + TOKEN_EXPIRE_TIME)) //过期时间，可由redis来控制
                .withClaim("username", uname) //保存身份标识
                .withClaim("userId",userId)
                .sign(algorithm);
        return token;
    }
    /**    验证token     */
    public static TokenEnum verify(String token){
        try {
            //签名算法
            Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY); //算法
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build();
            verifier.verify(token);
            return TokenEnum.TOKEN_SUCCESS;
        } catch (TokenExpiredException ex){
            return TokenEnum.TOKEN_EXPIRE;
            //ex.printStackTrace();
        } catch (Exception e) {
            return TokenEnum.TOKEN_BAD;
        }
    }
    /**   从token获取uname     */
    public static String getUname(String token){
        try{
            return JWT.decode(token).getClaim("uname").asString();
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return "";
    }
    /**   从token获取uname     */
    public static Long getUserId(String token){
        try{
            return JWT.decode(token).getClaim("userId").asLong();
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return null;
    }
}