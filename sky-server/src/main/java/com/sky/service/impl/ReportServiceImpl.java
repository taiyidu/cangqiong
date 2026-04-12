package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
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
    @Autowired
    private WorkspaceService workspaceService;

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

    /**
     * 导出excel报表
     */
    @Override
    public void getExcel(HttpServletResponse response) {
        //1.查数据库
        LocalDate DateBegin = LocalDate.now().minusDays(30);
        LocalDate Dateend = LocalDate.now().minusDays(1);
        LocalDateTime Begin = LocalDateTime.of(DateBegin, LocalTime.MAX);
        LocalDateTime end = LocalDateTime.of(Dateend, LocalTime.MAX);
        //查询概览数据
        BusinessDataVO businessDataVo = workspaceService.getBusinessData(Begin, end);
        //2.封装数据
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            //基于模板文件构建一个新的excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);
            //填充数据
            XSSFSheet sheet = excel.getSheet("Sheet1");
            sheet.getRow(1).getCell(1).setCellValue("时间："+Begin+"至"+end);
            sheet.getRow(3).getCell(2).setCellValue(businessDataVo.getTurnover());
            sheet.getRow(3).getCell(4).setCellValue(businessDataVo.getOrderCompletionRate());
            sheet.getRow(3).getCell(6).setCellValue(businessDataVo.getNewUsers());
            sheet.getRow(4).getCell(2).setCellValue(businessDataVo.getValidOrderCount());
            sheet.getRow(4).getCell(4).setCellValue(businessDataVo.getUnitPrice());
            //填充明细数据
            for(int i=0;i<30;i++){
                LocalDate date = DateBegin.plusDays(i);
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                sheet.getRow(7+i).getCell(1).setCellValue(date.toString());
                sheet.getRow(7+i).getCell(2).setCellValue(businessData.getTurnover());
                sheet.getRow(7+i).getCell(3).setCellValue(businessData.getValidOrderCount());
                sheet.getRow(7+i).getCell(4).setCellValue(businessData.getOrderCompletionRate());
                sheet.getRow(7+i).getCell(5).setCellValue(businessData.getUnitPrice());
                sheet.getRow(7+i).getCell(6).setCellValue(businessData.getNewUsers());
            }
            //3.推送
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            out.close();
            excel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
