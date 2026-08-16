package cn.edu.techgroup.outsourcing.modules.request.service;

import cn.edu.techgroup.outsourcing.modules.request.dto.CancelRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.dto.SaveDraftCommand;
import cn.edu.techgroup.outsourcing.modules.request.dto.SubmitRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.dto.UpdateRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.vo.CreatedRequestVO;
import cn.edu.techgroup.outsourcing.modules.request.vo.RequestMutationVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;

public interface RequesterRequestLifecycleService {

    CreatedRequestVO createDraft(SaveDraftCommand command, LoginUser operator);

    RequestMutationVO update(
            Long requestId,
            UpdateRequestCommand command,
            LoginUser operator);

    RequestMutationVO submit(
            Long requestId,
            SubmitRequestCommand command,
            LoginUser operator);

    RequestMutationVO cancel(
            Long requestId,
            CancelRequestCommand command,
            LoginUser operator);
}
