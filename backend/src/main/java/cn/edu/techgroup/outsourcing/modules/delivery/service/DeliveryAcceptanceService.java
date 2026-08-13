package cn.edu.techgroup.outsourcing.modules.delivery.service;

import cn.edu.techgroup.outsourcing.modules.delivery.dto.CreateAcceptanceCommand;
import cn.edu.techgroup.outsourcing.modules.delivery.dto.CreateDeliveryCommand;
import cn.edu.techgroup.outsourcing.modules.delivery.vo.CreatedAcceptanceResultVO;
import cn.edu.techgroup.outsourcing.modules.delivery.vo.CreatedDeliveryResultVO;
import cn.edu.techgroup.outsourcing.modules.delivery.vo.DeliveryAcceptanceSnapshotVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;

public interface DeliveryAcceptanceService {

    DeliveryAcceptanceSnapshotVO get(Long requestId, LoginUser viewer);

    CreatedDeliveryResultVO createDelivery(
            Long requestId,
            CreateDeliveryCommand command,
            LoginUser operator);

    CreatedAcceptanceResultVO createAcceptance(
            Long requestId,
            CreateAcceptanceCommand command,
            LoginUser operator);
}
