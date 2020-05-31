package com.hf.app;

import com.hf.app.utils.StudentInterceptor;
import org.h2.tools.Server;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class AppConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2DatabaseServer() throws SQLException {
        return Server.createTcpServer(
                "-tcp", "-tcpAllowOthers", "-tcpPort", "9090");
    }

    @Bean
    public HFCAClient platformHFCAClient(
            @Value("${ca.name}") final String caName,
            @Value("${ca.location}") final String caLocation
    ) throws MalformedURLException,
            InvalidArgumentException,
            IllegalAccessException,
            InvocationTargetException,
            org.hyperledger.fabric.sdk.exception.InvalidArgumentException,
            InstantiationException,
            NoSuchMethodException,
            CryptoException,
            ClassNotFoundException {

        HFCAClient ca = HFCAClient.createNewInstance(caName, caLocation, null);
        ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        return ca;
    }


    @Bean
    public HFClient hfClient()
            throws IllegalAccessException, InvocationTargetException, org.hyperledger.fabric.sdk.exception.InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException {

        HFClient hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        return hfClient;
    }


    @Bean
    public StudentInterceptor studentInterceptor(@Lazy PlatformInit platformInit) {
        return new StudentInterceptor(platformInit);
    }

}
