package com.kgc.kmall.user;

import com.kgc.kmall.user.bean.Member;
import com.kgc.kmall.user.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
class KmallUserServiceApplicationTests {

	@Resource
	MemberService memberService;
	
	@Test
	void contextLoads() {
        List<Member> members = memberService.selectAllMember();
        for (Member member : members) {
            System.out.println(member.toString());
        }
    }

}
