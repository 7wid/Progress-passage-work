package cn.edu.techgroup.outsourcing.modules.progress.service;

import cn.edu.techgroup.outsourcing.modules.progress.dto.CreateProgressCommand;
import cn.edu.techgroup.outsourcing.modules.progress.vo.CreatedProgressResultVO;
import cn.edu.techgroup.outsourcing.modules.progress.vo.RequestProgressSnapshotVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;

public interface ProgressService {

    RequestProgressSnapshotVO get(
            Long requestId,
            LoginUser viewer);

    CreatedProgressResultVO create(
            Long requestId,
            CreateProgressCommand command,
            LoginUser operator);
}
