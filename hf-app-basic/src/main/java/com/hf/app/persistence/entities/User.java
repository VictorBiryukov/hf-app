package com.hf.app.persistence.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hf.app.utils.CryptoUtils;
import lombok.Data;
import lombok.ToString;
import org.hyperledger.fabric.sdk.Enrollment;
import org.springframework.data.util.Pair;

import javax.persistence.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Entity
@ToString(exclude = "password")
@Data
public class User implements org.hyperledger.fabric.sdk.User {

    private
    @Id
    @GeneratedValue
    Long id;

    private
    @Column(unique = true)
    String name;

    private
    Boolean isAdmin;

    private
    Boolean isPeerAdmin;

    private
    @OneToOne
    Organization organization;

    private String account;

    private String mspId;

    private String[] roles;

    private String affiliation;

    private
    @JsonIgnore
    @Column(length = 1000)
    byte[] key;

    private
    @Column(length = 1000)
    String signedPEM;

    public User() {
    }


    public User(String name, Boolean isAdmin, Boolean isPeerAdmin, Organization organization, Enrollment enrollment, String account, String mspId, Set<String> roles, String affiliation) {

        this.name = name;
        this.isAdmin = isAdmin;
        this.isPeerAdmin = isPeerAdmin;
        this.organization = organization;
        setEnrollment(enrollment);
        this.account = account;
        this.mspId = mspId;
        setRoles(roles);
        this.affiliation = affiliation;
    }

    @Override
    public Enrollment getEnrollment() {
        try {
            return CryptoUtils.getEnrollmentFromKeyAndSignedPEM(this.key, this.signedPEM);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setEnrollment(Enrollment enrollment) {
        if (enrollment == null) {
            key = null;
            signedPEM = null;
        } else {
            Pair<byte[], String> pair = CryptoUtils.getKeyAndSignedPEMFromEnrollment(enrollment);
            key = pair.getFirst();
            signedPEM = pair.getSecond();
        }
    }

    public Set<String> getRoles() {
        return roles == null ? new HashSet<>() : new HashSet<>(Arrays.asList(roles));
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles == null ? null : roles.toArray(new String[roles.size()]);
    }

}
