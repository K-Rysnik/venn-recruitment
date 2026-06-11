package ca.venn.hometask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Scanner;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ca.venn.hometask.api.LoadOrder;
import ca.venn.hometask.api.LoadResult;
import ca.venn.hometask.api.VelocityLimiter;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@Slf4j
public class HometaskApplicationIT {

    @Autowired
    private VelocityLimiter velocityLimiter;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
	void shouldProcessSampleFile() {
		// given
		try (Scanner scanner = new Scanner(getClass().getResourceAsStream("/Venn - Back-End - Input.txt")); Scanner expectedOutputScanner = new Scanner(getClass().getResourceAsStream("/Venn - Back-End - Output .txt"))) {
			// when
			scanner.forEachRemaining(line-> {
                LoadOrder loadOrder = objectMapper.readValue(line, LoadOrder.class);
                try {
                    LoadResult loadResult = velocityLimiter.attemptLoad(loadOrder);
                    LoadResult expectedLoadResult = objectMapper.readValue(expectedOutputScanner.nextLine(), LoadResult.class);

                    // then
                    assertEquals(expectedLoadResult, loadResult);
                } catch (Exception e) {
                    log.error("Error occurred while processing load order: {}", e.getMessage());
                    if (e instanceof IllegalStateException && e.getMessage().contains("already exists")) {
                        // If the exception is due to duplicate load order, we can skip the assertion for that line
                        log.warn("Skipping assertion for duplicate load order: {}", line);
                    } else {
                        // For other exceptions, we should fail the test
                        fail("Exception thrown during processing: " + e.getMessage());
                    }
                }
            });
		}
	}
}
