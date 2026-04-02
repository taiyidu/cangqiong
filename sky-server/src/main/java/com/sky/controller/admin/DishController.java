package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 菜品管理
 */
@Api(tags = "菜品管理相关接口")
@RestController
@Slf4j
@RequestMapping("/admin/dish")
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品：{}",dishDTO);
        dishService.saveWithFlavor(dishDTO);

        cleanCache("dish_" +dishDTO.getCategoryId());

        return Result.success();
    }
    /**
     * 菜品分页查询
     */
    @ApiOperation("菜品分页查询")
    @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品分页查询：{}",dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }
    /**
     * 删除菜品
     */
    @ApiOperation("菜品删除接口")
    @DeleteMapping
    public Result delete(@RequestParam List<Long> ids){
        log.info("批量删除接口：{}",ids);
        dishService.deleteBatch(ids);

        cleanCache("dish_*");

        return Result.success();
    }
    /**
     * 菜品查询回显
     */
    @ApiOperation("菜品查询回显")
    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询数据 id:{}",id);
        DishVO dishVo = dishService.getById(id);
        return Result.success(dishVo);
    }
    /**
     * 修改菜品
     */
    @ApiOperation("修改菜品")
    @PutMapping
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品参数：{}",dishDTO);
        dishService.update(dishDTO);

        cleanCache("dish_*");

        return Result.success();
    }
    /**
     * 根据分类id查询菜品
     */
    @GetMapping("/list")
    public Result<List<DishVO>> getCategoryByDishId(Long categoryId){
        log.info("根据分类id查询菜品 categoryId：{}",categoryId);
        List<DishVO> dishVo = dishService.getBySetmealId(categoryId);
        return Result.success(dishVo);
    }
    @ApiOperation("调整起售停售")
    @PostMapping("/status/{status}")
    public Result updatestatus(@PathVariable Integer status,@RequestParam Long id){
        log.info("修改菜品状态 id:{} status:{}",id,status);
        dishService.updateStatusById(id,status);

        cleanCache("dish_*");

        return Result.success();
    }

    /**
     * 清理缓存数据
     * @param pattern key值
     */
    private void cleanCache(String pattern) {
        //将我们的所有菜品缓存数据全部清理掉，以dish_开头的key
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}
