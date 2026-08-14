package cn.edu.techgroup.outsourcing.modules.audit.mapper;

import cn.edu.techgroup.outsourcing.modules.audit.entity.AuditLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogEntity> {}
