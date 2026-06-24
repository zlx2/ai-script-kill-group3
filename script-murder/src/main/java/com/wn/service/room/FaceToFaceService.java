/**
 * @Author: 鱼
 * @Description:
 * @DateTime: 2026/6/23 17:04
 * @Component:
 **/
package com.wn.service.room;

import com.wn.service.exception.BusinessException;

public interface FaceToFaceService {

    String createFaceToFace(Long userId);

    String joinFaceToFace(String code, Long userId) throws BusinessException;

    boolean checkCodeExists(String code);

    void cancelFaceToFace(String code, Long userId);

    Integer getPlayerCount(String code);

}
