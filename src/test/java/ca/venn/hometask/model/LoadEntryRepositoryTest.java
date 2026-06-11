package ca.venn.hometask.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import ca.venn.hometask.model.LoadEntry.LoadEntryId;

@DataJpaTest
public class LoadEntryRepositoryTest {
    private static final Integer CUSTOMER_ID_1 = 1;
    private static final Integer CUSTOMER_ID_2 = 2;
    private static final BigDecimal AMOUNT_100 = new BigDecimal("100.00");
    private static final BigDecimal AMOUNT_200 = new BigDecimal("200.00");

    @Autowired
    private LoadEntryRepository repository;

    @Test
    void shouldFindAggregateWithinRange() {
        // given
        ZonedDateTime entryTime = ZonedDateTime.now();
        repository.saveAndFlush(new LoadEntry(new LoadEntryId(1, CUSTOMER_ID_1), AMOUNT_100, entryTime));

        // when
        LoadEntryAggregate result = repository.getEntryAggregateByCustomerIdAndTimeBetween(
                CUSTOMER_ID_1, entryTime.minusHours(1), entryTime.plusHours(1));

        // then
        assertNotNull(result);
        assertEquals(AMOUNT_100, result.totalAmount());
        assertEquals(1, result.loadCount());
    }

    @Test
    void shouldSumEntriesWithinRange() {
        // given
        ZonedDateTime entryTime = ZonedDateTime.now();
        repository.saveAndFlush(new LoadEntry(new LoadEntryId(1, CUSTOMER_ID_1), AMOUNT_100, entryTime));
        repository.saveAndFlush(new LoadEntry(new LoadEntryId(2, CUSTOMER_ID_1), AMOUNT_100, entryTime));

        // when
        LoadEntryAggregate result = repository.getEntryAggregateByCustomerIdAndTimeBetween(
                CUSTOMER_ID_1, entryTime.minusHours(1), entryTime.plusHours(1));

        // then
        assertNotNull(result);
        assertEquals(AMOUNT_200, result.totalAmount());
        assertEquals(2, result.loadCount());
    }

    @Test
    void shouldFindAggregateWithinRangeInclusiveStart() {
        // given
        ZonedDateTime entryTime = ZonedDateTime.now();
        repository.saveAndFlush(new LoadEntry(new LoadEntryId(1, CUSTOMER_ID_1), AMOUNT_100, entryTime));

        // when
        LoadEntryAggregate result = repository.getEntryAggregateByCustomerIdAndTimeBetween(
                CUSTOMER_ID_1, entryTime, entryTime.plusHours(1));

        // then
        assertNotNull(result);
        assertEquals(AMOUNT_100, result.totalAmount());
        assertEquals(1, result.loadCount());
    }

    @Test
    void shouldGetAggregateWithoutEntries() {
        // given
        ZonedDateTime entryTime = ZonedDateTime.now();

        // when
        LoadEntryAggregate result = repository.getEntryAggregateByCustomerIdAndTimeBetween(
                CUSTOMER_ID_1, entryTime, entryTime.plusHours(1));

        // then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.totalAmount());
        assertEquals(0, result.loadCount());
    }

    @Test
    void shouldFilterEntriesByCustomerId() {
        // given
        ZonedDateTime entryTime = ZonedDateTime.now();

        repository.saveAndFlush(new LoadEntry(new LoadEntryId(1, CUSTOMER_ID_1), AMOUNT_100, entryTime));
        repository.saveAndFlush(new LoadEntry(new LoadEntryId(2, CUSTOMER_ID_2), AMOUNT_100, entryTime));

        // when
        LoadEntryAggregate result = repository.getEntryAggregateByCustomerIdAndTimeBetween(
                CUSTOMER_ID_1, entryTime.minusHours(1), entryTime.plusHours(1));

        // then
        assertNotNull(result);
        assertEquals(AMOUNT_100, result.totalAmount());
        assertEquals(1, result.loadCount());
    }

    @Test
    void shouldExcludeEntriesOutsideRangeAfterEndDate() {
        // given
        ZonedDateTime entryTime = ZonedDateTime.now();
        repository.saveAndFlush(new LoadEntry(new LoadEntryId(1, CUSTOMER_ID_1), AMOUNT_100, entryTime));

        // when
        LoadEntryAggregate result = repository.getEntryAggregateByCustomerIdAndTimeBetween(
                CUSTOMER_ID_1, entryTime.minusHours(1), entryTime.minusMinutes(1));

        // then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.totalAmount());
        assertEquals(0, result.loadCount());
    }

    @Test
    void shouldExcludeEntriesOutsideRangeBeforeStartDate() {
        // given
        ZonedDateTime entryTime = ZonedDateTime.now();
        repository.saveAndFlush(new LoadEntry(new LoadEntryId(1, CUSTOMER_ID_1), AMOUNT_100, entryTime));

        // when
        LoadEntryAggregate result = repository.getEntryAggregateByCustomerIdAndTimeBetween(
                CUSTOMER_ID_1, entryTime.plusMinutes(1), entryTime.plusHours(1));

        // then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.totalAmount());
        assertEquals(0, result.loadCount());
    }

    @Test
    void shouldExcludeEntriesOutsideRangeEndDateExclusive() {
        // given
        ZonedDateTime entryTime = ZonedDateTime.now();
        repository.saveAndFlush(new LoadEntry(new LoadEntryId(1, CUSTOMER_ID_1), AMOUNT_100, entryTime));

        // when
        LoadEntryAggregate result = repository.getEntryAggregateByCustomerIdAndTimeBetween(
                CUSTOMER_ID_1, entryTime.minusHours(1), entryTime);

        // then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.totalAmount());
        assertEquals(0, result.loadCount());
    }

    @Test
    void shouldIgnoreEntriesNotAcceptedInAggregate() {
        // given
        ZonedDateTime entryTime = ZonedDateTime.now();
        repository.saveAndFlush(new LoadEntry(new LoadEntryId(1, CUSTOMER_ID_1), AMOUNT_100, entryTime));
        repository.saveAndFlush(new LoadEntry(new LoadEntryId(2, CUSTOMER_ID_1), AMOUNT_100, entryTime, false));

        // when
        LoadEntryAggregate result = repository.getEntryAggregateByCustomerIdAndTimeBetween(
                CUSTOMER_ID_1, entryTime.minusHours(1), entryTime.plusHours(1));

        // then
        assertNotNull(result);
        assertEquals(AMOUNT_100, result.totalAmount());
        assertEquals(1, result.loadCount());
    }
}
