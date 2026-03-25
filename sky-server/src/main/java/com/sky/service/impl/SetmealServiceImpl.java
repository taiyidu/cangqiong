package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Employee;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.SetMealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService{
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetMealDishMapper setMealDishMapper;
    /**
     * 添加套餐接口
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.save(setmeal);
        Long setmealId = setmeal.getId();
        for(SetmealDish setmealDish:setmealDTO.getSetmealDishes()){
            setmealDish.setSetmealId(setmealId);
            setMealDishMapper.save(setmealDish);
        }
    }

    @Override
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {
        // 1. 设置分页参数
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());

        // 2. 执行查询（紧跟 startPage 的第一个查询会被分页）
        Page<Setmeal> page = setMealDishMapper.pageQuery(setmealPageQueryDTO);

        // 3. 获取总记录数
        long total = page.getTotal();

        // 4. 获取当前页数据
        List<Setmeal> records = page.getResult();

        // 5. 封装返回结果
        return new PageResult(total, records);
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            setMealDishMapper.delete(id);
            setmealMapper.delete(id);
        }
    }

    @Override
    public void updateStatusById(Long id, Integer status) {
        setmealMapper.updateStatusById(id,status);
    }

    @Override
    public SetmealVO getById(Integer id) {
        Setmeal setmeal = setmealMapper.getById(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal,setmealVO);
        List<SetmealDish> setmealDishList = setMealDishMapper.getSetmealDishBySetmealId(id);
//        for (SetmealDish setmealDish : setmealDishList) {
//
//        }
        setmealVO.setSetmealDishes(setmealDishList);
        return setmealVO;
    }

    @Override
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.update(setmeal);
        setMealDishMapper.delete(setmealDTO.getId());
        for (SetmealDish setmealDish : setmealDTO.getSetmealDishes()) {
            setmealDish.setSetmealId(setmealDTO.getId());//设置套餐id
            setMealDishMapper.save(setmealDish);
        }
    }
}
