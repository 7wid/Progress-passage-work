package cn.edu.techgroup.outsourcing.modules.category.service;

import cn.edu.techgroup.outsourcing.modules.category.dto.CreateAdminCategoryCommand;
import cn.edu.techgroup.outsourcing.modules.category.dto.UpdateAdminCategoryCommand;
import cn.edu.techgroup.outsourcing.modules.category.dto.UpdateAdminCategoryStatusCommand;
import cn.edu.techgroup.outsourcing.modules.category.vo.AdminCategoryVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import java.util.List;

public interface AdminCategoryService {

    List<AdminCategoryVO> list(LoginUser operator);

    AdminCategoryVO create(
            CreateAdminCategoryCommand command,
            LoginUser operator);

    AdminCategoryVO update(
            Long categoryId,
            UpdateAdminCategoryCommand command,
            LoginUser operator);

    AdminCategoryVO updateStatus(
            Long categoryId,
            UpdateAdminCategoryStatusCommand command,
            LoginUser operator);
}
