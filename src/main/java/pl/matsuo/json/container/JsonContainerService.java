package pl.matsuo.json.container;

import static java.lang.Integer.parseInt;
import static pl.matsuo.json.container.DataTypes.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonContainerService {

  StorageService storageService = new StorageService();

  public int putObject(JsonNode json) {
    if (json.isObject()) {
      return putObject((ObjectNode) json);
    }

    throw new RuntimeException("Not an json object");
  }

  int putObject(ObjectNode node) {
    SortedSet<Integer> properties = new TreeSet<>();

    node.fieldNames()
        .forEachRemaining(
            fieldName -> {
              JsonNode fieldValue = node.get(fieldName);
              int idProperty = putProperty(fieldName, fieldValue);
              properties.add(idProperty);
            });

    return maybePut(
        Tables.objects, "properties = ARRAY" + properties, "properties", "ARRAY" + properties);
  }

  int putString(String string) {
    Integer idString = getString(string);
    if (idString != null) {
      return idString;
    }

    storageService.insert(Tables.strings, "text", "'" + string + "'");
    return getString(string);
  }

  Integer getString(String string) {
    return storageService.findId(Tables.strings, "text = '" + string + "'");
  }

  int putProperty(String fieldName, JsonNode fieldValue) {
    int idFieldName = putString(fieldName);

    DataTypes dataType = fixedValueType(fieldValue);

    if (DataTypes.fixedType(dataType)) {
      storageService.insert(
          Tables.fixed_properties, "name, type", "" + idFieldName + ", " + dataType.ordinal());

      return storageService.findId(
          Tables.fixed_properties, "name = " + idFieldName + " and type = " + dataType.ordinal());
    } else if (DataTypes.rawTypes(dataType)) {
      String bytes = toBytes(fieldValue);
      return maybePut(
          Tables.raw_properties,
          "name = "
              + idFieldName
              + " and type = "
              + dataType.ordinal()
              + " and propertyValue = "
              + bytes,
          "name, type, propertyValue",
          "" + idFieldName + ", " + dataType.ordinal() + ", " + bytes);
    } else {
      int idFieldValue = putNode(dataType, fieldValue);

      return maybePut(
          Tables.ref_properties,
          "name = "
              + idFieldName
              + " and type = "
              + dataType.ordinal()
              + " and propertyValue = "
              + idFieldValue,
          "name, type, propertyValue",
          "" + idFieldName + ", " + dataType.ordinal() + ", " + idFieldValue);
    }
  }

  // ARRAY
  // BINARY
  // BOOLEAN
  // MISSING
  // NULL
  // NUMBER
  // OBJECT
  // POJO
  // STRING
  private int putNode(DataTypes dataType, JsonNode fieldValue) {
    if (dataType.equals(dt_string)) {
      return putString(fieldValue.textValue());
    } else if (dataType.equals(dt_object)) {
      return putObject((ObjectNode) fieldValue);
    } else if (dataType.equals(dt_array)) {
      return putArray((ArrayNode) fieldValue);
    } else if (dataType.equals(dt_integer)) {
      return fieldValue.intValue();
    } else if (dataType.equals(dt_float)) {
      return Float.floatToRawIntBits(fieldValue.floatValue());
    } else if (dataType.equals(dt_double)) {
      return putRawValue(dt_double, fieldValue.doubleValue());
    } else {
      throw new RuntimeException("Unknown node type " + dataType);
    }
  }

  int maybePut(Tables table, String selectCondition, String insertFields, String insertValues) {
    Integer id = storageService.findId(table, selectCondition);
    if (id != null) {
      return id;
    }

    storageService.insert(table, insertFields, insertValues);

    return storageService.findId(table, selectCondition);
  }

  private int putRawValue(DataTypes dataType, double doubleValue) {
    String binaryValue = encodeDouble(doubleValue);

    return maybePut(
        Tables.raw_values,
        "type = " + dataType.ordinal() + " and propertyValue = " + binaryValue,
        "type, propertyValue",
        "" + dataType.ordinal() + ", " + binaryValue);
  }

  String toBytes(JsonNode node) {
    if (node.isDouble()) {
      return encodeDouble(node.doubleValue());
    } else if (node.isLong()) {
      return encodeLong(node.longValue());
    }

    throw new RuntimeException("Unknown type " + node.getNodeType().name());
  }

  private static String encodeDouble(double value) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DataOutputStream os = new DataOutputStream(baos);
      os.writeDouble(value);
      return "X'" + org.apache.commons.codec.binary.Hex.encodeHexString(baos.toByteArray()) + "'";
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String encodeLong(long value) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DataOutputStream os = new DataOutputStream(baos);
      os.writeLong(value);
      return "X'" + org.apache.commons.codec.binary.Hex.encodeHexString(baos.toByteArray()) + "'";
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private int putArray(ArrayNode node) {
    List<Integer> values = new ArrayList<>();

    node.elements()
        .forEachRemaining(
            element -> {
              DataTypes dataType = fixedValueType(element);
              int idValue = putValue(dataType, element);
              values.add(idValue);
            });

    return maybePut(Tables.arrays, "valuesArray = ARRAY" + values, "valuesArray", "ARRAY" + values);
  }

  private int putValue(DataTypes dataType, JsonNode element) {
    if (DataTypes.fixedType(dataType)) {
      return maybePut(
          Tables.fixed_values, "type = " + dataType.ordinal(), "type", "" + dataType.ordinal());
    } else {
      int idFieldValue = putNode(dataType, element);

      storageService.insert(
          Tables.ref_values, "type, propertyValue", "" + dataType.ordinal() + ", " + idFieldValue);

      return storageService.findId(
          Tables.ref_values,
          "type = " + dataType.ordinal() + " and propertyValue = " + idFieldValue);
    }
  }

  private DataTypes fixedValueType(JsonNode fieldValue) {
    if (fieldValue.isTextual()) {
      return dt_string;
    } else if (fieldValue.isNull()) {
      return dt_null;
    } else if (fieldValue.isNull()) {
      return dt_null;
    } else if (fieldValue.isBoolean()) {
      return fieldValue.asBoolean() ? dt_true : dt_false;
    } else if (fieldValue.isMissingNode()) {
      return dt_missing;
    } else if (fieldValue.isObject()) {
      return dt_object;
    } else if (fieldValue.isArray()) {
      return dt_array;
    } else if (fieldValue.isInt()) {
      return dt_integer;
    } else if (fieldValue.isDouble()) {
      return dt_double;
    } else {
      throw new RuntimeException(
          "Unrecognized field type " + fieldValue.getNodeType() + " " + fieldValue.textValue());
    }
  }

  public ObjectNode getObject(int id) {
    ResultSet resultSet = storageService.findById(Tables.objects, id, "properties");

    try {
      Array array = resultSet.getArray(1);
      ResultSet arrayElements = array.getResultSet();

      ObjectNode result = JsonNodeFactory.instance.objectNode();

      while (arrayElements.next()) {
        int idValue = arrayElements.getInt(1);
        KeyValue property = getProperty(idValue);

        if (property.getType().equals(dt_string)) {
          result.put(property.getName(), "" + property.getValue());
        } else if (property.getType().equals(dt_null)) {
          result.putNull(property.getName());
        } else if (property.getType().equals(dt_false)) {
          result.put(property.getName(), false);
        } else if (property.getType().equals(dt_true)) {
          result.put(property.getName(), true);
        } else if (property.getType().equals(dt_integer)) {
          result.put(property.getName(), (int) property.getValue());
        } else if (property.getType().equals(dt_float)) {
          result.put(property.getName(), (float) property.getValue());
        } else if (property.getType().equals(dt_long)) {
          result.put(property.getName(), (long) property.getValue());
        } else if (property.getType().equals(dt_double)) {
          result.put(property.getName(), (double) property.getValue());
        } else if (property.getType().equals(dt_array)) {
          result.set(property.getName(), getArray(parseInt("" + property.getValue())));
        } else if (property.getType().equals(dt_object)) {
          result.set(property.getName(), getObject(parseInt("" + property.getValue())));
        } else {
          throw new RuntimeException("Unrecognized value " + property);
        }
      }

      return result;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public ArrayNode getArray(int id) {
    ResultSet resultSet = storageService.findById(Tables.arrays, id, "properties");

    try {
      Array array = resultSet.getArray(1);
      ResultSet arrayElements = array.getResultSet();

      ArrayNode result = JsonNodeFactory.instance.arrayNode();

      while (arrayElements.next()) {
        int idValue = arrayElements.getInt(1);
        Value property = getValue(idValue);

        if (property.getType().equals(dt_string)) {
          result.add("" + property.getValue());
        } else if (property.getType().equals(dt_null)) {
          result.addNull();
        } else if (property.getType().equals(dt_false)) {
          result.add(false);
        } else if (property.getType().equals(dt_true)) {
          result.add(true);
        } else if (property.getType().equals(dt_integer)) {
          result.add(parseInt("" + property.getValue()));
        } else if (property.getType().equals(dt_array)) {
          result.add(getArray(parseInt("" + property.getValue())));
        } else if (property.getType().equals(dt_object)) {
          result.add(getObject(parseInt("" + property.getValue())));
        } else {
          throw new RuntimeException("Unrecognized value " + property);
        }
      }

      return result;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private Value getValue(int idValue) {
    ResultSet property = storageService.findById(Tables.ref_values, idValue, "*");

    if (property != null) {
      try {
        DataTypes type = DataTypes.values()[property.getByte(3)];
        Object value = getTypedValue(type, property.getInt(4));
        return new Value(type, value);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    property = storageService.findById(Tables.fixed_values, idValue, "*");

    if (property != null) {
      try {
        DataTypes type = DataTypes.values()[property.getByte(3)];
        return new Value(type, null);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    throw new RuntimeException("Could not find value with id " + idValue);
  }

  private KeyValue getProperty(int idProperty) {
    ResultSet property = storageService.findById(Tables.fixed_properties, idProperty, "*");

    if (property != null) {
      try {
        String key = getString(property.getInt(2));
        DataTypes type = DataTypes.values()[property.getByte(3)];
        return new KeyValue(key, type, null);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    property = storageService.findById(Tables.ref_properties, idProperty, "*");

    if (property != null) {
      try {
        String key = getString(property.getInt(2));
        DataTypes type = DataTypes.values()[property.getByte(3)];
        Object value = getTypedValue(type, property.getInt(4));
        return new KeyValue(key, type, value);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    property = storageService.findById(Tables.raw_properties, idProperty, "*");

    if (property != null) {
      try {
        String key = getString(property.getInt(2));
        DataTypes type = DataTypes.values()[property.getByte(3)];
        Object value = getTypedValue(type, property.getLong(4));
        return new KeyValue(key, type, value);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    throw new RuntimeException("Could not find property with id " + idProperty);
  }

  private Object getTypedValue(DataTypes type, long value) {
    if (type.equals(dt_long)) {
      return value;
    } else if (type.equals(dt_double)) {
      return Double.longBitsToDouble(value);
    }

    throw new RuntimeException("Unrecognized type " + type.name());
  }

  private Object getTypedValue(DataTypes type, int id) {
    if (type.equals(dt_string)) {
      return getString(id);
    } else if (type.equals(dt_integer)) {
      return id;
    } else if (type.equals(dt_float)) {
      return Float.intBitsToFloat(id);
    }

    throw new RuntimeException("Unrecognized type " + type.name());
  }

  private String getString(int id) {
    ResultSet text = storageService.findById(Tables.strings, id, "text");
    try {
      return text.getString(1);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
