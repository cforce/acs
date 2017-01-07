package com.ge.predix.acs.request.audit;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
//
//import com.ge.predix.audit.AuditClient;
//import com.ge.predix.audit.AuditCredentials;

@Component
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        MultiReadHttpServletRequest multiReadRequest = new MultiReadHttpServletRequest(request);
        System.out.println(IOUtils.toString(multiReadRequest.getInputStream()));
        System.out.println("Status before is: " + response.getStatus());
        filterChain.doFilter(multiReadRequest, response);
        System.out.println("Status after is: " + response.getStatus());
    }
    
//    private AuditClient createAuditClient() {
//        AuditCredentials credentials = new AuditCredentials(
//            "ehub.<cf_host>:443", //from VCAP credentials of an application bound to the Audit Service
//            "<Event-Hub-Zone-Id>", //from VCAP credentials of an application bound to the Audit Service
//            "<uaa_instance_host>",  //e.g. "https://9d06-2c95a1f4f5ea.predix-uaa.grc-apps.svc.ice.ge.com"
//            "<client_id>:<client_secret>"); //credentials of auditing OAuth client
//        return new AuditClient(credentials);
//    }
}
