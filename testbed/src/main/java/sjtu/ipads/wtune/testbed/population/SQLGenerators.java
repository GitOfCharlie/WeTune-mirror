package sjtu.ipads.wtune.testbed.population;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static sjtu.ipads.wtune.sqlparser.ast.constants.DataTypeName.DECIMAL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.DataTypeName.FIXED;
import static sjtu.ipads.wtune.sqlparser.ast.constants.DataTypeName.NUMERIC;
import static sjtu.ipads.wtune.testbed.util.MathHelper.base10;
import static sjtu.ipads.wtune.testbed.util.MathHelper.pow10;
import static sjtu.ipads.wtune.testbed.util.RandomHelper.GLOBAL_SEED;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Column.Flag;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;
import sjtu.ipads.wtune.testbed.common.BatchActuator;
import sjtu.ipads.wtune.testbed.common.Element;

class SQLGenerators implements Generators {
  private final PopulationConfig config;
  private final Map<Element, Generator> generators;

  SQLGenerators(PopulationConfig config) {
    this.config = config;
    this.generators = new HashMap<>();
  }

  @Override
  public PopulationConfig config() {
    return config;
  }

  @Override
  public Generator bind(Element element) {
    return generators.computeIfAbsent(element, this::makeGenerator);
  }

  private Generator makeGenerator(Element element) {
    final Column column = element.unwrap(Column.class);

    if (column.isFlag(Flag.AUTO_INCREMENT)) return new AutoIncrementModifier();

    final Column referred = getReferencedColumn(column);
    final Constraint uk = getUniqueKey(column);

    Generator gen;
    if (referred == null) gen = Converter.makeConverter(column);
    else {
      gen = makeGenerator(Element.ofColumn(referred));
      gen = new ForeignKeyModifier(gen, config.unitCountOf(referred.tableName()));
    }

    final String tableName = column.tableName();
    final String colName = column.name();

    if (uk == null) {
      // non-unique column, just use a random gen
      gen = new RandomModifier(column, config.randomGenOf(tableName, colName), gen);

    } else {
      final UniqueKeyDist dist = new UniqueKeyDist(uk.columns());
      final int colIdx = uk.columns().indexOf(column);
      gen = dist.generatorOf(colIdx, gen);
    }

    if (config.needPrePopulation() && !gen.isPrePopulated()) {
      prePopulate(gen, config.unitCountOf(element.collectionName()));
    }

    return gen;
  }

  private static Column getReferencedColumn(Column column) {
    final Collection<Constraint> fks = column.constraints(ConstraintType.FOREIGN);
    if (fks.isEmpty()) return null;

    final Constraint fk = Iterables.get(fks, 0);
    return fk.refColumns().get(fk.columns().indexOf(column));
  }

  private static Constraint getUniqueKey(Column column) {
    final Collection<Constraint> pks = column.constraints(ConstraintType.PRIMARY);
    final Constraint pk = pks.isEmpty() ? null : Iterables.get(pks, 0);

    Constraint shortestUk = pk;
    final Collection<Constraint> uks = column.constraints(ConstraintType.UNIQUE);
    for (Constraint uk : uks)
      if (shortestUk == null || uk.columns().size() < shortestUk.columns().size()) shortestUk = uk;

    return shortestUk;
  }

  private void prePopulate(Generator gen, int count) {
    final BatchActuator noOp = () -> 0;

    for (int i = 0; i < count; i++) gen.generate(i, noOp);
  }

  private class UniqueKeyDist {
    private final int unitCount;
    private final List<? extends Column> columns;
    private final int[] digitsDist;
    private final int totalDigits;
    private final int sharedSeed;

    private UniqueKeyDist(List<? extends Column> columns) {
      assert !columns.isEmpty();

      this.unitCount = config.unitCountOf(columns.get(0).tableName());
      this.columns = columns;
      this.digitsDist = new int[columns.size()];
      this.totalDigits = distributeDigits();

      final String ukStr = columns.stream().map(Object::toString).collect(Collectors.joining());
      this.sharedSeed = GLOBAL_SEED + ukStr.hashCode();
    }

    private int distributeDigits() {
      final int[] digitsDist = this.digitsDist;

      int totalDigits = 0;
      for (int i = 0, bound = columns.size(); i < bound; i++) {
        final Column column = columns.get(i);
        totalDigits += digitsDist[i] = getRequiredDigits(column, unitCount);
      }

      final int cardinality = pow10(Math.min(9, totalDigits));
      if (cardinality < unitCount) {
        throw new IllegalArgumentException(
            "the units (%d) is too large to enforce the uniqueness on %s"
                .formatted(unitCount, columns));
      }

      return shrinkDigits(base10(unitCount), totalDigits);
    }

    private int shrinkDigits(int requiredDigits, int providedDigits) {
      if (requiredDigits >= providedDigits) return requiredDigits;

      final int[] digitsDist = this.digitsDist;
      for (int i = 0, bound = columns.size(); i < bound; i++) {
        final Column column = columns.get(i);
        if (column.dataType().category() != Category.STRING || digitsDist[i] <= 4) continue;

        final int shrunk = providedDigits - digitsDist[i] + 4;
        if (shrunk < requiredDigits) break;
        else {
          providedDigits = shrunk;
          digitsDist[i] = 4;
        }
      }

      return providedDigits;
    }

    private Generator generatorOf(int index, Generator nextStep) {
      final int start0 = startDigitOf(index);
      final int end0 = start0 + digitsDist[index];
      final int[] adjusted = adjustRange(totalDigits, start0, end0);
      final int totalDigits1 = adjusted[0], start = adjusted[1], end = adjusted[2];

      if (pow10(totalDigits1) >= unitCount) {
        return new UniqueKeyModifier(sharedSeed, totalDigits1, start, end, nextStep);
      } else {
        // If the digits is too few for the required row count, then just use a random gen.
        // For example, a unique key <Int(FK),Bool>, the Int column is bound to some digits, thus
        // leave the Bool bound to a single bit. Obviously it is impossible to enforce the uniques
        // for that poor little bit for any row count > 2. In such case, the Int column itself
        // will
        // ensure the uniqueness.
        final Column col = columns.get(index);
        return new RandomModifier(col, config.randomGenOf(col.tableName(), col.name()), nextStep);
      }
    }

    private int startDigitOf(int index) {
      final int[] digitsDist = this.digitsDist;
      int start = 0;
      for (int i = 0; i < index; i++) if (digitsDist[i] >= 0) start += digitsDist[i];
      return start;
    }

    private int getRequiredDigits(Column column, int unitCount) {
      final Column referenced = getReferencedColumn(column);
      if (referenced != null) return base10(config.unitCountOf(referenced.tableName()));

      if (column.isFlag(Flag.IS_BOOLEAN)) return 1;
      if (column.isFlag(Flag.IS_ENUM)) return 1;

      final int cardinalityRequirement = base10(unitCount);

      final SQLDataType dataType = column.dataType();
      final int storageRequirement;
      switch (dataType.category()) {
        case INTEGRAL:
          if (dataType.storageSize() <= 3) storageRequirement = dataType.storageSize() << 1;
          else storageRequirement = 9;
          break;

        case BOOLEAN:
        case ENUM:
          storageRequirement = 1;
          break;

        case FRACTION:
          if (FIXED.equals(dataType.name())
              || NUMERIC.equals(dataType.name())
              || DECIMAL.equals(dataType.name()))
            storageRequirement = dataType.width() - dataType.precision();
          else storageRequirement = 9;
          break;

        case STRING:
          storageRequirement = dataType.width() == -1 ? 9 : dataType.width();
          break;

        default:
          storageRequirement = 9;
          break;
      }

      return Math.min(storageRequirement, cardinalityRequirement);
    }

    private static int[] adjustRange(int range, int start, int end) {
      if (range <= 9) return new int[] {range, start, end};

      final int width = min(end - start, 9);
      final int rightPadding = range - end;
      final int adjustedRightPadding = (int) (rightPadding * (9 / (double) range));

      range = 9;
      start = max(0, range - adjustedRightPadding - width);
      end = min(range, start + width);

      return new int[] {range, start, end};
    }
  }
}
