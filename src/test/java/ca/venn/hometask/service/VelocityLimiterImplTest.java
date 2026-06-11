package ca.venn.hometask.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Currency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.venn.hometask.api.Amount;
import ca.venn.hometask.api.LoadOrder;
import ca.venn.hometask.api.LoadResult;
import ca.venn.hometask.model.LoadEntry;
import ca.venn.hometask.model.LoadEntryAggregate;
import ca.venn.hometask.model.LoadEntryRepository;

@ExtendWith(MockitoExtension.class)
class VelocityLimiterImplTest {
    private static final Integer CUSTOMER_ID = 1;
    private static final Integer LOAD_ID = 1;
    private static final Currency CAD = Currency.getInstance("CAD");
    private static final BigDecimal AMOUNT_1000 = new BigDecimal("1000");
    private static final BigDecimal AMOUNT_5000 = new BigDecimal("5000");
    private static final BigDecimal AMOUNT_20000 = new BigDecimal("20000");
    private static final ZoneId ZONE = ZoneId.of("UTC");
    private static final ZonedDateTime TIME = ZonedDateTime.now(ZONE);

    @Mock
    private LoadEntryRepository loadEntryRepository;

    @InjectMocks
    private VelocityLimiterImpl velocityLimiter;

    @Test
    void shouldAcceptLoadWhenWithinDailyAndWeeklyLimits() {
        // given
        LoadOrder request = new LoadOrder(LOAD_ID, CUSTOMER_ID, new Amount(CAD, AMOUNT_1000), TIME);
        LoadEntryAggregate aggregate = new LoadEntryAggregate(BigDecimal.ZERO, 0L);

        BDDMockito.given(loadEntryRepository.existsById(any(LoadEntry.LoadEntryId.class))).willReturn(false);
        BDDMockito.given(loadEntryRepository.getEntryAggregateByCustomerIdAndTimeBetween(eq(CUSTOMER_ID), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .willReturn(aggregate);

        // when
        LoadResult result = velocityLimiter.attemptLoad(request);

        // then
        assertEquals(LOAD_ID, result.id());
        assertEquals(CUSTOMER_ID, result.customerId());
        assertEquals(true, result.accepted());
        verify(loadEntryRepository).save(any(LoadEntry.class));
    }

    @Test
    void shouldRejectDuplicateLoadOrder() {
        // given
        LoadOrder request = new LoadOrder(LOAD_ID, CUSTOMER_ID, new Amount(CAD, AMOUNT_1000), TIME);
        BDDMockito.given(loadEntryRepository.existsById(any(LoadEntry.LoadEntryId.class))).willReturn(true);

        // when
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> velocityLimiter.attemptLoad(request));

        // then
        assertEquals("Load order with id 1 for customer 1 already exists", exception.getMessage());
    }

    @Test
    void shouldRejectWhenDailyLoadCountExceeded() {
        // given
        LoadOrder request = new LoadOrder(LOAD_ID, CUSTOMER_ID, new Amount(CAD, AMOUNT_1000), TIME);
        LoadEntryAggregate aggregate = new LoadEntryAggregate(AMOUNT_1000, 3L);

        BDDMockito.given(loadEntryRepository.existsById(any(LoadEntry.LoadEntryId.class))).willReturn(false);
        BDDMockito.given(loadEntryRepository.getEntryAggregateByCustomerIdAndTimeBetween(eq(CUSTOMER_ID), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .willReturn(aggregate);

        ArgumentCaptor<LoadEntry> loadEntryCaptor = ArgumentCaptor.forClass(LoadEntry.class);

        // when
        LoadResult result = velocityLimiter.attemptLoad(request);

        // then
        assertEquals(LOAD_ID, result.id());
        assertEquals(CUSTOMER_ID, result.customerId());
        assertEquals(false, result.accepted());
        verify(loadEntryRepository).save(loadEntryCaptor.capture());
        assertFalse(loadEntryCaptor.getValue().isAccepted());
    }

    @Test
    void shouldRejectWhenDailyAmountLimitExceeded() {
        // given
        LoadOrder request = new LoadOrder(LOAD_ID, CUSTOMER_ID, new Amount(CAD, BigDecimal.ONE), TIME);
        LoadEntryAggregate aggregate = new LoadEntryAggregate(AMOUNT_5000, 1L);

        BDDMockito.given(loadEntryRepository.existsById(any(LoadEntry.LoadEntryId.class))).willReturn(false);
        BDDMockito.given(loadEntryRepository.getEntryAggregateByCustomerIdAndTimeBetween(eq(CUSTOMER_ID), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .willReturn(aggregate);

        ArgumentCaptor<LoadEntry> loadEntryCaptor = ArgumentCaptor.forClass(LoadEntry.class);

        // when
        LoadResult result = velocityLimiter.attemptLoad(request);

        // then
        assertEquals(LOAD_ID, result.id());
        assertEquals(CUSTOMER_ID, result.customerId());
        assertEquals(false, result.accepted());
        verify(loadEntryRepository).save(loadEntryCaptor.capture());
        assertFalse(loadEntryCaptor.getValue().isAccepted());
    }

    @Test
    void shouldRejectWhenWeeklyAmountLimitExceeded() {
        // given
        LoadOrder request = new LoadOrder(LOAD_ID, CUSTOMER_ID, new Amount(CAD, BigDecimal.ONE), TIME);
        LoadEntryAggregate dailyAggregate = new LoadEntryAggregate(AMOUNT_1000, 1L);
        LoadEntryAggregate weeklyAggregate = new LoadEntryAggregate(AMOUNT_20000, 1L);

        BDDMockito.given(loadEntryRepository.existsById(any(LoadEntry.LoadEntryId.class))).willReturn(false);
        BDDMockito.given(loadEntryRepository.getEntryAggregateByCustomerIdAndTimeBetween(eq(CUSTOMER_ID), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .willReturn(dailyAggregate)
                .willReturn(weeklyAggregate);

        ArgumentCaptor<LoadEntry> loadEntryCaptor = ArgumentCaptor.forClass(LoadEntry.class);

        // when
        LoadResult result = velocityLimiter.attemptLoad(request);

        // then
        assertEquals(LOAD_ID, result.id());
        assertEquals(CUSTOMER_ID, result.customerId());
        assertEquals(false, result.accepted());
        verify(loadEntryRepository).save(loadEntryCaptor.capture());
        assertFalse(loadEntryCaptor.getValue().isAccepted());
    }

    @Test
    void shouldRejectUnsupportedCurrency() {
        // given
        LoadOrder request = new LoadOrder(LOAD_ID, CUSTOMER_ID, new Amount(Currency.getInstance("USD"), BigDecimal.ONE), TIME);

        // when
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> velocityLimiter.attemptLoad(request));

        // then
        assertEquals("Unsupported currency: USD", exception.getMessage());
    }

    @Test
    void shouldAcceptLoadRegardlessOfWeeklyCount() {
        // given
        LoadOrder request = new LoadOrder(LOAD_ID, CUSTOMER_ID, new Amount(CAD, BigDecimal.ONE), TIME);
        LoadEntryAggregate dailyAggregate = new LoadEntryAggregate(AMOUNT_1000, 1L);
        LoadEntryAggregate weeklyAggregate = new LoadEntryAggregate(AMOUNT_5000, Integer.valueOf(Integer.MAX_VALUE).longValue());

        BDDMockito.given(loadEntryRepository.existsById(any(LoadEntry.LoadEntryId.class))).willReturn(false);
        BDDMockito.given(loadEntryRepository.getEntryAggregateByCustomerIdAndTimeBetween(eq(CUSTOMER_ID), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .willReturn(dailyAggregate)
                .willReturn(weeklyAggregate);

        // when
        LoadResult result = velocityLimiter.attemptLoad(request);

        // then
        assertEquals(LOAD_ID, result.id());
        assertEquals(CUSTOMER_ID, result.customerId());
        assertEquals(true, result.accepted());
    }
}
