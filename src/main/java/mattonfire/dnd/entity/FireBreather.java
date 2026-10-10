package mattonfire.dnd.entity;

/** A dragon that breathes fire */
public interface FireBreather {
    FireBreath getFireBreath();

    /** The DEX save DC against its breath (see {@link DragonSaves}). */
    int getBreathSaveDc();
}
