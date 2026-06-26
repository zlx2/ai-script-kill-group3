/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 17:43
 * @Component:
 **/
package com.wn.service.impl.script;

import com.wn.entity.R;

import com.wn.entity.script.questions.GameQuestionAnalysisPO;
import com.wn.entity.script.questions.GameQuestionOptionPO;
import com.wn.entity.script.questions.GameQuestionPO;
import com.wn.entity.script.questions.dto.QuestionAddReq;
import com.wn.entity.script.questions.vo.*;

import com.wn.mapper.script.GameQuestionAnalysisRepository;
import com.wn.mapper.script.GameQuestionOptionRepository;
import com.wn.mapper.script.GameQuestionRepository;
import com.wn.service.script.GameQuestionService;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GameQuestionServiceImpl implements GameQuestionService {

    @Resource
    private GameQuestionRepository questionRepo;
    @Resource
    private GameQuestionOptionRepository optionRepo;
    @Resource
    private GameQuestionAnalysisRepository analysisRepo;
    // 注入JPA实体管理器
    @PersistenceContext
    private EntityManager entityManager;
    // ========== 玩家查询：剧本+角色ID 查题目 ==========
    @Override
    @Transactional(readOnly = true)
    public R getQuestionByRole(Long scriptId, Long roleId) {
        List<GameQuestionPO> poList = questionRepo.findByScriptIdAndRoleIdOrderBySortNumAsc(scriptId, roleId);
        List<QuestionVO> voList = new ArrayList<>();

        for (GameQuestionPO po : poList) {
            QuestionVO vo = new QuestionVO();
            // 拷贝同名属性
            BeanUtils.copyProperties(po, vo);
            vo.setQuestionId(po.getId());

            List<GameQuestionOptionPO> optPOList = po.getOptionList();
            List<QuestionOptionVO> optVOList = new ArrayList<>();
            for (GameQuestionOptionPO optPO : optPOList) {
                QuestionOptionVO optVo = new QuestionOptionVO();
                BeanUtils.copyProperties(optPO, optVo);
                optVOList.add(optVo);
            }
            vo.setOptions(optVOList);
            voList.add(vo);
        }
        return R.success(voList);
    }

    // ========== 后台新增题目（req内携带scriptId、roleId） ==========
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R addQuestion(QuestionAddReq req) {
        GameQuestionPO question = new GameQuestionPO();
        BeanUtils.copyProperties(req, question);
        GameQuestionPO saveQ = questionRepo.save(question);
        Long qId = saveQ.getId();

        // 循环转换选项
        List<GameQuestionOptionPO> optList = new ArrayList<>();
        List<QuestionAddReq.QuestionOptionDto> dtoList = req.getOptionList();
        for (QuestionAddReq.QuestionOptionDto dto : dtoList) {
            GameQuestionOptionPO opt = new GameQuestionOptionPO();
            BeanUtils.copyProperties(dto, opt);
            opt.setQuestionId(qId);
            opt.setScriptId(req.getScriptId());
            opt.setRoleId(req.getRoleId());
            optList.add(opt);
        }
        optionRepo.saveAll(optList);

        // 保存解析
        GameQuestionAnalysisPO analysis = new GameQuestionAnalysisPO();
        BeanUtils.copyProperties(req, analysis);
        analysis.setQuestionId(qId);
        analysisRepo.save(analysis);

        return R.success(saveQ);
    }

    // ========== 答题提交（增加script、role权限校验 ==========
    @Override
    public R submitAnswer(AnswerSubmitReq req, Long scriptId, Long roleId) {
        // 先校验该题目属于当前剧本+角色，防止跨剧本答题
        Optional<GameQuestionPO> questionOpt = questionRepo.findById(req.getQuestionId());
        if (questionOpt.isEmpty()) {
            return R.error("题目不存在");
        }
        GameQuestionPO targetQuestion = questionOpt.get();
        if (!targetQuestion.getScriptId().equals(scriptId) || !targetQuestion.getRoleId().equals(roleId)) {
            return R.error("无权限作答该题目");
        }

        List<GameQuestionOptionPO> allOpt = optionRepo.findByQuestionId(req.getQuestionId());
        int totalScore = 0;
        List<String> selectCodes = req.getSelectCodes();

        for (GameQuestionOptionPO opt : allOpt) {
            if (opt.getIsCorrect() == 1 && selectCodes.contains(opt.getOptionCode())) {
                totalScore += opt.getScore();
            }
        }

        String analysisText = "暂无解析";
        List<GameQuestionAnalysisPO> analysisList = analysisRepo.findByQuestionId(req.getQuestionId());
        if (!analysisList.isEmpty()) {
            GameQuestionAnalysisPO analysisPO = analysisList.get(0);
            analysisText = analysisPO.getAnalysis();
        }

        AnswerResultVO resultVo = new AnswerResultVO();
        resultVo.setTotalScore(totalScore);
        resultVo.setAnalysis(analysisText);
        return R.success(resultVo);
    }

    // ========== 后台根据剧本查全部题目 ==========
    @Override
    public R listAllQuestionByScript(Long scriptId) {
        List<GameQuestionPO> allList = questionRepo.findByScriptId(scriptId);
        return R.success(allList);
    }

    // ========== 题目详情 ==========
    @Override
    @Transactional(readOnly = true) // 必须加事务
    public R getQuestionDetail(Long id) {
        Optional<GameQuestionPO> optional = questionRepo.findById(id);
        if (optional.isEmpty()) {
            return R.error("题目不存在");
        }
        GameQuestionPO po = optional.get();
        // 查询选项与解析
        List<GameQuestionOptionPO> optionPOList = optionRepo.findByQuestionId(id);
        List<GameQuestionAnalysisPO> analysisList = analysisRepo.findByQuestionId(id);

        // 组装VO
        QuestionDetailVO vo = new QuestionDetailVO();
        BeanUtils.copyProperties(po, vo);
        // 转换选项
        List<QuestionOptionVO> optVOList = new ArrayList<>();
        for (GameQuestionOptionPO optPO : optionPOList) {
            QuestionOptionVO optVo = new QuestionOptionVO();
            BeanUtils.copyProperties(optPO, optVo);
            optVo.setIsCorrect(optPO.getIsCorrect());
            optVo.setScore(optPO.getScore());
            optVOList.add(optVo);
        }
        vo.setOptions(optVOList);
        // 解析赋值
        if (!analysisList.isEmpty()) {
            vo.setAnalysis(analysisList.get(0).getAnalysis());
        } else {
            vo.setAnalysis("暂无解析");
        }
        return R.success(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R editQuestion(Long id, QuestionAddReq req) {
        Optional<GameQuestionPO> optional = questionRepo.findById(id);
        if (optional.isEmpty()) {
            return R.error("题目不存在");
        }
        GameQuestionPO question = optional.get();
        BeanUtils.copyProperties(req, question);
        GameQuestionPO updateQ = questionRepo.save(question);

        // 先删旧数据
        optionRepo.deleteByQuestionId(id);
        analysisRepo.deleteByQuestionId(id);

        // 新增新选项
        List<GameQuestionOptionPO> optList = new ArrayList<>();
        List<QuestionAddReq.QuestionOptionDto> dtoList = req.getOptionList();
        for (QuestionAddReq.QuestionOptionDto dto : dtoList) {
            GameQuestionOptionPO opt = new GameQuestionOptionPO();
            BeanUtils.copyProperties(dto, opt);
            opt.setQuestionId(id);
            opt.setScriptId(req.getScriptId());
            opt.setRoleId(req.getRoleId());
            optList.add(opt);
        }
        optionRepo.saveAll(optList);

        // 新增解析
        GameQuestionAnalysisPO analysis = new GameQuestionAnalysisPO();
        BeanUtils.copyProperties(req, analysis);
        analysis.setQuestionId(id);
        analysisRepo.save(analysis);

        // 组装VO返回，现在查询用List，不会报唯一结果异常
        QuestionDetailVO vo = new QuestionDetailVO();
        BeanUtils.copyProperties(updateQ, vo);

        List<GameQuestionOptionPO> opts = optionRepo.findByQuestionId(id);
        List<QuestionOptionVO> optVos = new ArrayList<>();
        for (GameQuestionOptionPO opt : opts) {
            QuestionOptionVO ov = new QuestionOptionVO();
            BeanUtils.copyProperties(opt, ov);
            optVos.add(ov);
        }
        vo.setOptions(optVos);

        // 核心修复：用List接收，不再强制唯一
        List<GameQuestionAnalysisPO> analysisList = analysisRepo.findByQuestionId(id);
        if (!analysisList.isEmpty()) {
            vo.setAnalysis(analysisList.get(0).getAnalysis());
        } else {
            vo.setAnalysis("暂无解析");
        }

        return R.success(vo);
    }

    // ========== 删除题目 ==========
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R deleteQuestion(Long id) {
        optionRepo.deleteByQuestionId(id);
        analysisRepo.deleteByQuestionId(id);
        // 强制刷新，同步删除到数据库，清除缓存关联
        entityManager.flush();
        questionRepo.deleteById(id);
        return R.success();
    }
}