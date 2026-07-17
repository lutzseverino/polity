package com.odonta.polity.repository;

import com.odonta.polity.model.MembershipInvitation;
import com.odonta.polity.model.MembershipInvitationStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MembershipInvitationRepository extends JpaRepository<MembershipInvitation, UUID> {

  Optional<MembershipInvitation> findEntityById(UUID id);

  boolean existsByPolityIdAndEmailIgnoreCaseAndStatus(
      UUID polityId, String email, MembershipInvitationStatus status);

  Optional<MembershipInvitation> findEntityByIdAndStatus(
      UUID id, MembershipInvitationStatus status);

  Page<MembershipInvitationProjection> findProjectionsByPolityIdOrderByInvitedAtDescIdAsc(
      UUID polityId, Pageable pageable);

  @Query(
      """
      select
        invitation.id as id,
        invitation.polityId as polityId,
        invitation.email as email,
        invitation.invitedBy as invitedBy,
        invitation.status as status,
        invitation.invitedAt as invitedAt,
        invitation.respondedAt as respondedAt
      from MembershipInvitation invitation
      where invitation.status = :status
        and (invitation.invitedUserId = :userId or lower(invitation.email) in :emails)
      order by invitation.invitedAt desc, invitation.id asc
      """)
  Page<MembershipInvitationProjection> findPendingProjectionsForInvitee(
      UUID userId, Collection<String> emails, MembershipInvitationStatus status, Pageable pageable);

  Optional<MembershipInvitationProjection> findProjectedById(UUID id);

  Optional<MembershipInvitationProjection> findProjectedByCardoInvitationId(UUID cardoInvitationId);
}
