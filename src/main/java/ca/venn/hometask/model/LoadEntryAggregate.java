package ca.venn.hometask.model;

import java.math.BigDecimal;

public record LoadEntryAggregate(BigDecimal totalAmount, Long loadCount) {}