package com.fenxiao.admin.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;

@Component
public class AdminSessionCookieFilter extends OncePerRequestFilter {
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) { return !request.getRequestURI().startsWith("/admin/"); }

    @Override
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String header=request.getHeader("X-Admin-Session"); String cookie=readCookie(request);
        if((header==null||header.isBlank())&&cookie!=null&&!cookie.isBlank()){
            HttpServletRequestWrapper wrapped=new HttpServletRequestWrapper(request){
                @Override public String getHeader(String name){return "X-Admin-Session".equalsIgnoreCase(name)?cookie:super.getHeader(name);}
                @Override public Enumeration<String> getHeaders(String name){
                    return "X-Admin-Session".equalsIgnoreCase(name)
                            ? Collections.enumeration(java.util.List.of(cookie))
                            : super.getHeaders(name);
                }
                @Override public Enumeration<String> getHeaderNames(){
                    LinkedHashSet<String> names=new LinkedHashSet<>();
                    Enumeration<String> existing=super.getHeaderNames();
                    if(existing!=null)while(existing.hasMoreElements())names.add(existing.nextElement());
                    names.add("X-Admin-Session");
                    return Collections.enumeration(names);
                }
            };
            chain.doFilter(wrapped,response); return;
        }
        chain.doFilter(request,response);
    }

    private String readCookie(HttpServletRequest request){
        if(request.getCookies()==null)return null;
        for(Cookie cookie:request.getCookies())if("bandeira_admin_session".equals(cookie.getName()))return cookie.getValue();
        return null;
    }
}
