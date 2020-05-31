package com.hf.app.persistence.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Data
public class StoredChannel {

    private
    @Id
    @GeneratedValue
    Long id;

    private
    @Column(unique = true)
    String name;

    private
    @JsonIgnore
    @Column(length = 10000)
    byte[] channelInfo;

    public StoredChannel(String name, byte[] channelInfo) {
        this.name = name;
        this.channelInfo = channelInfo;
    }

    public StoredChannel() {
    }
}
