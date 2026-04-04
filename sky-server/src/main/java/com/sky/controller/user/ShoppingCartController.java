package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@Api("c端-购物车接口")
@RequestMapping("/user/shoppingCart")
public class ShoppingCartController{

    @Autowired
    private ShoppingCartService shoppingCartService;

    @ApiOperation("添加购物车")
    @PostMapping("/add")
    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("添加购物车商品信息：{}",shoppingCartDTO);
        shoppingCartService.addShoppingCart(shoppingCartDTO);
        return Result.success();
    }
    /**
     * 查看购物车
     */
    @ApiOperation("查看购物车")
    @GetMapping("/list")
    public Result<List<ShoppingCart>> list(){
        log.info("查看{}号用户购物车", BaseContext.getCurrentId());
        List<ShoppingCart> list = shoppingCartService.list();
        return Result.success(list);
    }
    /**
     * 清空购物车
     */
    @ApiOperation("清空购物车")
    @DeleteMapping("/clean")
    public Result clean(){
        log.info("清空{}号用户购物车{}",BaseContext.getCurrentId());
        shoppingCartService.deleteAll();
        return Result.success();
    }
    /**
     * 删除商品
     */
    @ApiOperation("删除商品")
    @PostMapping("/sub")
    public Result sub(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("删除菜品");
        shoppingCartService.delete(shoppingCartDTO);
        return Result.success();
    }
}
