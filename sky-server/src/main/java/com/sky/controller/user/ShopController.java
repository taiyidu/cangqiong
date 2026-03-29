package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController("userShopController")
@RequestMapping("user/shop")
@Api(tags = "用户店铺相关接口")
public class ShopController {
    @Autowired
    private RedisTemplate redisTemplate;

    public static final String KEY = "SHOP_STATUS";

    /**
     * 查询店铺状态
     */
    @ApiOperation("查询店铺状态")
    @GetMapping("/status")
    public Result<Integer> getStatus(){
        log.info("查询营业状态");
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        if(status!=null){
            log.info("营业状态：{}",status == 1?"营业中":"打洋中");
        }
        return Result.success(status);
    }
}
