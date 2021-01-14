package com.kgc.kmall.search.controller;

import com.kgc.kmall.annotations.LoginRequired;
import com.kgc.kmall.bean.*;
import com.kgc.kmall.service.AttrService;
import com.kgc.kmall.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@Controller
public class SearchController {

    @Reference
    SearchService searchService;

    @Reference
    AttrService attrService;

    @LoginRequired(false)
    @RequestMapping("/index.html")
    public String index(){        return "index";}

    @RequestMapping("/list.html")
    public String list(PmsSearchSkuParam searchSkuParam, Model model){
        List<PmsSearchSkuInfo> pmsSearchSkuInfos = searchService.list(searchSkuParam);
        model.addAttribute("skuLsInfoList",pmsSearchSkuInfos);
        //平台属性去重
        Set<Long> valueSet=new HashSet<>();
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
            for (int i = 0; i < pmsSearchSkuInfo.getSkuAttrValueList().size(); i++) {
                Map<Object,Object> pmsSkuAttrValue = (Map<Object,Object>)pmsSearchSkuInfo.getSkuAttrValueList().get(i);
                System.out.println(pmsSkuAttrValue);
                valueSet.add(Long.parseLong(pmsSkuAttrValue.get("valueId").toString()));
            }
        }
        //平台属性和平台属性值
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = attrService.selectAttrInfoValueListByValueId(valueSet);
        model.addAttribute("attrList",pmsBaseAttrInfos);

        //拼接平台属性URL
        String urlParam=getURLParam(searchSkuParam);
        model.addAttribute("urlParam",urlParam);

        //显示关键字
        model.addAttribute("keyword",searchSkuParam.getKeyword());

        //已选中的valueId
        String[] valueId = searchSkuParam.getValueId();

        //封装面包屑数据
        if(valueId!=null){
            List<PmsSearchCrumb> pmsSearchCrumbList=new ArrayList<>();
            for (String s : valueId) {
                PmsSearchCrumb pmsSearchCrumb=new PmsSearchCrumb();
                pmsSearchCrumb.setValueName(getValueName(pmsBaseAttrInfos,s));
                pmsSearchCrumb.setValueId(s);
                pmsSearchCrumb.setUrlParam(getURLParam(searchSkuParam,s));

                pmsSearchCrumbList.add(pmsSearchCrumb);
            }
            model.addAttribute("attrValueSelectedList", pmsSearchCrumbList);
        }


        if(valueId!=null){
            //利用迭代器排除已选的平台属性,删除集合元素不能使用for循环，因为会出现数组越界
            Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfos.iterator();
            while (iterator.hasNext()){
                PmsBaseAttrInfo next = iterator.next();
                for (PmsBaseAttrValue pmsBaseAttrValue : next.getAttrValueList()) {
                    for (String s : valueId) {
                        if(s.equals(pmsBaseAttrValue.getId().toString())){
                            iterator.remove();
                        }
                    }
                }
            }
        }


        return "list";
    }


    //根据参数对象拼接URL
    public String getURLParam(PmsSearchSkuParam pmsSearchSkuParam){
        StringBuffer stringBuffer=new StringBuffer();
        String keyword = pmsSearchSkuParam.getKeyword();
        String catalog3Id = pmsSearchSkuParam.getCatalog3Id();
        String[] valueId = pmsSearchSkuParam.getValueId();

        if (StringUtils.isNotBlank(catalog3Id)){
            stringBuffer.append("&catalog3Id="+catalog3Id);
        }
        if (StringUtils.isNotBlank(keyword)){
            stringBuffer.append("&keyword="+keyword);
        }
        if (valueId!=null){
            for (String pmsSkuAttrValue : valueId) {
                stringBuffer.append("&valueId="+pmsSkuAttrValue);
            }
        }
        return stringBuffer.substring(1);
    }
    //封装面包屑数据-urlParam
    //面包屑对应的urlParam=当前URL中的valueId-面包屑的valueId
    public String getURLParam(PmsSearchSkuParam pmsSearchSkuParam,String vid){
        StringBuffer stringBuffer=new StringBuffer();
        String keyword = pmsSearchSkuParam.getKeyword();
        String catalog3Id = pmsSearchSkuParam.getCatalog3Id();
        String[] valueId = pmsSearchSkuParam.getValueId();

        if (StringUtils.isNotBlank(catalog3Id)){
            stringBuffer.append("&catalog3Id="+catalog3Id);
        }
        if (StringUtils.isNotBlank(keyword)){
            stringBuffer.append("&keyword="+keyword);
        }
        //面包屑的url是不能包括自身的valueId的，因为点击面包屑以后会从当前URL中去除面包屑的valueId
        if (valueId!=null){
            for (String pmsSkuAttrValue : valueId) {
                if(vid.equals(pmsSkuAttrValue)==false)
                stringBuffer.append("&valueId="+pmsSkuAttrValue);
            }
        }
        return stringBuffer.substring(1);
    }

    //根据valueId查询valueName
    public String getValueName(List<PmsBaseAttrInfo> pmsBaseAttrInfos,String valueId){
        String valueName="";
        for (PmsBaseAttrInfo pmsBaseAttrInfo : pmsBaseAttrInfos) {
            for (PmsBaseAttrValue pmsBaseAttrValue : pmsBaseAttrInfo.getAttrValueList()) {
                if(valueId.equals(pmsBaseAttrValue.getId().toString())){
                    valueName=pmsBaseAttrInfo.getAttrName()+":"+pmsBaseAttrValue.getValueName();
                }
            }
        }
        return valueName;
    }
}
