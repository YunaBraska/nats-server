package berlin.yuna.natsserver.model;

public class MapValue {

    private ValueSource source;
    private String value;

    public static MapValue mapValueOf(final ValueSource source, final String value) {
        return new MapValue(source, value);
    }

    public MapValue(final ValueSource source, final String value) {
        this.source = source;
        this.value = value;
    }

    public MapValue update(final ValueSource source, final String value) {
        if (source.ordinal() >= this.source.ordinal()) {
            this.source = source;
            this.value = value;
        }
        return this;
    }

    public ValueSource source() {
        return source;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "ConfigValue{" +
                "source=" + source +
                ", value='" + value + '\'' +
                '}';
    }
}
