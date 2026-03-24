package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Slf4j
@Api(tags = "套餐管理相关接口")
public class SetmealController{
    @Autowired
    private SetmealService setmealService;
    /**
     * 新增套餐接口
     */
    @ApiOperation("新增套餐接口")
    @PostMapping
    public Result save(@RequestBody SetmealDTO setmealDTO){
        log.info("新增菜品数据：{}",setmealDTO);
        setmealService.save(setmealDTO);
        return Result.success();
    }

    /**
     * 根据id查询套餐
     */
    @ApiOperation("根据id查询套餐")
    @GetMapping("/{id}")
    public Result<SetmealVO> getById(@PathVariable Integer id){
        log.info("根据id查询套餐 id:{}",id);

        return Result.success();
    }

    /**
     * 分页查询套餐
     */
    @ApiOperation("分页查询")
    @GetMapping("/page")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("分页查询接口信息：{}",setmealPageQueryDTO);
        PageResult pageResult = setmealService.page(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 按照id删除套餐
     */
    @ApiOperation("按照id删除套餐")
    @DeleteMapping
    public Result deleteById(@RequestParam List<Long> ids){
        log.info("删除套餐接口id:{}",ids);
        setmealService.delete(ids);
        return Result.success();
    }

    /**
     * 调整起售停售
     */
    @PostMapping("/status/{status}")
    public Result updatestatus(@PathVariable Integer status,@RequestParam Long id){
        log.info("修改套餐状态 id:{} status:{}",id,status);
        setmealService.updateStatusById(id,status);
        return Result.success();
    }
}
