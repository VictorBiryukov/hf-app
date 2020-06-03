package com.hf.app.utils;

import com.hf.app.PlatformInit;
import com.hf.app.persistence.entities.Student;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.springframework.context.annotation.Lazy;

import java.io.Serializable;


public class StudentInterceptor extends EmptyInterceptor {


    private final PlatformInit platformInit;

    public StudentInterceptor(@Lazy PlatformInit platformInit) {
        this.platformInit = platformInit;
    }

    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {

        if (!(entity instanceof Student)) {
            return false;
        }


        if (platformInit != null) {
            try {
                platformInit.add(((Student) entity).getHashValue());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return false;
    }


}
