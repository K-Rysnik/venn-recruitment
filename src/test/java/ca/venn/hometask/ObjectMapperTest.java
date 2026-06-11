package ca.venn.hometask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ca.venn.hometask.api.LoadOrder;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
public class ObjectMapperTest {
    private final String JSON_PAYLOAD = "{\"id\":\"1\",\"customer_id\":\"1\",\"load_amount\":\"$0.99\",\"time\":\"2000-01-01T00:00:00Z\"}";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void shouldDeserializeCustomAmountType () {
        //given

        //when
        LoadOrder result = objectMapper.readValue(JSON_PAYLOAD, LoadOrder.class);

        //then
        assertNotNull(result);
        assertNotNull(result.loadAmount());
        assertEquals(Currency.getInstance("CAD"), result.loadAmount().currency());
        assertEquals(new BigDecimal("0.99"), result.loadAmount().value());
    }

    @Test
    public void shouldThrowExceptionOnUnknownCurrency () {
        //given
        String faultyCurrency = JSON_PAYLOAD.replace("$", "€");

        //when
        DatabindException exception = assertThrows(DatabindException.class, ()->objectMapper.readValue(faultyCurrency, LoadOrder.class));

        //then
        assertEquals(UnsupportedOperationException.class, exception.getCause().getClass());
        assertTrue(exception.getMessage().contains("Unsupported amount format"));
    }

    @Test
    public void shouldThrowExceptionOnMalformedAmount () {
        //given
        String faultyCurrency = JSON_PAYLOAD.replace("$0.99", "0.12");

        //when
        DatabindException exception = assertThrows(DatabindException.class, ()->objectMapper.readValue(faultyCurrency, LoadOrder.class));

        //then
        assertEquals(UnsupportedOperationException.class, exception.getCause().getClass());
        assertTrue(exception.getMessage().contains("Unsupported amount format"));
    }

        @Test
        public void shouldThrowExceptionOnMalformedAmountUsingComa () {
        //given
        String faultyCurrency = JSON_PAYLOAD.replace("$0.99", "$0,99");

        //when
        DatabindException exception = assertThrows(DatabindException.class, ()->objectMapper.readValue(faultyCurrency, LoadOrder.class));

        //then
        assertEquals(UnsupportedOperationException.class, exception.getCause().getClass());
        assertTrue(exception.getMessage().contains("Unsupported amount format"));
    }
}
