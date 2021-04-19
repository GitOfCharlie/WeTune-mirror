package sjtu.ipads.wtune.testbed.population;

import sjtu.ipads.wtune.testbed.common.Collection;

public interface Populatable {
  boolean bindGen(Generators generators);

  boolean populateOne(Actuator actuator);

  static Populatable ofCollection(Collection collection) {
    return new BasePopulatable(collection);
  }
}
