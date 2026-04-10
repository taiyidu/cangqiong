package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.websocket.servlet.TomcatWebSocketServletWebServerCustomizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sky.entity.Orders.CONFIRMED;
import static com.sky.entity.Orders.DELIVERY_IN_PROGRESS;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private TomcatWebSocketServletWebServerCustomizer websocketServletWebServerCustomizer;

    private Orders orders;


    @Autowired
    private WebSocketServer webSocketServer;


    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //处理业务异常(地址簿为空，购物车数据为空)
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null){
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //查询当前购物车数据
        Long userId = BaseContext.getCurrentId();//通过jwp令牌获取用户id
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(ShoppingCart.builder().userId(userId).build());
        if(shoppingCartList == null || shoppingCartList.isEmpty()){
            //抛出
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //向订单添加一条数据(并将id反回来)
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);

        // 计算订单总金额
        BigDecimal totalAmount = new BigDecimal("0");
        for (ShoppingCart cart : shoppingCartList) {
            totalAmount = totalAmount.add(cart.getAmount());
        }
        orders.setAmount(totalAmount);
        this.orders = orders;

        orderMapper.insert(orders);
        //向订单明细表添加n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();//订单明细
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(orders.getId());//设置当前订单明细关联的订单id
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);
        //清空购物车数据
        shoppingCartMapper.deleteAll(userId);       //封装vo返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder().
                id(orders.getId()).
                orderNumber(orders.getNumber()).
                orderAmount(orders.getAmount()).
                orderTime(orders.getOrderTime()).
                build();
        return orderSubmitVO;
    }
    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

////        调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }

//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code","ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        Integer OrderPaidStatus = Orders.PAID;//支付状态，已支付
        Integer OrderStatus = Orders.TO_BE_CONFIRMED;  //订单状态，待接单
        LocalDateTime check_out_time = LocalDateTime.now();//更新支付时间
        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, this.orders.getId());

        //通过websocket向浏览器推送消息 type orderId content
        Map map = new HashMap();
        map.put("type",1);// 1表示来单提醒 2表示客户催单
        map.put("orderId",this.orders.getId() );
        map.put("content","订单号:"+ordersPaymentDTO.getOrderNumber());
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
        log.info("1表示来单提醒JSON:{}",json);

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);


    }

    @Override
    public void reminder(Long id) {
        //根据Id查询订单
        Orders ordersDB = orderMapper.getById(id);
        //校验订单是否存在
        if(ordersDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //通过websocket向浏览器推送消息 type orderId content
        Map map = new HashMap();
        map.put("type",2);// 1表示来单提醒 2表示客户催单
        map.put("orderId",this.orders.getId());
        map.put("content","订单号:"+orders.getNumber());
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
        log.info("2表示客户催单JSON:{}",json);
    }
    /**
     * 历史订单查询
     * @param dishPageQueryDTO
     * @return
     */
    @Transactional
    @Override
    public PageResult historyOrders(DishPageQueryDTO dishPageQueryDTO) {
        // 1. 设置分页参数
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        // 2. 执行查询（紧跟 startPage 的第一个查询会被分页）
        Page<Orders> page = orderMapper.historyOrders(dishPageQueryDTO);

        // 3. 获取总记录数
        long total = page.getTotal();

        // 4. 获取当前页数据
        List<Orders> records = page.getResult();

        log.info("分页查询结果:{}", records);

        List<OrderVO> orderVOList = new ArrayList<>();
        for (Orders record : records) {
//            OrderVO orderVO = new OrderVO();
//            orderVO.setOrderDishes(JSON.toJSONString(orderVO));
//            Long id = record.getId();
//            List<OrderDetail> list = orderDetailMapper.getByOrderId(id);
//            orderVO.setOrderDetailList(list);

            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(record, orderVO);

            Long id = record.getId();
            List<OrderDetail> list = orderDetailMapper.getByOrderId(id);
            orderVO.setOrderDetailList(list);
            orderVO.setOrderDishes(JSON.toJSONString(list));

            orderVOList.add(orderVO);
        }
        log.info("分页查询结果:{}", orderVOList);
        // 5. 封装返回结果
        return new PageResult(total, orderVOList);
    }

    @Override
    @Transactional
    public OrderVO orderDetail(Long id) {
        Orders order = orderMapper.getById(id);
        OrderVO orderVO = new OrderVO();
        AddressBook addressBook = addressBookMapper.getAddressById(order.getUserId());
        log.info("addressBook订单地址信息:{}", addressBook);
        if (addressBook != null) {
            String address = addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail();
            order.setAddress(address);
        }
        log.info("订单地址信息:{}", order.getAddress());
        BeanUtils.copyProperties(order, orderVO);
        List<OrderDetail> list = orderDetailMapper.getByOrderId(id);
        orderVO.setOrderDetailList(list);
        return orderVO;
    }

    @Override
    public void cancel(Long id) {
        Orders order = Orders.builder()
                .id(id)
                .status(Orders.CANCELLED)
                .cancelReason("用户取消")
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(order);
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
//        orderMapper
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.conditionSearch(ordersPageQueryDTO);
//        long total = page.getTotal();
//        List<OrderVO> orderVOList = new ArrayList<>();
//        for (Orders record : page) {
//            OrderVO orderVO = new OrderVO();
//            BeanUtils.copyProperties(record, orderVO);
//            orderVO.setOrderDishes(JSON.toJSONString(orderVO));
//            Long id = record.getId();
//            List<OrderDetail> list = orderDetailMapper.getByOrderId(id);
//            orderVO.setOrderDetailList(list);
//            orderVOList.add(orderVO);
//        }


        // 3. 获取总记录数
        long total = page.getTotal();

        // 4. 获取当前页数据
        List<Orders> records = page.getResult();

        for (Orders record : records) {
            Long addressBookId = record.getAddressBookId();
            AddressBook addressBook = addressBookMapper.getAddressById(record.getUserId());
            log.info("addressBook订单地址信息:{}", addressBook);
            if (addressBook != null) {
                String address = addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail();
                record.setAddress(address);
            }
        }

        // 5. 封装返回结果
        return new PageResult(total, records);
    }

    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        Integer TO_BE_CONFIRMED = orderMapper.getByStatus(Orders.TO_BE_CONFIRMED);//待接单
        Integer CONFIRMED = orderMapper.getByStatus(Orders.CONFIRMED);//待派送
        Integer DELIVERY_IN_PROGRESS = orderMapper.getByStatus(Orders.DELIVERY_IN_PROGRESS);//派送中
        orderStatisticsVO.setToBeConfirmed(TO_BE_CONFIRMED);
        orderStatisticsVO.setConfirmed(CONFIRMED);
        orderStatisticsVO.setDeliveryInProgress(DELIVERY_IN_PROGRESS);
        return orderStatisticsVO;
    }
    /**
     * 接单
     * @param ordersConfirmD
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmD) {
        ordersConfirmD.setStatus(Orders.CONFIRMED);
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersConfirmD, orders);
        orderMapper.update(orders);
    }

    @Override
    public void cancelByAdmin(OrdersCancelDTO ordersCancelDTO) {
        Orders order = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(order);
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersRejectionDTO, orders);
        orders.setStatus(Orders.CANCELLED);// 拒单
        orders.setPayStatus(Orders.REFUND);// 退款
        log.info("拒单原因:{}", orders);
        orderMapper.update(orders);
    }

    @Override
    public void delivery(Long id) {
        Orders order = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        orderMapper.update(order);
    }

    @Override
    public void completeOrder(Long id) {
        Orders order = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .build();
        orderMapper.update(order);
    }
}
