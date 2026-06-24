package com.wn.controller.room;

import com.wn.entity.R;
import com.wn.service.exception.BusinessException;
import com.wn.service.room.FaceToFaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: 鱼
 * @Description:面对面建群控制器
 * @DateTime: 2026/6/23
 * @Component: room模块控制器
 **/
@RestController
@RequestMapping("/face2face")
@RequiredArgsConstructor
public class FaceToFaceController {

    private final FaceToFaceService faceToFaceService;

    /**
     * 创建面对面房间
     * POST /face2face/create
     * 第一个人点"面对面建房"，生成一个4位号码
     * @param userId 创建者用户ID
     * @return 4位面对面号码
     */
    @PostMapping("/create")
    public R create(@RequestHeader("userId") Long userId) {
        String code = faceToFaceService.createFaceToFace(userId);
        return new R(code);
    }

    /**
     * 加入面对面房间
     * 其他人输入4位号码加入
     * @param code 4位面对面号码
     * @param id 加入者用户ID
     * @return 正式房间roomId（UUID字符串，原Long类型同步修正为项目统一String主键）
     */
    @PostMapping("/join")
    public R join(
            @RequestParam String code,
            @RequestHeader("userId") Long id) throws BusinessException {

        String roomId = faceToFaceService.joinFaceToFace(code, id);
        return new R(roomId);
    }

    /**
     * 检查面对面号码是否存在
     * 前端输入号码时可以实时校验，给用户反馈
     * @param code 4位面对面号码
     * @return 是否存在（true=存在，false=不存在或已过期）
     */
    @GetMapping("/check")
    public R checkCode(@RequestParam String code) throws BusinessException {
        boolean exists = faceToFaceService.checkCodeExists(code);
        return new R(exists);
    }

    /**
     * 取消面对面房间
     * 创建者可以取消，释放号码
     * @param code 4位面对面号码
     * @param userId 创建者用户ID
     */
    @PostMapping("/cancel")
    public R cancel(
            @RequestParam String code,
            @RequestHeader("userId") Long userId) throws BusinessException {

        faceToFaceService.cancelFaceToFace(code, userId);
        return R.SUCCESS;
    }

    /**
     * 获取面对面房间的当前人数
     * 显示已经有多少人加入了
     * @param code 4位面对面号码
     * @return 当前在线人数
     */
    @GetMapping("/{code}/count")
    public R getPlayerCount(@PathVariable String code) throws BusinessException {
        Integer count = faceToFaceService.getPlayerCount(code);
        return new R(count);
    }
}