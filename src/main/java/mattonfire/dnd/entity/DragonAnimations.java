package mattonfire.dnd.entity;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.loader.api.FabricLoader;

/**
 * A server-side reader for a model's .animation.json, so dragon parts can follow the animation the
 * client is playing. It evaluates keyframes the way GeckoLib 4.2 does (BakedAnimationsAdapter and
 * AnimationController): linear interpolation, pre/post keyframes, rotations in radians with x and y
 * flipped, and Molang values evaluated at the current animation time with degree-based sin/cos.
 * <p>
 * It has its own small Molang evaluator instead of GeckoLib's MolangParser, which is a global
 * singleton the client render thread writes query.anim_time into.
 */
public final class DragonAnimations {
    /** A keyframe's three axes, eased linearly from {@code start} to {@code end} over {@code length} ticks. */
    record Keyframe(double length, Expr[] start, Expr[] end) {
    }

    /** Animated rotation (radians, GeckoLib's axes), position (pixels) and scale of one bone. */
    record BoneAnimation(Keyframe[] rotation, Keyframe[] position, Keyframe[] scale) {
        /** Fills {@code out} with the channel's three axes at {@code tick}; false if the bone has no such channel. */
        boolean sample(Keyframe[] channel, double tick, double[] out) {
            if (channel == null) {
                return false;
            }
            for (int axis = 0; axis < 3; axis++) {
                out[axis] = DragonAnimations.sample(channel, axis, tick);
            }
            return true;
        }
    }

    /** {@code length} in ticks, or {@link Double#MAX_VALUE} if the animation never ends on its own. */
    record Animation(String name, double length, boolean loop, Map<String, BoneAnimation> bones) {
        /** The animation time (ticks) at world time {@code time}, the same phase DragonAnimationController gives the client. */
        public double phase(double time) {
            if (this.length >= Double.MAX_VALUE / 2) {
                return Math.max(time, 0);
            }
            if (!this.loop) {
                return Math.min(Math.max(time, 0), this.length);
            }
            double phase = time % this.length;
            return phase < 0 ? phase + this.length : phase;
        }
    }

    private final Map<String, Animation> animations;

    private DragonAnimations(Map<String, Animation> animations) {
        this.animations = animations;
    }

    public Animation get(String name) {
        return this.animations.get(name);
    }

    /** Reads {@code names} from assets/dndclasses/animations/{@code model}.animation.json; missing ones are skipped. */
    public static DragonAnimations load(String model, String... names) {
        Map<String, Animation> animations = new HashMap<>();
        String path = "assets/" + DnDClasses.MOD_ID + "/animations/" + model + ".animation.json";
        Optional<Path> file = FabricLoader.getInstance().getModContainer(DnDClasses.MOD_ID).flatMap(mod -> mod.findPath(path));
        if (file.isEmpty()) {
            DnDClasses.LOGGER.error("Missing dragon animations {}", path);
            return new DragonAnimations(animations);
        }
        try (Reader reader = Files.newBufferedReader(file.get())) {
            JsonObject all = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
            for (String name : names) {
                if (all.has(name)) {
                    animations.put(name, readAnimation(name, all.getAsJsonObject(name)));
                } else {
                    DnDClasses.LOGGER.warn("No animation {} in {}", name, path);
                }
            }
        } catch (Exception e) {
            DnDClasses.LOGGER.error("Couldn't read dragon animations {}", path, e);
        }
        return new DragonAnimations(animations);
    }

    private static Animation readAnimation(String name, JsonObject json) {
        Map<String, BoneAnimation> bones = new HashMap<>();
        double longest = 0;
        if (json.has("bones")) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("bones").entrySet()) {
                JsonObject bone = entry.getValue().getAsJsonObject();
                Keyframe[] rotation = readChannel(bone.get("rotation"), true);
                Keyframe[] position = readChannel(bone.get("position"), false);
                Keyframe[] scale = readChannel(bone.get("scale"), false);
                longest = Math.max(longest, Math.max(totalLength(rotation), Math.max(totalLength(position), totalLength(scale))));
                bones.put(entry.getKey(), new BoneAnimation(rotation, position, scale));
            }
        }
        double length = json.has("animation_length") ? json.get("animation_length").getAsDouble() * 20 : (longest == 0 ? Double.MAX_VALUE : longest);
        JsonElement loop = json.get("loop");
        boolean loops = loop != null && loop.isJsonPrimitive()
                && (loop.getAsJsonPrimitive().isBoolean() ? loop.getAsBoolean() : "loop".equals(loop.getAsString()));
        return new Animation(name, length, loops, bones);
    }

    private static double totalLength(Keyframe[] frames) {
        double total = 0;
        if (frames != null) {
            for (Keyframe frame : frames) {
                total += frame.length();
            }
        }
        return total;
    }

    /** Like BakedAnimationsAdapter.getTripletObj + buildKeyframeStack. */
    private static Keyframe[] readChannel(JsonElement element, boolean rotation) {
        if (element == null) {
            return null;
        }
        List<Map.Entry<Double, JsonArray>> entries = new ArrayList<>();
        if (element instanceof JsonPrimitive primitive) {
            JsonArray array = new JsonArray();
            array.add(primitive);
            array.add(primitive);
            array.add(primitive);
            entries.add(Map.entry(0.0, array));
        } else if (element instanceof JsonArray array) {
            entries.add(Map.entry(0.0, array));
        } else if (element instanceof JsonObject obj) {
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                if (key.equals("easing") || key.equals("easingArgs") || key.equals("lerp_mode")) {
                    continue;
                }
                double time = parseTime(key);
                if (entry.getValue() instanceof JsonObject frame && !frame.has("vector")) {
                    if (frame.has("pre")) {
                        entries.add(Map.entry(time, frame.getAsJsonArray("pre")));
                    }
                    if (frame.has("post")) {
                        entries.add(Map.entry(time + 0.0000001, frame.getAsJsonArray("post")));
                    }
                } else if (entry.getValue() instanceof JsonObject frame) {
                    entries.add(Map.entry(time, frame.getAsJsonArray("vector")));
                } else if (entry.getValue() instanceof JsonArray array) {
                    entries.add(Map.entry(time, array));
                }
            }
        }
        if (entries.isEmpty()) {
            return null;
        }
        Keyframe[] frames = new Keyframe[entries.size()];
        double prevTime = 0;
        Expr[] prev = null;
        for (int i = 0; i < frames.length; i++) {
            double time = entries.get(i).getKey();
            JsonArray vector = entries.get(i).getValue();
            Expr[] value = new Expr[3];
            for (int axis = 0; axis < 3; axis++) {
                Expr raw = Expr.parse(vector.get(axis));
                // Rotations: degrees to radians, x and y flipped (GeckoLib's axes)
                value[axis] = rotation ? Expr.rotation(raw, axis != 2) : raw;
            }
            frames[i] = new Keyframe((time - prevTime) * 20, prev == null ? value : prev, value);
            prevTime = time;
            prev = value;
        }
        return frames;
    }

    private static double parseTime(String key) {
        try {
            return Double.parseDouble(key);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** One axis of a channel at {@code tick} (AnimationController.getAnimationPointAtTick + EasingType.LINEAR). */
    static double sample(Keyframe[] frames, int axis, double tick) {
        double animTime = tick / 20.0;
        double total = 0;
        Keyframe frame = frames[frames.length - 1];
        double current = tick;
        for (Keyframe candidate : frames) {
            total += candidate.length();
            if (total > tick) {
                frame = candidate;
                current = tick - (total - candidate.length());
                break;
            }
        }
        double end = frame.end()[axis].eval(animTime);
        if (current >= frame.length()) {
            return end;
        }
        double start = frame.start()[axis].eval(animTime);
        return start + (end - start) * (current / frame.length());
    }

    // ------------------------------------------------------------------ Molang

    /** A Molang value: numbers, + - * /, parentheses, query.anim_time and math.* functions (sin/cos in degrees). */
    interface Expr {
        double eval(double animTime);

        static Expr rotation(Expr raw, boolean flip) {
            if (raw instanceof Const constant) {
                return new Const(Math.toRadians(flip ? -constant.value() : constant.value()));
            }
            return t -> Math.toRadians(flip ? -raw.eval(t) : raw.eval(t));
        }

        static Expr parse(JsonElement element) {
            if (!element.isJsonPrimitive()) {
                return new Const(0);
            }
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                return new Const(primitive.getAsDouble());
            }
            String text = primitive.getAsString().trim();
            try {
                return new Const(Double.parseDouble(text));
            } catch (NumberFormatException e) {
                try {
                    return new MolangReader(text).read();
                } catch (RuntimeException ex) {
                    DnDClasses.LOGGER.warn("Can't evaluate Molang '{}' for dragon parts: {}", text, ex.getMessage());
                    return new Const(0);
                }
            }
        }
    }

    private record Const(double value) implements Expr {
        @Override
        public double eval(double animTime) {
            return this.value;
        }
    }

    /** Recursive descent over a Molang expression. */
    private static final class MolangReader {
        private final String text;
        private int pos;

        MolangReader(String text) {
            this.text = text.toLowerCase();
        }

        Expr read() {
            Expr expr = this.sum();
            this.skipSpaces();
            if (this.pos < this.text.length()) {
                throw new IllegalArgumentException("unexpected '" + this.text.charAt(this.pos) + "'");
            }
            return expr;
        }

        private Expr sum() {
            Expr left = this.product();
            while (true) {
                if (this.eat('+')) {
                    Expr a = left, b = this.product();
                    left = t -> a.eval(t) + b.eval(t);
                } else if (this.eat('-')) {
                    Expr a = left, b = this.product();
                    left = t -> a.eval(t) - b.eval(t);
                } else {
                    return left;
                }
            }
        }

        private Expr product() {
            Expr left = this.unary();
            while (true) {
                if (this.eat('*')) {
                    Expr a = left, b = this.unary();
                    left = t -> a.eval(t) * b.eval(t);
                } else if (this.eat('/')) {
                    Expr a = left, b = this.unary();
                    left = t -> a.eval(t) / b.eval(t);
                } else {
                    return left;
                }
            }
        }

        private Expr unary() {
            if (this.eat('-')) {
                Expr inner = this.unary();
                return t -> -inner.eval(t);
            }
            if (this.eat('+')) {
                return this.unary();
            }
            return this.primary();
        }

        private Expr primary() {
            this.skipSpaces();
            if (this.eat('(')) {
                Expr inner = this.sum();
                this.expect(')');
                return inner;
            }
            int start = this.pos;
            char c = this.peek();
            if (Character.isDigit(c) || c == '.') {
                while (this.pos < this.text.length() && (Character.isDigit(this.text.charAt(this.pos)) || this.text.charAt(this.pos) == '.')) {
                    this.pos++;
                }
                return new Const(Double.parseDouble(this.text.substring(start, this.pos)));
            }
            while (this.pos < this.text.length() && (Character.isLetterOrDigit(this.text.charAt(this.pos))
                    || this.text.charAt(this.pos) == '_' || this.text.charAt(this.pos) == '.')) {
                this.pos++;
            }
            String name = this.text.substring(start, this.pos);
            if (name.isEmpty()) {
                throw new IllegalArgumentException("expected a value at " + start);
            }
            if (this.eat('(')) {
                List<Expr> args = new ArrayList<>();
                if (!this.eat(')')) {
                    do {
                        args.add(this.sum());
                    } while (this.eat(','));
                    this.expect(')');
                }
                return function(name, args);
            }
            return switch (name) {
                case "query.anim_time", "q.anim_time" -> t -> t;
                case "math.pi" -> new Const(Math.PI);
                default -> throw new IllegalArgumentException("unknown variable " + name);
            };
        }

        private static Expr function(String name, List<Expr> args) {
            Expr a = args.isEmpty() ? new Const(0) : args.get(0);
            Expr b = args.size() < 2 ? new Const(0) : args.get(1);
            return switch (name) {
                case "math.sin" -> t -> Math.sin(Math.toRadians(a.eval(t)));
                case "math.cos" -> t -> Math.cos(Math.toRadians(a.eval(t)));
                case "math.abs" -> t -> Math.abs(a.eval(t));
                case "math.max" -> t -> Math.max(a.eval(t), b.eval(t));
                case "math.min" -> t -> Math.min(a.eval(t), b.eval(t));
                case "math.sqrt" -> t -> Math.sqrt(a.eval(t));
                case "math.floor" -> t -> Math.floor(a.eval(t));
                case "math.ceil" -> t -> Math.ceil(a.eval(t));
                case "math.round" -> t -> Math.round(a.eval(t));
                case "math.mod" -> t -> a.eval(t) % b.eval(t);
                case "math.pow" -> t -> Math.pow(a.eval(t), b.eval(t));
                default -> throw new IllegalArgumentException("unknown function " + name);
            };
        }

        private char peek() {
            this.skipSpaces();
            return this.pos < this.text.length() ? this.text.charAt(this.pos) : '\0';
        }

        private boolean eat(char c) {
            if (this.peek() == c) {
                this.pos++;
                return true;
            }
            return false;
        }

        private void expect(char c) {
            if (!this.eat(c)) {
                throw new IllegalArgumentException("expected '" + c + "'");
            }
        }

        private void skipSpaces() {
            while (this.pos < this.text.length() && Character.isWhitespace(this.text.charAt(this.pos))) {
                this.pos++;
            }
        }
    }
}
