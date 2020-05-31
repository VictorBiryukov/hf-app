package com.hf.app.persistence.repositories;

import com.hf.app.persistence.entities.StoredChannel;
import com.hf.app.persistence.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface StoredChannelRepository extends JpaRepository<StoredChannel, Long> {

    StoredChannel findByName(String name);
}
