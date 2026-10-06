package mattonfire.dnd.entity;

/** A dragon whose model is bigger than its hitbox, so it exposes extra hittable DragonParts. */
public interface MultipartDragon {
    DragonPart[] getParts();

    DragonPartLayout getPartLayout();
}
