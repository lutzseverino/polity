package com.odonta.polity.repository;

import com.odonta.polity.model.Polity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolityRepository extends JpaRepository<Polity, UUID> {
  List<Polity> findByIdInOrderByCreatedAtDesc(List<UUID> ids);
}
