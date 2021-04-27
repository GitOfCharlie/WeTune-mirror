package sjtu.ipads.wtune.testbed.profile;

import static sjtu.ipads.wtune.testbed.profile.Profiler.LOG;

import java.lang.System.Logger.Level;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import sjtu.ipads.wtune.common.utils.FuncUtils;
import sjtu.ipads.wtune.sqlparser.relational.Relation;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.util.ASTHelper;
import sjtu.ipads.wtune.stmt.resolver.ParamDesc;
import sjtu.ipads.wtune.stmt.resolver.ParamModifier;
import sjtu.ipads.wtune.testbed.common.Element;
import sjtu.ipads.wtune.testbed.population.Generator;
import sjtu.ipads.wtune.testbed.util.MathHelper;

class ParamGen {
  private final ParamsGen ctx;
  private final ParamDesc param;
  private final Deque<Object> stack;

  ParamGen(ParamsGen ctx, ParamDesc param) {
    this.ctx = ctx;
    this.param = param;
    this.stack = new LinkedList<>();
  }

  boolean generate() {
    stack.clear();

    for (ParamModifier modifier : param.modifiers())
      if (!applyModifier(modifier)) {
        LOG.log(
            Level.ERROR, "cannot apply modifier {0} when generate {1}", modifier, param.index());
        return false;
      }

    return stack.size() == 1;
  }

  Object value() {
    return stack.peekFirst();
  }

  private boolean applyModifier(ParamModifier modifier) {
    final Object[] modifierArgs = modifier.args();
    switch (modifier.type()) {
      case COLUMN_VALUE:
        {
          final Relation relation = (Relation) modifierArgs[0];
          final Column column = (Column) modifierArgs[1];

          final int rowNum = ctx.seedOf(relation);
          final Generator generator = ctx.generators().bind(Element.ofColumn(column));

          final Object obj = generator.generate(rowNum);
          if (obj == null) return false;
          stack.push(obj);
          return true;
        }

      case NEQ:
        {
          final Object top = stack.peekFirst();
          if (top instanceof String) {
            stack.pop();
            stack.push("xxx"); // a value never be generated by wetune
            return true;
          } else if (top instanceof Boolean) {
            stack.pop();
            stack.push(!(Boolean) top);
            return true;
          }
        }
        // otherwise fall through to INCREASE

      case INCREASE:
        {
          final Object top = stack.peekFirst();
          if (top instanceof Number) return applyUnaryArithOp(it -> MathHelper.add(it, 100));
          else if (top instanceof Temporal) {
            return applyUnaryTimeOp(it -> it.plus(1, findSupportedTimeUnit(it)));

          } else return false;
        }
      case DECREASE:
        {
          final Object top = stack.peekFirst();
          if (top instanceof Number) return applyUnaryArithOp(it -> MathHelper.sub(it, 100));
          else if (top instanceof Temporal) {
            return applyUnaryTimeOp(it -> it.minus(1, findSupportedTimeUnit(it)));

          } else return false;
        }
      case INVERSE:
        return applyUnaryArithOp(MathHelper::inverse);

      case ADD:
        return applyBinaryArithOp(MathHelper::add);
      case SUBTRACT:
        return applyBinaryArithOp(MathHelper::sub);
      case TIMES:
        return applyBinaryArithOp(MathHelper::mul);
      case DIVIDE:
        return applyBinaryArithOp(MathHelper::div);

      case LIKE:
        if (modifierArgs.length > 0) {
          assert modifierArgs.length == 2;
          String str = (String) stack.pop();
          if ((boolean) modifierArgs[0]) str = '%' + str;
          if ((boolean) modifierArgs[1]) str = str + '%';
          stack.push(str);
        }
        return true;
      case REGEX:
        {
          final String str = (String) stack.pop();
          stack.push(str + ".*");
          return true;
        }

      case DIRECT_VALUE:
        stack.push(modifierArgs[0]);
        return true;

      case INVOKE_FUNC:
        return applyFunc(modifierArgs);

      case INVOKE_AGG:
        stack.push(1000); // sloppy handling
        return true;

      case MATCHING:
        {
          final Object top = stack.pop();
          assert top instanceof String;
          final String s = (String) top;
          stack.push(((String) top).substring(1, Math.min(4, s.length())));
          return true;
        }
      case CHECK_NULL: // do nothing
        if (stack.pop() == null) stack.push(ParamsGen.IS_NULL);
        else stack.push(ParamsGen.NOT_NULL);
        return true;

      case CHECK_NULL_NOT:
        if (stack.pop() == null) stack.push(ParamsGen.NOT_NULL);
        else stack.push(ParamsGen.IS_NULL);
        return true;

      case CHECK_BOOL_NOT:
        {
          stack.push(!(Boolean) stack.pop());
          return true;
        }

      case CHECK_BOOL: // do nothing
      case TUPLE_ELEMENT: // not longer used
      case ARRAY_ELEMENT: // not longer used
      case MAKE_TUPLE: // not longer used
      case GEN_OFFSET: // not longer used
        return true;

      case KEEP: // shouldn't appear hear
      case GUESS: // shouldn't appear hear
      default:
        return false;
    }
  }

  private boolean applyBinaryArithOp(BiFunction<Number, Number, Number> func) {
    final Object right = stack.pop();
    final Object left = stack.pop();
    assert left instanceof Number && right instanceof Number;

    final Number result = func.apply((Number) left, (Number) right);
    if (result == null) return false;

    stack.push(result);
    return true;
  }

  private boolean applyUnaryArithOp(Function<Number, Number> func) {
    final Object o = stack.pop();
    assert o instanceof Number;

    final Number result = func.apply((Number) o);
    if (result == null) return false;

    stack.push(result);
    return true;
  }

  private boolean applyUnaryTimeOp(Function<Temporal, Temporal> func) {
    final Object top = stack.pop();
    assert top instanceof Temporal;

    stack.push(func.apply((Temporal) top));
    return true;
  }

  private boolean applyFunc(Object[] modifierArgs) {
    final String funcName = ASTHelper.simpleName((String) modifierArgs[0]);
    final int argCount = (Integer) modifierArgs[1];

    final Object[] args = new Object[argCount];
    for (int i = 0; i < argCount; i++) args[i] = stack.pop();

    switch (funcName) {
      case "upper":
        stack.push(((String) args[0]).toUpperCase());
        return true;
      case "lower":
        stack.push(((String) args[0]).toLowerCase());
        return true;
      case "coalesce":
        stack.push(FuncUtils.find(Objects::nonNull, args));
        return true;
      case "string_to_array":
        stack.push(args[0]);
        return true;
      case "length":
        stack.push(((String) args[0]).length());
        return true;
      default:
        return false;
    }
  }

  private ChronoUnit findSupportedTimeUnit(Temporal t) {
    final ChronoUnit[] values = ChronoUnit.values();
    for (int i = ChronoUnit.SECONDS.ordinal(), bound = values.length; i < bound; i++) {
      if (t.isSupported(values[i])) return values[i];
    }
    throw new IllegalArgumentException();
  }
}
