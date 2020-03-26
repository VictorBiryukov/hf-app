package com.hf.app.persistence.repositories;

import com.hf.app.persistence.entities.Organization;
import org.springframework.data.jpa.repository.JpaRepository;


public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Organization findByName(String name);

    Organization findByMspId(String mspId);

}
