package cn.edu.techgroup.outsourcing.modules.category.controller;

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.modules.category.service.CategoryService;
import cn.edu.techgroup.outsourcing.modules.category.vo.CategoryOptionVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<CategoryOptionVO>> listEnabled() {
        return ApiResponse.success(categoryService.listEnabled());
    }
}