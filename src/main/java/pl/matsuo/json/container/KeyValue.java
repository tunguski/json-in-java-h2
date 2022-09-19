package pl.matsuo.json.container;

import lombok.Value;

@Value
public class KeyValue {

  String name;
  DataTypes type;
  Object value;
}
