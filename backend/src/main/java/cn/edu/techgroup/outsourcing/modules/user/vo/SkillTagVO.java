package cn.edu.techgroup.outsourcing.modules.user.vo;

import cn.edu.techgroup.outsourcing.modules.user.entity.SkillTagEntity;

public record SkillTagVO(String id, String name) {

    public static SkillTagVO from(SkillTagEntity skillTag) {
        return new SkillTagVO(
                skillTag.getId().toString(),
                skillTag.getName());
    }
}
