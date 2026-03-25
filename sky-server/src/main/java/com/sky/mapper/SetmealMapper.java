package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.constant.StatusConstant;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    @AutoFill(value=OperationType.INSERT)
    void save(Setmeal setmeal);

    void delete(Long id);

    @Update("update setmeal set status = #{status} where id = #{id}")
    void updateStatusById(Long id, Integer status);

    @Select("select * from setmeal where id = #{id}")
    Setmeal getById(Integer id);

    @AutoFill(value = OperationType.UPDATE)
    void update(Setmeal setmeal);
}
