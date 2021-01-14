package com.kgc.kmall.user.service;


import com.alibaba.fastjson.JSON;
import com.kgc.kmall.bean.Member;
import com.kgc.kmall.bean.MemberExample;
import com.kgc.kmall.service.MemberService;
import com.kgc.kmall.user.mapper.MemberMapper;
import com.kgc.kmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import java.util.List;

@Component
@Service
public class MemberServiceImpl implements MemberService {

    @Resource
    MemberMapper memberMapper;

    @Resource
    RedisUtil redisUtil;

    @Override
    public List<Member> selectAllMember() {
        return memberMapper.selectByExample(null);
    }

    @Override
    public Member login(Member member) {
        //先从redis中进行查询
        Jedis jedis=null;
        try {
            //连接jedis
            jedis = redisUtil.getJedis();
            //不为空
            if(jedis!=null){
                //获取"user:" + member.getId() + ":info"这个键
                String umsMemberStr = jedis.get("user:" + member.getUsername() + ":info");
                //如果这个键不为空
                if (StringUtils.isNotBlank(umsMemberStr)) {
                    // 密码正确
                    //将jedis键中的值转换为对象
                    Member umsMemberFromCache = JSON.parseObject(umsMemberStr, Member.class);
                    return umsMemberFromCache;
                }
            }
            // 链接redis失败，开启数据库
            Member umsMemberFromDb =loginFromDb(member);
            //如果能查到就存入缓存设置有效期
            if(umsMemberFromDb!=null){
                jedis.setex("user:" + umsMemberFromDb.getId() + ":info",60*60*24, JSON.toJSONString(umsMemberFromDb));
            }
            return umsMemberFromDb;
        }finally {
            jedis.close();
        }
    }

    //账号登录
    private Member loginFromDb(Member member) {
        MemberExample example=new MemberExample();
        MemberExample.Criteria criteria = example.createCriteria();
        criteria.andUsernameEqualTo(member.getUsername());
        criteria.andPasswordEqualTo(member.getPassword());
        List<Member> members = memberMapper.selectByExample(example);
        if (members.size()!=0 && members!=null) {
            return members.get(0);
        }
        return null;
    }

    @Override
    public void addUserToken(String token, Long id) {
        //连接jedis
        Jedis jedis = redisUtil.getJedis();
        //把token存入缓存
        jedis.setex("user:"+id+":token",60*60*2,token);
        //关闭jedis
        jedis.close();
    }
}
