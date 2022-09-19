package pl.matsuo.json.container;

import java.util.List;

public enum DataTypes {
  dt_string,
  dt_null,
  dt_true,
  dt_false,
  dt_missing,
  dt_integer,
  dt_float,
  dt_object,
  dt_array,
  dt_double,
  dt_long;

  public static List<DataTypes> fixedTypes = List.of(dt_null, dt_true, dt_false, dt_missing);
  public static List<DataTypes> rawTypes = List.of(dt_double, dt_long);

  public static boolean fixedType(DataTypes dataType) {
    return fixedTypes.contains(dataType);
  }

  public static boolean rawTypes(DataTypes dataType) {
    return rawTypes.contains(dataType);
  }
}
