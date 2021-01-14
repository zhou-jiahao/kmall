package com.kgc.kmall.interceptors;

import com.alibaba.fastjson.JSON;
import com.kgc.kmall.annotations.LoginRequired;
import com.kgc.kmall.util.CookieUtil;
import com.kgc.kmall.util.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断是否是HandlerMethod，因为访问静态资源时handler是ResourceHttpRequestHandler
        if(handler.getClass().equals(HandlerMethod.class)){
            //获取注解信息
            HandlerMethod hm=(HandlerMethod)handler;
            LoginRequired methodAnnotation = hm.getMethodAnnotation(LoginRequired.class);
            if(methodAnnotation!=null){
                Map statusMap=new HashMap();
                //定义一个token
                String token="";
                //从Cookie中取
                String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);
                if(StringUtils.isNotBlank(oldToken)){
                    token=oldToken;
                }
                //取token
                String newToken = request.getParameter("token");
                if(StringUtils.isNotBlank(newToken)){
                    //如果不为空 把取到的新的覆盖掉旧的
                    token=newToken;
                }
                String result="fail";
                if(StringUtils.isNotBlank(token)){

                    String ip = request.getRemoteAddr();// 从request中获取ip
                    if(StringUtils.isBlank(ip)||ip.equals("0:0:0:0:0:0:0:1")){
                        ip = "127.0.0.1";
                    }

                    String doGet = HttpclientUtil.doGet("http://passport.kmall.com:8086/verify?token=" + token + "&currentIp=" + ip);

                    statusMap = JSON.parseObject(doGet, Map.class);

                    result = statusMap.get("status").toString();
                }


                // 没有LoginRequired注解不拦截
                //获取注解的值
                boolean value = methodAnnotation.value();
                //判断是true还是false
                if(value){
                    //如果为true
                    //System.out.println("拦截必须登录");
                    if(result.equals("success")==false){
                        //重定向会passport登录
                        StringBuffer requestURL = request.getRequestURL();
                        response.sendRedirect("http://passport.kmall.com:8086/index?ReturnUrl="+requestURL);
                        return false;
                    }

                    request.setAttribute("memberId",statusMap.get("memberId"));
                    request.setAttribute("nickname",statusMap.get("nickname"));

                    if(StringUtils.isNotBlank(token)){
                        CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
                    }
                }
                //System.out.println("拦截不必登录");
                if(result.equals("success")){
                    request.setAttribute("memberId",statusMap.get("memberId"));
                    request.setAttribute("nickname",statusMap.get("nickname"));

                    if(StringUtils.isNotBlank(token)){
                        CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
                    }
                }
            }
            //System.out.println("没有拦截");
        }
        //System.out.println("前段资源不拦截");
        return true;
    }
}
