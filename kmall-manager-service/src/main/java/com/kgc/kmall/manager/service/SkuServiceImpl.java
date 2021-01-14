package com.kgc.kmall.manager.service;

import com.alibaba.fastjson.JSON;
import com.kgc.kmall.bean.*;
import com.kgc.kmall.manager.mapper.PmsSkuAttrValueMapper;
import com.kgc.kmall.manager.mapper.PmsSkuImageMapper;
import com.kgc.kmall.manager.mapper.PmsSkuInfoMapper;
import com.kgc.kmall.manager.mapper.PmsSkuSaleAttrValueMapper;
import com.kgc.kmall.service.SkuService;
import com.kgc.kmall.util.RedisUtil;
import org.apache.dubbo.config.annotation.Service;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;

@Component
@Service
public class SkuServiceImpl implements SkuService {

    @Resource
    PmsSkuInfoMapper pmsSkuInfoMapper;
    @Resource
    PmsSkuImageMapper pmsSkuImageMapper;
    @Resource
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;
    @Resource
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;

    @Resource
    RedisUtil redisUtil;
    @Resource
    RedissonClient redissonClient;


    @Override
    public String saveSkuInfo(PmsSkuInfo skuInfo) {
        pmsSkuInfoMapper.insert(skuInfo);
        List<PmsSkuImage> skuImageList = skuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : skuImageList) {
            pmsSkuImage.setSkuId(skuInfo.getId());
            pmsSkuImageMapper.insert(pmsSkuImage);
        }
        List<PmsSkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
            pmsSkuAttrValue.setSkuId(skuInfo.getId());
            pmsSkuAttrValueMapper.insert(pmsSkuAttrValue);
        }
        List<PmsSkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
            pmsSkuSaleAttrValue.setSkuId(skuInfo.getId());
            pmsSkuSaleAttrValueMapper.insert(pmsSkuSaleAttrValue);
        }
        return "success";
    }

    @Override
    public PmsSkuInfo selectBySkuId(Long skuId) {
        PmsSkuInfo pmsSkuInfo=null;
        Jedis jedis=redisUtil.getJedis();
        String keys="sku:"+skuId+":info";
        String jsonSku = jedis.get(keys);
        if(jsonSku!=null){
            pmsSkuInfo = JSON.parseObject(jsonSku, PmsSkuInfo.class);
            return pmsSkuInfo;
        }else{
            Lock lock = redissonClient.getLock("lock");
            lock.lock();
            try {
                pmsSkuInfo = pmsSkuInfoMapper.selectByPrimaryKey(skuId);
                //防止缓存穿透，从DB中找不到数据也要缓存，但是缓存时间不要太长
                if(pmsSkuInfo!=null){
                    //保存到redis
                    String jsonString = JSON.toJSONString(pmsSkuInfo);
                    //有效期随机，防止缓存雪崩
                    Random random=new Random();
                    int i = random.nextInt(10);
                    jedis.setex(keys,i*60*1000,jsonString);
                }else{
                    jedis.setex(keys,5*60*1000,"empty");
                }
            } finally {
                lock.unlock();
            }
        }
        jedis.close();
        return pmsSkuInfo;
    }

//    @Override
//    public PmsSkuInfo selectBySkuId(Long skuId) {
//        PmsSkuInfo pmsSkuInfo=null;
//        Jedis jedis=redisUtil.getJedis();
//        String keys="sku:"+skuId+":info";
//        String jsonSku = jedis.get(keys);
//        if(jsonSku!=null){
//            pmsSkuInfo = JSON.parseObject(jsonSku, PmsSkuInfo.class);
//            return pmsSkuInfo;
//        }else{
//            //使用nx分布式锁，避免缓存击穿
//            //创建redis中分布式锁的键
//            String skuLockKey="sku:"+skuId+":lock";
//            //随机生成一个UUID
//            String SkuUUID = UUID.randomUUID().toString();
//            String lock = jedis.set(skuLockKey, SkuUUID, "NX", "PX", 60 * 1000);
//            if(lock.equals("OK")){
//                pmsSkuInfo = pmsSkuInfoMapper.selectByPrimaryKey(skuId);
//                //防止缓存穿透，从DB中找不到数据也要缓存，但是缓存时间不要太长
//                if(pmsSkuInfo!=null){
//                    //保存到redis
//                    String jsonString = JSON.toJSONString(pmsSkuInfo);
//                    //有效期随机，防止缓存雪崩
//                    Random random=new Random();
//                    int i = random.nextInt(10);
//                    jedis.setex(keys,i*60*1000,jsonString);
//                }else{
//                    jedis.setex(keys,5*60*1000,"empty");
//                }
//                //删除分布式锁
//                  //String SkuUUID2 = jedis.get(skuLockKey);
////                if(SkuUUID2.equals(SkuUUID)) {
////                    jedis.del(skuLockKey);
////                }
//                String script ="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
//                jedis.eval(script, Collections.singletonList(skuLockKey),Collections.singletonList(SkuUUID ));
//            }else{
//                try {
//                    Thread.sleep(3000);
//                }catch (Exception ex){
//                    ex.printStackTrace();
//                }
//                return selectBySkuId(skuId);
//            }
//
//        }
//        jedis.close();
//        return pmsSkuInfo;
//    }

    @Override
    public List<PmsSkuInfo> selectBySpuId(Long spuId) {
        return pmsSkuInfoMapper.selectBySpuId(spuId);
    }

    @Override
    public List<PmsSkuInfo> getAllSku() {
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectByExample(null);
        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos) {
            PmsSkuAttrValueExample example=new PmsSkuAttrValueExample();
            example.createCriteria().andSkuIdEqualTo(pmsSkuInfo.getId());
            List<PmsSkuAttrValue> pmsSkuAttrValues = pmsSkuAttrValueMapper.selectByExample(example);
            pmsSkuInfo.setSkuAttrValueList(pmsSkuAttrValues);
        }
        return pmsSkuInfos;
    }
}
