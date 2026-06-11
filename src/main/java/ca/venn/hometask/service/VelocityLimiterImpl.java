package ca.venn.hometask.service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import ca.venn.hometask.api.Amount;
import ca.venn.hometask.api.LoadOrder;
import ca.venn.hometask.api.LoadResult;
import ca.venn.hometask.api.VelocityLimiter;
import ca.venn.hometask.model.LoadEntry;
import ca.venn.hometask.model.LoadEntryAggregate;
import ca.venn.hometask.model.LoadEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class VelocityLimiterImpl implements VelocityLimiter {
    private final LoadEntryRepository loadEntryRepository;
    private static final int DAILY_LOAD_COUNT_LIMIT = 3;
    private static final BigDecimal DAILY_LOAD_LIMIT = new BigDecimal("5000");
    private static final BigDecimal WEEKLY_LOAD_LIMIT = new BigDecimal("20000");
    private static final ZoneId UTC_ZONE = ZoneId.of("UTC").normalized();

    @Transactional(isolation = Isolation.SERIALIZABLE)// To avoid phantom reads. Isolation level is only necessary if we allow concurrent load attempts
    @Override
    public LoadResult attemptLoad(LoadOrder loadOrder) {
        // Call conversion early to trigger exchange rate validation before any database calls
        BigDecimal amountInCAD = convertToCAD(loadOrder.loadAmount());
        validateExistence(loadOrder);
        boolean validationResult = validateLimits(loadOrder, amountInCAD);
        
        loadEntryRepository.save(new LoadEntry(
                new LoadEntry.LoadEntryId(loadOrder.id(), loadOrder.customerId()),
                amountInCAD,
                loadOrder.time(),
                validationResult
        ));

        return new LoadResult(loadOrder.id(), loadOrder.customerId(), validationResult);
    }

    private BigDecimal convertToCAD(Amount amount) {
        switch (amount.currency().getCurrencyCode()) {
            case "CAD":
                return amount.value();
            default:
                log.error("No conversion defined for: {}", amount.currency().getCurrencyCode());
                throw new UnsupportedOperationException("Unsupported currency: " + amount.currency().getCurrencyCode());
        }
    }

    private void validateExistence(LoadOrder loadOrder) {
        if (loadEntryRepository.existsById(new LoadEntry.LoadEntryId(loadOrder.id(), loadOrder.customerId()))){
            throw new IllegalStateException("Load order with id %d for customer %d already exists".formatted(loadOrder.id(), loadOrder.customerId()));
        }
    }
    
    private boolean validateLimits(LoadOrder loadOrder, BigDecimal amountInCAD) {
        return validateDailyLimits(loadOrder, amountInCAD) && validateWeeklyLimits(loadOrder, amountInCAD);
    }

    private boolean validateDailyLimits(LoadOrder loadOrder, BigDecimal amountInCAD) {
        ZonedDateTime startOfDay = loadOrder.time().withZoneSameInstant(UTC_ZONE).toLocalDate().atStartOfDay(UTC_ZONE);
        LoadEntryAggregate aggregate = loadEntryRepository.getEntryAggregateByCustomerIdAndTimeBetween(loadOrder.customerId(), startOfDay, startOfDay.plusDays(1));
        if (aggregate.loadCount() >= DAILY_LOAD_COUNT_LIMIT) {
            log.debug("Customer {} exceeded daily limit of load count. Limit {}, processed loads over the day {}", loadOrder.customerId(), DAILY_LOAD_COUNT_LIMIT, aggregate.loadCount());
            return false;
        }

        if (aggregate.totalAmount().add(amountInCAD).compareTo(DAILY_LOAD_LIMIT) > 0) {
            log.debug("Customer {} exceeded daily limit of loads. Limit {}, loads over the day {}, request {}{} in CAD: {}", loadOrder.customerId(), DAILY_LOAD_LIMIT, aggregate.totalAmount(), loadOrder.loadAmount().value(), loadOrder.loadAmount().currency().getCurrencyCode(), amountInCAD);
            return false;
        }
        return true;
    }

    private boolean validateWeeklyLimits(LoadOrder loadOrder, BigDecimal amountInCAD) {
        ZonedDateTime startOfWeek = loadOrder.time().withZoneSameInstant(UTC_ZONE).toLocalDate().with(java.time.DayOfWeek.MONDAY).atStartOfDay(UTC_ZONE);
        LoadEntryAggregate aggregate = loadEntryRepository.getEntryAggregateByCustomerIdAndTimeBetween(loadOrder.customerId(), startOfWeek, startOfWeek.plusWeeks(1));
        if (aggregate.totalAmount().add(amountInCAD).compareTo(WEEKLY_LOAD_LIMIT) > 0) {
            log.debug("Customer {} exceeded weekly limit of loads. Limit {}, loads over the week {}, request {}{} in CAD: {}", loadOrder.customerId(), WEEKLY_LOAD_LIMIT, aggregate.totalAmount(), loadOrder.loadAmount().value(), loadOrder.loadAmount().currency().getCurrencyCode(), amountInCAD);
            return false;
        }
        return true;
    }
}
