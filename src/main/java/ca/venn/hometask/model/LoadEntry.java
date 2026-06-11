package ca.venn.hometask.model;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class LoadEntry {
    @EmbeddedId
    LoadEntryId id;

    @Column(name = "amount_cad")
    BigDecimal amountCAD;

    ZonedDateTime time;

    boolean accepted;

    public LoadEntry(LoadEntryId id, BigDecimal amountCAD, ZonedDateTime time, boolean accepted) {
        this.id = id;
        this.amountCAD = amountCAD;
        this.time = time;
        this.accepted = accepted;
    }

    public LoadEntry(LoadEntryId id, BigDecimal amountCAD, ZonedDateTime time) {
        this.id = id;
        this.amountCAD = amountCAD;
        this.time = time;
        this.accepted = true;
    }

    @Embeddable
    @Getter
    @EqualsAndHashCode
    @AllArgsConstructor
    @NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
    public static class LoadEntryId {
        Integer loadId;
        Integer customerId;
    }
}
