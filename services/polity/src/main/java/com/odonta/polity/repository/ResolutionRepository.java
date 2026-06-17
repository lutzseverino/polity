package com.odonta.polity.repository;

import com.odonta.polity.model.Resolution;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResolutionRepository extends JpaRepository<Resolution, UUID> {}
