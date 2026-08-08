package cn.edu.techgroup.outsourcing.modules.progress.mapper;

import cn.edu.techgroup.outsourcing.modules.progress.entity.StatusHistoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StatusHistoryMapper
        extends BaseMapper<StatusHistoryEntity> {
}