package com.wn.mapper.auth;


import com.wn.entity.user.Userinfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * 用户信息mapper层
 */
@Repository
public interface UserinfoMapper extends JpaRepository<Userinfo,Long> {

    Userinfo findOneByEmail(String email);

    Userinfo findByUsername(String username);

    Object findUserByUserId(Long userId);
}
