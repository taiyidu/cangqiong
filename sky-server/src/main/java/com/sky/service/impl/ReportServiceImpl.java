package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> list = new ArrayList<>();
        list.add(begin);
        while(!begin.equals(end)) {
            //计算指定日期的后一天日期
            begin = begin.plusDays(1);
            list.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate Date : list) {
            LocalDateTime beginTime = LocalDateTime.of(Date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(Date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnoverList.add(turnover == null ? 0.0 : turnover);
        }
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(list,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }
    @Override
    public UserReportVO getUserReportVO(LocalDate begin, LocalDate end) {
        List<LocalDate> list = new ArrayList<>();
        list.add(begin);
        while(!begin.equals(end)) {
            //计算指定日期的后一天日期
            begin = begin.plusDays(1);
            list.add(begin);
        }
        //存放新增用户数量
        List<Integer> newUserList = new ArrayList<>();
        //存放总用户数量
        List<Integer> totalUserList = new ArrayList<>();
        for (LocalDate Date : list) {
            LocalDateTime beginTime = LocalDateTime.of(Date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(Date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("end",endTime);
            totalUserList.add(userMapper.countByMap(map));
            map.put("begin",beginTime);
            newUserList.add(userMapper.countByMap(map));
        }
//        log.info("新增用户数量：{}", newUserList);
//        log.info("总用户数量：{}", totalUserList);
        return UserReportVO.builder()
                .dateList(StringUtils.join(list,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .build();
    }

    @Override
    public OrderReportVO getOrderStatisticsVO(LocalDate begin, LocalDate end) {
        List<LocalDate> list = new ArrayList<>();
        list.add(begin);
        while(!begin.equals(end)) {
            //计算指定日期的后一天日期
            begin = begin.plusDays(1);
            list.add(begin);
        }
        List<Integer> orderCountList = new ArrayList<>();//每日订单数
        List<Integer> validOrderCountList = new ArrayList<>();//每日有效订单数
        for (LocalDate Date : list) {
            LocalDateTime beginTime = LocalDateTime.of(Date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(Date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("end",endTime);
            map.put("begin",beginTime);
            orderCountList.add(orderMapper.getOrderCountByMap(map));
            map.put("status", Orders.COMPLETED);
            validOrderCountList.add(orderMapper.getOrderCountByMap(map));
        }
        Integer sum = 0;
        for (Integer num : orderCountList) {
            sum += num;
        }
        Integer eff = 0;
        for (Integer num : validOrderCountList) {
            eff += num;
        }
        Double orderCompletionRate = sum == 0 ? 0.0 : (double) eff / sum;

        return OrderReportVO.builder()
                .dateList(StringUtils.join(list,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .totalOrderCount(sum)
                .validOrderCount(eff)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 销量排名
     *
     * 这里有一个思想是直接查菜品名然后查菜品名和其销量的键值对
     * 后封装到一个DTO（数据存储）类中将其全部存储到List中List<GoodsSalesDTO>
     * 减少数据库的调用量
     *
     * @return SalesTop10ReportVO
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
        salesTop10.forEach(goodsSalesDTO -> {
            nameList.add(goodsSalesDTO.getName());
            numberList.add(goodsSalesDTO.getNumber());
        });
        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList,","))
                .numberList(StringUtils.join(numberList,","))
                .build();
    }
}
