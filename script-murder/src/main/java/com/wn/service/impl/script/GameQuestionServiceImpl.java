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
import com.wn.entity.script.questions.vo.AnswerResultVO;
import com.wn.entity.script.questions.vo.AnswerSubmitReq;
import com.wn.entity.script.questions.vo.QuestionOptionVO;
import com.wn.entity.script.questions.vo.QuestionVO;

import com.wn.mapper.script.GameQuestionAnalysisRepository;
import com.wn.mapper.script.GameQuestionOptionRepository;
import com.wn.mapper.script.GameQuestionRepository;
import com.wn.service.script.GameQuestionService;
import jakarta.annotation.Resource;
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

    // ========== 玩家查询：剧本+角色ID 查题目 ==========
    @Override
    public R getQuestionByRole(Long scriptId, Long roleId) {
        // 使用你新写的JPA方法 findByScriptIdAndRoleIdOrderBySortNumAsc
        List<GameQuestionPO> poList = questionRepo.findByScriptIdAndRoleIdOrderBySortNumAsc(scriptId, roleId);
        List<QuestionVO> voList = new ArrayList<>();

        for (int i = 0; i < poList.size(); i++) {
            GameQuestionPO po = poList.get(i);
            QuestionVO vo = new QuestionVO();
            vo.setQuestionId(po.getId());
            vo.setQuestionTitle(po.getQuestionTitle());

            List<GameQuestionOptionPO> optPOList = po.getOptionList();
            List<QuestionOptionVO> optVOList = new ArrayList<>();
            for (int j = 0; j < optPOList.size(); j++) {
                GameQuestionOptionPO optPO = optPOList.get(j);
                QuestionOptionVO optVo = new QuestionOptionVO();
                optVo.setOptionCode(optPO.getOptionCode());
                optVo.setOptionContent(optPO.getOptionContent());
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
        // 从Req赋值新增的两个字段
        question.setScriptId(req.getScriptId());
        question.setRoleId(req.getRoleId());
        question.setQuestionTitle(req.getQuestionTitle());
        question.setSortNum(req.getSortNum());
        GameQuestionPO saveQ = questionRepo.save(question);
        Long qId = saveQ.getId();

        // 循环转换选项
        List<GameQuestionOptionPO> optList = new ArrayList<>();
        List<QuestionAddReq.QuestionOptionDto> dtoList = req.getOptionList();
        for (int i = 0; i < dtoList.size(); i++) {
            QuestionAddReq.QuestionOptionDto dto = dtoList.get(i);
            GameQuestionOptionPO opt = new GameQuestionOptionPO();
            opt.setQuestionId(qId);
            // 子表同步冗余剧本、角色ID
            opt.setScriptId(req.getScriptId());
            opt.setRoleId(req.getRoleId());
            opt.setOptionCode(dto.getOptionCode());
            opt.setOptionContent(dto.getOptionContent());
            opt.setIsCorrect(dto.getIsCorrect());
            opt.setScore(dto.getScore());
            optList.add(opt);
        }
        optionRepo.saveAll(optList);

        // 保存解析
        GameQuestionAnalysisPO analysis = new GameQuestionAnalysisPO();
        analysis.setQuestionId(qId);
        analysis.setScriptId(req.getScriptId());
        analysis.setRoleId(req.getRoleId());
        analysis.setAnalysis(req.getAnalysis());
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

        for (int i = 0; i < allOpt.size(); i++) {
            GameQuestionOptionPO opt = allOpt.get(i);
            if (opt.getIsCorrect() == 1) {
                for (int j = 0; j < selectCodes.size(); j++) {
                    String code = selectCodes.get(j);
                    if (opt.getOptionCode().equals(code)) {
                        totalScore = totalScore + opt.getScore();
                        break;
                    }
                }
            }
        }

        String analysisText = "暂无解析";
        Optional<GameQuestionAnalysisPO> analysisOpt = analysisRepo.findByQuestionId(req.getQuestionId());
        if (analysisOpt.isPresent()) {
            GameQuestionAnalysisPO analysisPO = analysisOpt.get();
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

    // ========== 原有详情、编辑、删除仅微调，贴完整 ==========
    @Override
    public R getQuestionDetail(Long id) {
        Optional<GameQuestionPO> optional = questionRepo.findById(id);
        if (optional.isEmpty()) {
            return R.error("题目不存在");
        }
        return R.success(optional.get());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R editQuestion(Long id, QuestionAddReq req) {
        Optional<GameQuestionPO> optional = questionRepo.findById(id);
        if (optional.isEmpty()) {
            return R.error("题目不存在");
        }
        GameQuestionPO question = optional.get();
        // 更新剧本、角色、基础内容
        question.setScriptId(req.getScriptId());
        question.setRoleId(req.getRoleId());
        question.setQuestionTitle(req.getQuestionTitle());
        question.setSortNum(req.getSortNum());
        GameQuestionPO updateQ = questionRepo.save(question);

        // 先删旧数据
        optionRepo.deleteByQuestionId(id);
        analysisRepo.deleteByQuestionId(id);

        // 新增新选项
        List<GameQuestionOptionPO> optList = new ArrayList<>();
        List<QuestionAddReq.QuestionOptionDto> dtoList = req.getOptionList();
        for (int i = 0; i < dtoList.size(); i++) {
            QuestionAddReq.QuestionOptionDto dto = dtoList.get(i);
            GameQuestionOptionPO opt = new GameQuestionOptionPO();
            opt.setQuestionId(id);
            opt.setScriptId(req.getScriptId());
            opt.setRoleId(req.getRoleId());
            opt.setOptionCode(dto.getOptionCode());
            opt.setOptionContent(dto.getOptionContent());
            opt.setIsCorrect(dto.getIsCorrect());
            opt.setScore(dto.getScore());
            optList.add(opt);
        }
        optionRepo.saveAll(optList);

        // 新增解析
        GameQuestionAnalysisPO analysis = new GameQuestionAnalysisPO();
        analysis.setQuestionId(id);
        analysis.setScriptId(req.getScriptId());
        analysis.setRoleId(req.getRoleId());
        analysis.setAnalysis(req.getAnalysis());
        analysisRepo.save(analysis);
        return R.success(updateQ);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R deleteQuestion(Long id) {
        optionRepo.deleteByQuestionId(id);
        analysisRepo.deleteByQuestionId(id);
        questionRepo.deleteById(id);
        return R.success();
    }
}
