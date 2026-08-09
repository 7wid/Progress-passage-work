package cn.edu.techgroup.outsourcing.modules.category.service.impl;

import cn.edu.techgroup.outsourcing.modules.category.entity.CategoryEntity;
import cn.edu.techgroup.outsourcing.modules.category.mapper.CategoryMapper;
import cn.edu.techgroup.outsourcing.modules.category.service.CategoryService;
import cn.edu.techgroup.outsourcing.modules.category.vo.CategoryOptionVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<CategoryOptionVO> listEnabled() {
        return categoryMapper.selectList(
                        Wrappers.<CategoryEntity>lambdaQuery()
                                .eq(CategoryEntity::getEnabled, true)
                                .orderByAsc(CategoryEntity::getSortOrder)
                                .orderByAsc(CategoryEntity::getId))
                .stream()
                .map(CategoryOptionVO::from)
                .toList();
    }
}