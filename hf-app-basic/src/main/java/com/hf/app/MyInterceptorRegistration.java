package com.hf.app;

import com.hf.app.utils.StudentInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MyInterceptorRegistration implements HibernatePropertiesCustomizer {

    @Autowired
    private StudentInterceptor studentInterceptor;

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put("hibernate.session_factory.interceptor", studentInterceptor);
    }
}
