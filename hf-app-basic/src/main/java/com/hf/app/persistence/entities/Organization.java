package com.hf.app.persistence.entities;


import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Data
public class Organization {


    private
    @Id
    @GeneratedValue
    Long id;

    private
    @Column(unique = true)
    String name;


    private
    @Column(unique = true)
    String mspId;

    public Organization() {
    }


    public Organization(String name, String mspId) {
        this.name = name;
        this.mspId = mspId;
    }
}
