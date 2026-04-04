package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper{
    /**
     * 动态条件查询
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 根据id修改商品数量
     * @param cart
     */
    @Update("update shopping_cart set number = #{number} where id= #{id}")
    void updateNumberById(ShoppingCart cart);

    /**
     * 插入购物车数据
     * @param shoppingCart
     */
    @Insert("insert into shopping_cart (user_id, dish_id, setmeal_id, name, image, amount, number, create_time,dish_flavor) " +
            "values (#{userId}, #{dishId}, #{setmealId}, #{name}, #{image}, #{amount}, #{number}, #{createTime},#{dishFlavor})")
    void insert(ShoppingCart shoppingCart);

    @Delete("delete from shopping_cart where user_id = #{userId}")
    void deleteAll(Long userId);

    void delete(ShoppingCart shoppingCart);

    void updateNumberByshoppingCart(ShoppingCart shoppingCart);
}
