package com.kgc.kmall.itemweb.controller;

import com.alibaba.fastjson.JSON;
import com.kgc.kmall.bean.PmsProductSaleAttr;
import com.kgc.kmall.bean.PmsSkuInfo;
import com.kgc.kmall.bean.PmsSkuSaleAttrValue;
import com.kgc.kmall.service.SkuService;
import com.kgc.kmall.service.SpuService;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    @Reference
    SkuService skuService;


    @Reference
    SpuService spuService;


    @RequestMapping("{skuId}.html")
    public String item(@PathVariable Long skuId, Model model){
        PmsSkuInfo pmsSkuInfo=skuService.selectBySkuId(skuId);
        model.addAttribute("skuInfo",pmsSkuInfo);
        List<PmsProductSaleAttr> pmsProductSaleAttrs = spuService.spuSaleAttrListIsCheck(pmsSkuInfo.getSpuId(), skuId);
        model.addAttribute("spuSaleAttrListCheckBySku",pmsProductSaleAttrs);

        List<PmsSkuInfo> pmsSkuInfos = skuService.selectBySpuId(pmsSkuInfo.getSpuId());
        Map<String,Long> skuSaleAttrHash=new HashMap<>();
        if(pmsSkuInfos!=null && pmsSkuInfos.size()!=0){
            for (PmsSkuInfo skuInfo : pmsSkuInfos) {
                String k="";
                if(skuInfo.getSkuSaleAttrValueList()!=null && skuInfo.getSkuSaleAttrValueList().size()!=0){
                    for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuInfo.getSkuSaleAttrValueList()) {
                        k+=pmsSkuSaleAttrValue.getSaleAttrValueId()+"|";
                    }
                    skuSaleAttrHash.put(k,skuInfo.getId());
                }
            }
        }
        String string = JSON.toJSONString(skuSaleAttrHash);
        model.addAttribute("skuSaleAttrHashJsonStr",string);
        return "item";
    }

}
