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

    // ========== 玩家原有三个方法 ==========
    @Override
    public R getQuestionByRole(String roleType) {
        List<GameQuestionPO> poList = questionRepo.findByRoleTypeOrderBySortNumAsc(roleType);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R addQuestion(QuestionAddReq req) {
        GameQuestionPO question = new GameQuestionPO();
        question.setRoleType(req.getRoleType());
        question.setQuestionTitle(req.getQuestionTitle());
        question.setSortNum(req.getSortNum());
        GameQuestionPO saveQ = questionRepo.save(question);
        Long qId = saveQ.getId();

        List<GameQuestionOptionPO> optList = new ArrayList<>();
        List<QuestionAddReq.QuestionOptionDto> dtoList = req.getOptionList();
        for (int i = 0; i < dtoList.size(); i++) {
            QuestionAddReq.QuestionOptionDto dto = dtoList.get(i);
            GameQuestionOptionPO opt = new GameQuestionOptionPO();
            opt.setQuestionId(qId);
            opt.setOptionCode(dto.getOptionCode());
            opt.setOptionContent(dto.getOptionContent());
            opt.setIsCorrect(dto.getIsCorrect());
            opt.setScore(dto.getScore());
            optList.add(opt);
        }
        optionRepo.saveAll(optList);

        GameQuestionAnalysisPO analysis = new GameQuestionAnalysisPO();
        analysis.setQuestionId(qId);
        analysis.setAnalysis(req.getAnalysis());
        analysisRepo.save(analysis);
        return R.success(saveQ);
    }

    @Override
    public R submitAnswer(AnswerSubmitReq req) {
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

    // ========== 新增后台扩展方法 ==========
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
        question.setRoleType(req.getRoleType());
        question.setQuestionTitle(req.getQuestionTitle());
        question.setSortNum(req.getSortNum());
        GameQuestionPO updateQ = questionRepo.save(question);

        // 先删除旧选项、旧解析
        optionRepo.deleteByQuestionId(id);
        analysisRepo.deleteByQuestionId(id);

        // 新增新选项
        List<GameQuestionOptionPO> optList = new ArrayList<>();
        List<QuestionAddReq.QuestionOptionDto> dtoList = req.getOptionList();
        for (int i = 0; i < dtoList.size(); i++) {
            QuestionAddReq.QuestionOptionDto dto = dtoList.get(i);
            GameQuestionOptionPO opt = new GameQuestionOptionPO();
            opt.setQuestionId(id);
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

    @Override
    public R listAllQuestion() {
        List<GameQuestionPO> allList = questionRepo.findAll();
        return R.success(allList);
    }
}
