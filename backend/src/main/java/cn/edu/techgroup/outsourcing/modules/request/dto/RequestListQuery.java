package cn.edu.techgroup.outsourcing.modules.request.dto;

import cn.edu.techgroup.outsourcing.modules.request.enums.RequestSort;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.assignment.enums.RequestMemberType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record RequestListQuery(

        @Min(value = 1, message = "页码不能小于 1")
        Integer page,

        @Min(value = 1, message = "每页数量不能小于 1")
        @Max(value = 100, message = "每页数量不能超过 100")
        Integer pageSize,

        @Size(max = 80, message = "搜索内容不能超过 80 个字符")
        String keyword,

        RequestStatus status,

        @Positive(message = "分类 ID 必须为正数")
        Long categoryId,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate submittedFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate submittedTo,

        RequestSort sort,

        RequestMemberType assignmentType,

        Boolean activeOnly,

        Boolean overdue) {

    public RequestListQuery {
        page = page == null ? 1 : page;
        pageSize = pageSize == null ? 20 : pageSize;
        sort = sort == null ? RequestSort.NEWEST : sort;
        activeOnly = Boolean.TRUE.equals(activeOnly);
        overdue = Boolean.TRUE.equals(overdue);

        if (keyword != null) {
            keyword = keyword.trim();
            keyword = keyword.isEmpty() ? null : keyword;
        }
    }
}
