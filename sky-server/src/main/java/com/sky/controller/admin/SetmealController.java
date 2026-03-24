package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
}
