package pl.matsuo.json.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

class JsonContainerServiceTest {

  public static final String TEST_JSON = // "{\"prop\":\"value\",\"key\":\"smile\"}";
      """
             {
               "prop" : "value",
               "key" : "smile",
               "true" : true,
               "false" : false,
               "null" : null,
               "integer" : 7,
               "float" : 1.3
             }""";
  JsonContainerService service = new JsonContainerService();
  ObjectMapper mapper = new ObjectMapper();

  @Test
  void putAndGetObject() throws JsonProcessingException {
    JsonNode jsonNode = mapper.readTree(TEST_JSON);
    String expected = jsonNode.toPrettyString();
    int id = service.putObject(jsonNode);
    assertEquals(1, id);

    JsonNode object = service.getObject(id);
    assertNotNull(object);
    assertEquals(expected, object.toPrettyString());
  }

  @Test
  void generate1000RandomJsonFiles() throws IOException {
    int id1 =
        service.putObject(
            mapper.readTree(
                IOUtils.resourceToString("/json/sample1.json", StandardCharsets.UTF_8)));
    int id2 =
        service.putObject(
            mapper.readTree(
                IOUtils.resourceToString("/json/sample2.json", StandardCharsets.UTF_8)));
    int id3 =
        service.putObject(
            mapper.readTree(
                IOUtils.resourceToString("/json/sample3.json", StandardCharsets.UTF_8)));
    int id4 =
        service.putObject(
            mapper.readTree(
                IOUtils.resourceToString("/json/sample4.json", StandardCharsets.UTF_8)));
  }
}
