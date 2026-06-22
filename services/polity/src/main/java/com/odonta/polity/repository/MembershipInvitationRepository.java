package com.odonta.polity.repository;

import com.odonta.polity.model.InvitationStatus;
import com.odonta.polity.model.MembershipInvitation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MembershipInvitationRepository extends JpaRepository<MembershipInvitation, UUID> {

  boolean existsByPolityIdAndInvitedUserIdAndStatus(
      UUID polityId, UUID invitedUserId, InvitationStatus status);

  boolean existsByPolityIdAndEmailIgnoreCaseAndStatus(
      UUID polityId, String email, InvitationStatus status);

  Optional<MembershipInvitation> findByIdAndStatus(UUID id, InvitationStatus status);

  List<MembershipInvitationProjection> findProjectionsByPolityIdOrderByInvitedAtDesc(UUID polityId);

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
      order by invitation.invitedAt desc
      """)
  List<MembershipInvitationProjection> findPendingProjectionsForInvitee(
      UUID userId, Collection<String> emails, InvitationStatus status);

  Optional<MembershipInvitationProjection> findProjectedById(UUID id);
}
