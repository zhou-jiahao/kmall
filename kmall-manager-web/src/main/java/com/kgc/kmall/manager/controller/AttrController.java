package com.kgc.kmall.manager.controller;

import com.kgc.kmall.bean.PmsBaseAttrInfo;
import com.kgc.kmall.bean.PmsBaseAttrValue;
import com.kgc.kmall.service.AttrService;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
public class AttrController {

    @Reference
    AttrService attrService;

    @RequestMapping("/attrInfoList")
    public List<PmsBaseAttrInfo> attrInfoList(Long catalog3Id){
        return attrService.select(catalog3Id);
    }


    @RequestMapping("/saveAttrInfo")
    public Integer saveAttrInfo(@RequestBody PmsBaseAttrInfo attrInfo){
        Integer i = attrService.add(attrInfo);
        return i;
    }

    @RequestMapping("/getAttrValueList")
    public List<PmsBaseAttrValue> getAttrValueList(Long attrId){
        return attrService.getAttrValueList(attrId);
    }

}
