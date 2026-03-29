package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController("adminShopController")
@RequestMapping("admin/shop")
@Api(tags = "店铺相关接口")
public class ShopController {
    @Autowired
    private RedisTemplate redisTemplate;

    public static final String KEY = "SHOP_STATUS";
    /**
     * 设置营业状态
     */
    @PutMapping("/{status}")
    @ApiOperation("管理设置营业状态")
    public Result setStatus(@PathVariable Integer status){
        log.info("设置营业状态：{}",status == 1?"营业中":"打洋中");
        redisTemplate.opsForValue().set(KEY,status);
        return Result.success();
    }

    /**
     * 查询店铺状态
     */
    @ApiOperation("查询店铺状态")
    @GetMapping("/status")
    public Result<Integer> getStatus(){
//        log.info("查询营业状态");
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        if(status!=null){
            log.info("营业状态：{}",status == 1?"营业中":"打洋中");
        }
        return Result.success(status);
    }
}
