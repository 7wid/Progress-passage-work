package cn.edu.techgroup.outsourcing.modules.category.mapper;

import cn.edu.techgroup.outsourcing.modules.category.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CategoryMapper extends BaseMapper<CategoryEntity> {
    @Select("SELECT * FROM category WHERE id = #{id} FOR UPDATE")
    CategoryEntity selectByIdForUpdate(@Param("id") Long id);
}
