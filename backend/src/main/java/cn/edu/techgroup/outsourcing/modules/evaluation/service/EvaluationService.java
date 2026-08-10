package cn.edu.techgroup.outsourcing.modules.evaluation.service;

import cn.edu.techgroup.outsourcing.modules.evaluation.dto.ConfirmRejectionCommand;
import cn.edu.techgroup.outsourcing.modules.evaluation.dto.CreateEvaluationCommand;
import cn.edu.techgroup.outsourcing.modules.evaluation.vo.EvaluationResultVO;
import cn.edu.techgroup.outsourcing.modules.evaluation.vo.EvaluationVO;
import cn.edu.techgroup.outsourcing.modules.evaluation.vo.RejectionConfirmationVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import java.util.List;

public interface EvaluationService {

    List<EvaluationVO> list(Long requestId, LoginUser viewer);

    EvaluationResultVO create(
            Long requestId,
            CreateEvaluationCommand command,
            LoginUser operator);

    RejectionConfirmationVO confirmRejection(
            Long requestId,
            Long evaluationId,
            ConfirmRejectionCommand command,
            LoginUser operator);
}