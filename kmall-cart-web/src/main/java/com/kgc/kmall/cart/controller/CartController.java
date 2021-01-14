package com.kgc.kmall.cart.controller;

import com.alibaba.fastjson.JSON;
import com.kgc.kmall.annotations.LoginRequired;
import com.kgc.kmall.bean.OmsCartItem;
import com.kgc.kmall.bean.PmsSkuInfo;
import com.kgc.kmall.service.CartService;
import com.kgc.kmall.service.SkuService;
import com.kgc.kmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.*;

@Controller
public class CartController {

    @Reference
    SkuService skuService;

    @Reference
    CartService cartService;

    @LoginRequired(false)
    @RequestMapping("/addToCart")
    public String addToCart(long skuId, Integer num, HttpServletRequest request, HttpServletResponse response){
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        // 调用商品服务查询商品信息
        PmsSkuInfo skuInfo = skuService.selectBySkuId(skuId);
        //将商品信息封装成购物车信息
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setCreateDate(new Date());
        omsCartItem.setDeleteStatus(0);
        omsCartItem.setModifyDate(new Date());
        omsCartItem.setPrice(new BigDecimal(skuInfo.getPrice()));
        omsCartItem.setProductAttr("");
        omsCartItem.setProductBrand("");
        omsCartItem.setProductCategoryId(skuInfo.getCatalog3Id());
        omsCartItem.setProductId(skuInfo.getSpuId());
        omsCartItem.setProductName(skuInfo.getSkuName());
        omsCartItem.setProductPic(skuInfo.getSkuDefaultImg());
        omsCartItem.setProductSkuCode("20210108666");
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setQuantity(num);


        // 判断用户是否登录
        String memberId ="";
        if(request.getAttribute("memberId")!=null){
            memberId=request.getAttribute("memberId").toString();
        }

        if(StringUtils.isBlank(memberId)){
            //cookie里原有的购物车数据
            String cartListCookieWork = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if(StringUtils.isBlank(cartListCookieWork)){
                //cookie为空
                //给购物车集合添加封装数据
                omsCartItems.add(omsCartItem);
            }else{
                //cookie不为空
                //将cookie中的数据转换为list类型给 购物车集合
                omsCartItems = JSON.parseArray(cartListCookieWork,OmsCartItem.class);

                // 判断添加的购物车数据在cookie中是否存在
                boolean exist = if_cart_exist(omsCartItems, omsCartItem);
                if(exist){
                    //通过 说明有这件商品只用更新数量就行
                    for (OmsCartItem cartItem : omsCartItems) {
                        //判断需要添加的购物车数据 与 集合中的数据是否相同 如果相同就更改数量
                        if(cartItem.getProductSkuId().equals(omsCartItem.getProductSkuId())){
                            cartItem.setQuantity(cartItem.getQuantity()+omsCartItem.getQuantity());
                            break;
                        }
                    }
                }else{
                    // 之前没有添加，新增当前的购物车
                    omsCartItems.add(omsCartItem);
                }
            }
            // 更新cookie
            CookieUtil.setCookie(request,response,"cartListCookie",JSON.toJSONString(omsCartItems),60*60*72,true);
        }else{
            //如果已登录
            //根据用户id和skuid查询，如果不存在则添加，如果存在则修改
            OmsCartItem omsCartItemFromDb  = cartService.ifCartExistByUser(memberId, skuId);
            if(omsCartItemFromDb==null){
                //如果结果为空 说明没有添加这条数据
                omsCartItem.setMemberId(Long.parseLong(memberId.toString()));
                omsCartItem.setMemberNickname("哈哈哈哈");
                cartService.addCart(omsCartItem);
            }else{
                //不为空的话 就是添加过这件商品 那么只需要更改数量就行了
                Integer quantity = omsCartItemFromDb.getQuantity();
                quantity = quantity +num;
                omsCartItemFromDb.setQuantity(quantity);
                cartService.updateCart(omsCartItemFromDb);
            }

            //同步缓存
            cartService.flushCartCache(memberId);
        }


        return "redirect:/success.html";
    }

    private boolean if_cart_exist(List<OmsCartItem> omsCartItems, OmsCartItem omsCartItem) {
        boolean b=false;
        //循环声明的购物车集合
        for (OmsCartItem cartItem : omsCartItems) {
            //取到购物车结合的skuid
            Long productSkuId = cartItem.getProductSkuId();
            //购物车集合的skuid和数据库购物车的skuid对比 如果相同就返回true
            if(productSkuId.equals(omsCartItem.getProductSkuId().toString())){
                b=true;
                break;
            }
        }
        return b;
    }


    //显示购物车数据
    @LoginRequired(false)
    @RequestMapping("/cartList")
    public String cartList(ModelMap modelMap, HttpServletRequest request){
        //声明一个购物车集合
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        //判断是否登陆
        String memberId = "";
        if(request.getAttribute("memberId")!=null){
            memberId=request.getAttribute("memberId").toString();
        }

        if(StringUtils.isNotBlank(memberId)){
            // 已经登录查询db
            omsCartItems = cartService.cartList(memberId);
        }else{
            // 没有登录查询cookie
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if(StringUtils.isNotBlank(cartListCookie)){
                //将cookie中的数据转换为list类型
                omsCartItems = JSON.parseArray(cartListCookie,OmsCartItem.class);
            }
        }

        BigDecimal totalAmount = getTotalAmount(omsCartItems);
        modelMap.put("totalAmount",totalAmount);
        modelMap.put("cartList",omsCartItems);
        return "cartList";
    }

    @LoginRequired(false)
    @RequestMapping("/checkCart")
    @ResponseBody
    public Map<String,Object> checkCart(Integer isChecked,Long skuId,HttpServletRequest request,HttpServletResponse response){
        Map<String,Object> map=new HashMap<>();
        String memberId = "";
        if(request.getAttribute("memberId")!=null){
            memberId=request.getAttribute("memberId").toString();
        }

        if (StringUtils.isNotBlank(memberId)){
            // 调用服务，修改状态
            OmsCartItem omsCartItem = new OmsCartItem();
            omsCartItem.setMemberId(Long.parseLong(memberId));
            omsCartItem.setProductSkuId(skuId);
            omsCartItem.setIsChecked(isChecked);
            cartService.checkCart(omsCartItem);
            //计算总价
            List<OmsCartItem> omsCartItems = cartService.cartList(memberId);
            BigDecimal totalAmount =getTotalAmount(omsCartItems);
            map.put("totalAmount",totalAmount);
        }else{
            // 没有登录 查询cookie
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if(StringUtils.isNotBlank(cartListCookie)){
                List<OmsCartItem> omsCartItems = JSON.parseArray(cartListCookie,OmsCartItem.class);

                //修改
                for (OmsCartItem omsCartItem : omsCartItems) {
                    if (omsCartItem.getProductSkuId()==skuId){
                        omsCartItem.setIsChecked(isChecked);
                        break;
                    }
                }

                //保存cookie
                CookieUtil.setCookie(request, response, "cartListCookie", JSON.toJSONString(omsCartItems), 60 * 60 * 72, true);

                //计算总价
                BigDecimal totalAmount =getTotalAmount(omsCartItems);
                map.put("totalAmount",totalAmount);
            }

        }

        return map;
    }

    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {
        if (omsCartItems==null||omsCartItems.size()==0){
            return new BigDecimal(0);
        }
        BigDecimal total=new BigDecimal(0);
        for (OmsCartItem omsCartItem : omsCartItems) {
            //计算小计
            BigDecimal multiply = omsCartItem.getPrice().multiply(new BigDecimal(omsCartItem.getQuantity()));
            omsCartItem.setTotalPrice(multiply);

            //计算总价
            if (omsCartItem.getIsChecked()!=null&&omsCartItem.getIsChecked()==1){
                total=total.add(omsCartItem.getTotalPrice());
            }
        }
        return total;
    }

    @LoginRequired(true)
    @RequestMapping("toTrade")
    public String toTrade() {

        return "toTrade";
    }
}
