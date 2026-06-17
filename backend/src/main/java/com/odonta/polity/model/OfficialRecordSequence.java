package com.odonta.polity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "official_record_sequences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficialRecordSequence {
  @Id
  @Column(name = "polity_id")
  private UUID polityId;

  @Column(name = "next_entry_number", nullable = false)
  private int nextEntryNumber;

  public OfficialRecordSequence(UUID polityId) {
    this.polityId = polityId;
    this.nextEntryNumber = 1;
  }

  public int claimNextEntryNumber() {
    int claimed = nextEntryNumber;
    nextEntryNumber += 1;
    return claimed;
  }
}
