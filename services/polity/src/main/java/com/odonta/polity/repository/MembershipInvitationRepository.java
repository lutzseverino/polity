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

  @Query(
      """
      select
        invitation.id as id,
        invitation.polityId as polityId,
        polity.name as polityName,
        invitation.email as email,
        inviter.displayName as invitedByName,
        invitation.status as status,
        invitation.invitedAt as invitedAt,
        invitation.respondedAt as respondedAt
      from MembershipInvitation invitation
      join Polity polity on polity.id = invitation.polityId
      join Membership inviter on inviter.polityId = invitation.polityId and inviter.id = invitation.invitedBy
      where invitation.polityId = :polityId
      order by invitation.invitedAt desc
      """)
  List<MembershipInvitationProjection> findProjectionsByPolityId(UUID polityId);

  @Query(
      """
      select
        invitation.id as id,
        invitation.polityId as polityId,
        polity.name as polityName,
        invitation.email as email,
        inviter.displayName as invitedByName,
        invitation.status as status,
        invitation.invitedAt as invitedAt,
        invitation.respondedAt as respondedAt
      from MembershipInvitation invitation
      join Polity polity on polity.id = invitation.polityId
      join Membership inviter on inviter.polityId = invitation.polityId and inviter.id = invitation.invitedBy
      where invitation.status = :status
        and (invitation.invitedUserId = :userId or lower(invitation.email) in :emails)
      order by invitation.invitedAt desc
      """)
  List<MembershipInvitationProjection> findPendingProjectionsForInvitee(
      UUID userId, Collection<String> emails, InvitationStatus status);

  @Query(
      """
      select
        invitation.id as id,
        invitation.polityId as polityId,
        polity.name as polityName,
        invitation.email as email,
        inviter.displayName as invitedByName,
        invitation.status as status,
        invitation.invitedAt as invitedAt,
        invitation.respondedAt as respondedAt
      from MembershipInvitation invitation
      join Polity polity on polity.id = invitation.polityId
      join Membership inviter on inviter.polityId = invitation.polityId and inviter.id = invitation.invitedBy
      where invitation.id = :id
      """)
  Optional<MembershipInvitationProjection> findProjectedById(UUID id);
}
