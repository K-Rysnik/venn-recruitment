package ca.venn.hometask.service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import ca.venn.hometask.api.LoadOrder;
import ca.venn.hometask.api.LoadResult;
import ca.venn.hometask.api.VelocityLimiter;
import ca.venn.hometask.model.LoadEntry;
import ca.venn.hometask.model.LoadEntryAggregate;
import ca.venn.hometask.model.LoadEntryRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class VelocityLimiterImpl implements VelocityLimiter {
    private final LoadEntryRepository loadEntryRepository;
    private static final int DAILY_LOAD_COUNT_LIMIT = 3;
    private static final BigDecimal DAILY_LOAD_LIMIT = new BigDecimal("5000");
    private static final BigDecimal WEEKLY_LOAD_LIMIT = new BigDecimal("20000");

    @Transactional(isolation = Isolation.REPEATABLE_READ)//Isolation level is only necessary if we allow concurrent load attempts
    @Override
    public LoadResult attemptLoad(LoadOrder loadOrder) {
        validateExistence(loadOrder);
        validateLimits(loadOrder);
        
        loadEntryRepository.save(new LoadEntry(
                new LoadEntry.LoadEntryId(loadOrder.id(), loadOrder.customerId()),
                loadOrder.loadAmount().value(),
                loadOrder.time()
        ));

        return new LoadResult(loadOrder.id(), loadOrder.customerId(), true);
    }

    private void validateExistence(LoadOrder loadOrder) {
        if (loadEntryRepository.existsById(new LoadEntry.LoadEntryId(loadOrder.id(), loadOrder.customerId()))){
            throw new IllegalStateException("Load order with id %d for customer %d already exists".formatted(loadOrder.id(), loadOrder.customerId()));
        }
    }
    
    private void validateLimits(LoadOrder loadOrder) {
        validateDailyLimits(loadOrder);
        validateWeeklyLimits(loadOrder);
    }

    private void validateDailyLimits(LoadOrder loadOrder) {
        ZonedDateTime startOfDay = loadOrder.time().toLocalDate().atStartOfDay(loadOrder.time().getZone());
        LoadEntryAggregate aggregate = loadEntryRepository.getEntryAggregateByCustomerIdAndTimeBetween(loadOrder.customerId(), startOfDay, startOfDay.plusDays(1));
        if (aggregate.loadCount() >= DAILY_LOAD_COUNT_LIMIT) {
            throw new IllegalStateException("Customer %d has already reached the maximum number of loads for the day".formatted(loadOrder.customerId()));
        }
        if (aggregate.totalAmount().add(loadOrder.loadAmount().value()).compareTo(DAILY_LOAD_LIMIT) > 0) {
            throw new IllegalStateException("Customer %d has already reached the maximum total load amount for the day".formatted(loadOrder.customerId()));
        }
    }

    private void validateWeeklyLimits(LoadOrder loadOrder) {
        ZonedDateTime startOfWeek = loadOrder.time().toLocalDate().with(java.time.DayOfWeek.MONDAY).atStartOfDay(loadOrder.time().getZone());
        LoadEntryAggregate aggregate = loadEntryRepository.getEntryAggregateByCustomerIdAndTimeBetween(loadOrder.customerId(), startOfWeek, startOfWeek.plusWeeks(1));
        if (aggregate.totalAmount().add(loadOrder.loadAmount().value()).compareTo(WEEKLY_LOAD_LIMIT) > 0) {
            throw new IllegalStateException("Customer %d has already reached the maximum total load amount for the week".formatted(loadOrder.customerId()));
        }
    }
}
