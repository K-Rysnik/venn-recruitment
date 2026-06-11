package ca.venn.hometask;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import ca.venn.hometask.api.LoadOrder;
import ca.venn.hometask.api.VelocityLimiter;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class ConsoleRunner implements ApplicationRunner {

    private final VelocityLimiter velocityLimiter;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path inputFilePath = Path.of(args.containsOption("file") ? args.getOptionValues("file").get(0) : "input.txt");
        if (Files.notExists(inputFilePath, LinkOption.NOFOLLOW_LINKS)) {
            System.err.println("Input file not found: " + inputFilePath);
            return;
        }
        try (var lines = Files.lines(inputFilePath)) {
            lines.forEach(line -> {
                LoadOrder loadOrder = objectMapper.readValue(line, LoadOrder.class);
                try {
                    System.out.println(objectMapper.writeValueAsString(velocityLimiter.attemptLoad(loadOrder)));
                } catch (Exception e) {
                    System.err.println("Error occurred while processing load order: " + e.getMessage());
                }
            });
        } finally {
            System.out.flush();
        }
    }
}
