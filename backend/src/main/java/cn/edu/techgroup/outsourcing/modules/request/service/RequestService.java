package cn.edu.techgroup.outsourcing.modules.request.service;

import cn.edu.techgroup.outsourcing.common.api.PageResponse;
import cn.edu.techgroup.outsourcing.modules.request.dto.CreateRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.dto.RequestListQuery;
import cn.edu.techgroup.outsourcing.modules.request.vo.CreatedRequestVO;
import cn.edu.techgroup.outsourcing.modules.request.vo.RequestDetailVO;
import cn.edu.techgroup.outsourcing.modules.request.vo.RequestSummaryVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;

public interface RequestService {

    CreatedRequestVO createAndSubmit(
            CreateRequestCommand command,
            LoginUser operator);

    PageResponse<RequestSummaryVO> list(
            RequestListQuery query,
            LoginUser viewer);

    RequestDetailVO getDetail(
            Long requestId,
            LoginUser viewer);
}
