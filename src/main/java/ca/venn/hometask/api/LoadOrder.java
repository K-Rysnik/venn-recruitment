package ca.venn.hometask.api;

import java.time.ZonedDateTime;

public record LoadOrder(Integer id, Integer customerId, Amount loadAmount, ZonedDateTime time) {

}
