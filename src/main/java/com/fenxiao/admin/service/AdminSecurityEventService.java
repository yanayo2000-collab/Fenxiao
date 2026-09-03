package com.fenxiao.admin.service;

import com.fenxiao.admin.api.dto.AdminSecurityEventResponse;
import com.fenxiao.admin.entity.AdminSecurityEvent;
import com.fenxiao.admin.repository.AdminSecurityEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminSecurityEventService {
    private final AdminSecurityEventRepository repository;
    public AdminSecurityEventService(AdminSecurityEventRepository repository){this.repository=repository;}
    public boolean knownIp(Long accountId,String ip){return accountId!=null&&ip!=null&&repository.existsByAccountIdAndIpAddressAndSuccessTrue(accountId,ip);}
    public void record(Long accountId,String username,String type,boolean success,String ip,String userAgent,String detail){repository.save(AdminSecurityEvent.create(accountId,username,type,success,trim(ip,64),trim(userAgent,512),trim(detail,512),LocalDateTime.now()));}
    public List<AdminSecurityEventResponse> recent(Long accountId){return repository.findByAccountIdOrderByOccurredAtDesc(accountId,PageRequest.of(0,50)).stream().map(e->new AdminSecurityEventResponse(e.getId(),e.getAccountId(),e.getUsername(),e.getEventType(),e.isSuccess(),e.getIpAddress(),e.getUserAgent(),e.getDetail(),e.getOccurredAt())).toList();}
    private String trim(String v,int max){return v==null?null:v.substring(0,Math.min(v.length(),max));}
}
