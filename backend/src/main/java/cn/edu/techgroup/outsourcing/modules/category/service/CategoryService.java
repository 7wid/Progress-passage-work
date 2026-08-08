package cn.edu.techgroup.outsourcing.modules.category.service;

import cn.edu.techgroup.outsourcing.modules.category.vo.CategoryOptionVO;
import java.util.List;

public interface CategoryService {

    List<CategoryOptionVO> listEnabled();
}