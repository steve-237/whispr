import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.time.ZonedDateTime;

public class TestJson {
    public static void main(String[] args) throws Exception {
        MessageDto dto = new MessageDto(UUID.randomUUID(), "test", "text", "READ", ZonedDateTime.now(), "FR", "Mobile", true, "POSITIVE");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        System.out.println(mapper.writeValueAsString(dto));
    }
}

record MessageDto(
        UUID id,
        String content,
        String type,
        String status,
        ZonedDateTime createdAt,
        String country,
        String deviceHint,
        boolean isRead,
        String aiCategory
) {}
