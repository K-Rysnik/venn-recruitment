package ca.venn.hometask.model;

import java.time.ZonedDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ca.venn.hometask.model.LoadEntry.LoadEntryId;

public interface LoadEntryRepository extends JpaRepository<LoadEntry, LoadEntryId> {

    @Query("SELECT new ca.venn.hometask.model.LoadEntryAggregate(COALESCE(SUM(le.amountCAD), 0) AS totalAmount, COUNT(le) AS loadCount) " +
            "FROM LoadEntry le " +
            "WHERE le.id.customerId = :customerId AND le.accepted = true AND le.time >= :startDate AND le.time < :endDate")
    LoadEntryAggregate getEntryAggregateByCustomerIdAndTimeBetween(Integer customerId, ZonedDateTime startDate, ZonedDateTime endDate);
}
