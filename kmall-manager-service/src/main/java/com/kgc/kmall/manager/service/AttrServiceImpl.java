package com.kgc.kmall.manager.service;

import com.alibaba.fastjson.JSON;
import com.kgc.kmall.bean.PmsBaseAttrInfo;
import com.kgc.kmall.bean.PmsBaseAttrInfoExample;
import com.kgc.kmall.bean.PmsBaseAttrValue;
import com.kgc.kmall.bean.PmsBaseAttrValueExample;
import com.kgc.kmall.manager.mapper.PmsBaseAttrInfoMapper;
import com.kgc.kmall.service.AttrService;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

@Component
@Service
public class AttrServiceImpl implements AttrService {

    @Resource
    PmsBaseAttrInfoMapper pmsBaseAttrInfoMapper;

    @Resource
    com.kgc.kmall.manager.mapper.PmsBaseAttrValueMapper PmsBaseAttrValueMapper;

    @Override
    public List<PmsBaseAttrInfo> select(Long catalog3Id) {
        PmsBaseAttrInfoExample example=new PmsBaseAttrInfoExample();
        PmsBaseAttrInfoExample.Criteria criteria = example.createCriteria();
        criteria.andCatalog3IdEqualTo(catalog3Id);
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = pmsBaseAttrInfoMapper.selectByExample(example);
        //为每个平台属性添加属性值
//        for (PmsBaseAttrInfo pmsBaseAttrInfo : pmsBaseAttrInfos) {
//            PmsBaseAttrValueExample valueExample=new PmsBaseAttrValueExample();
//            valueExample.createCriteria().andAttrIdEqualTo(pmsBaseAttrInfo.getId());
//            List<PmsBaseAttrValue> pmsBaseAttrValues = PmsBaseAttrValueMapper.selectByExample(valueExample);
//            pmsBaseAttrInfo.setAttrValueList(pmsBaseAttrValues);
//        }
        return pmsBaseAttrInfos;
    }

    @Override
    public Integer add(PmsBaseAttrInfo attrInfo) {
        try {
            //判断添加还是修改 id是否为null
            if(attrInfo.getId()==null){
                //添加，添加属性，(返回自增的id)
                pmsBaseAttrInfoMapper.insert(attrInfo);
            }else{
                //修改，修改属性
                pmsBaseAttrInfoMapper.updateByPrimaryKeySelective(attrInfo);
                //删除原属性值
                PmsBaseAttrValueExample example=new PmsBaseAttrValueExample();
                example.createCriteria().andAttrIdEqualTo(attrInfo.getId());
                PmsBaseAttrValueMapper.deleteByExample(example);
            }
            //批量添加属性值（属性id，list<属性值>）
            PmsBaseAttrValueMapper.insertBatch(attrInfo.getId(),attrInfo.getAttrValueList());
            return 1;
        }catch (Exception e){
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public List<PmsBaseAttrValue> getAttrValueList(Long attrId) {
        PmsBaseAttrValueExample example=new PmsBaseAttrValueExample();
        PmsBaseAttrValueExample.Criteria criteria = example.createCriteria();
        criteria.andAttrIdEqualTo(attrId);
        return PmsBaseAttrValueMapper.selectByExample(example);
    }

    @Override
    public List<PmsBaseAttrInfo> selectAttrInfoValueListByValueId(Set<Long> valueIds) {
        //获取Set集合里的值
        String join = StringUtils.join(valueIds);
        //截取第一位和最后一位
        join=join.substring(1,join.length()-1);
        System.out.println(join);
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = pmsBaseAttrInfoMapper.selectAttrInfoValueListByValueId(join);
        return pmsBaseAttrInfos;
    }
}
