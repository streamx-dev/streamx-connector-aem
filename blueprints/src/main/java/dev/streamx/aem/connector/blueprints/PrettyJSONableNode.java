package dev.streamx.aem.connector.blueprints;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.jackrabbit.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PrettyJSONableNode {

  private static final Logger LOG = LoggerFactory.getLogger(PrettyJSONableNode.class);

  private final JSONableNode jsoNableNode;

  PrettyJSONableNode(JSONableNode jsoNableNode) {
    this.jsoNableNode = jsoNableNode;
  }

  String json() {
    return cleanJson(jsoNableNode.json());
  }

  private String cleanJson(String jsonString) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      JsonNode root = mapper.readTree(jsonString);
      return removeUnwantedNodes(root)
          .map(
              node -> {
                try {
                  String cleanedJSON = mapper.writeValueAsString(node);
                  LOG.trace("Cleaned JSON: {}", cleanedJSON);
                  return cleanedJSON;
                } catch (JsonProcessingException exception) {
                  LOG.error("Cannot write JSON", exception);
                  return "{ }";
                }
              }
          ).orElse("{ }");
    } catch (IOException e) {
      return "{ }";
    }
  }

  /**
   * Recursively removes nodes that have jcr:* (except jcr:content) or cq:*, returning
   * Optional.empty() whenever the entire node should be removed.
   */
  @SuppressWarnings({
      "squid:S3776", "MethodWithMultipleLoops", "MethodWithMultipleReturnPoints",
      "OverlyComplexMethod", "OverlyNestedMethod", "ReassignedVariable", "squid:S127"
  })
  private Optional<JsonNode> removeUnwantedNodes(JsonNode node) {
    if (node.isObject()) {
      ObjectNode objectNode = (ObjectNode) node;
      @SuppressWarnings("TypeMayBeWeakened")
      List<String> fieldsToRemove = new ArrayList<>();

      Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        String fieldName = entry.getKey();
        JsonNode childNode = entry.getValue();

        // If field is "jcr:*" but not "jcr:content", or "cq:*", remove it
        if (
            (fieldName.startsWith("jcr:") && !fieldName.equals(JcrConstants.JCR_CONTENT))
                || fieldName.startsWith("cq:")
        ) {
          fieldsToRemove.add(fieldName);
        } else {
          // Recursively process child node
          Optional<JsonNode> cleanedChild = removeUnwantedNodes(childNode);
          if (cleanedChild.isEmpty()) {
            // If cleanedChild is not present, remove the field
            fieldsToRemove.add(fieldName);
          } else {
            // Otherwise replace the child with its cleaned version
            objectNode.set(fieldName, cleanedChild.get());
          }
        }
      }

      // Remove fields collected
      for (String fieldToRemove : fieldsToRemove) {
        objectNode.remove(fieldToRemove);
      }

      // If no fields remain, return empty
      return objectNode.size() == 0 ? Optional.empty() : Optional.of(objectNode);

    } else if (node.isArray()) {
      ArrayNode arrayNode = (ArrayNode) node;
      // We'll iterate using a manual index so we can remove while iterating
      //noinspection MethodCallInLoopCondition
      for (int i = 0; i < arrayNode.size(); ) {
        JsonNode childNode = arrayNode.get(i);
        Optional<JsonNode> cleanedChild = removeUnwantedNodes(childNode);
        if (cleanedChild.isEmpty()) {
          arrayNode.remove(i);
        } else {
          arrayNode.set(i, cleanedChild.get());
          i++;
        }
      }

      // If the array node has no elements left, treat it as empty
      return arrayNode.size() == 0 ? Optional.empty() : Optional.of(arrayNode);

    } else {
      // Value node: keep as-is
      return Optional.of(node);
    }
  }
}
