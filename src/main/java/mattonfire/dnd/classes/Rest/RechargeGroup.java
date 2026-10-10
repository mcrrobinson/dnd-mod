package mattonfire.dnd.classes.Rest;

/**
 * How a class gets its charges back: {@link #SHORT} classes refill on any rest,
 * {@link #LONG} classes get only one charge back from a short rest but have more
 * charges.
 */
public enum RechargeGroup {
    SHORT, LONG;

    public String label() {
        return this == SHORT ? "short rest" : "long rest";
    }
}
