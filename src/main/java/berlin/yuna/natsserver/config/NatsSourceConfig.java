package berlin.yuna.natsserver.config;

import berlin.yuna.clu.model.OsArch;
import berlin.yuna.clu.model.OsArchType;
import berlin.yuna.clu.model.OsType;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public enum NatsSourceConfig {

    URL("https://github.com/nats-io/nats-server/releases/download/v2.6.4/nats-server-v2.6.4#SYSTEM#.zip", "[STRING] DEFAULT SOURCE URL");

    private final String defaultValue;
    private final String description;

    NatsSourceConfig(final String defaultValue, final String description) {
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public String getDefaultValue(final OsType os, OsArch arch, final OsArchType archType) {
        return defaultValue
                .replace("#SYSTEM#", osString(os, "-") + osString(arch, "-") + osString(archType, null))
                .replace("mips64", "linux-mips64le");
    }

    public String getDescription() {
        return description;
    }

    private static String osString(final Enum<?> input, final String prefix) {
        if (input != null && !input.toString().contains("UNKNOWN")) {
            String result = input.toString();
            result = result.startsWith("OS_") ? result.substring(3) : result;
            result = result.startsWith("ARCH_") ? result.substring(5) : result;
            result = result.startsWith("AT_") ? result.substring(3) : result;
            result = result.replace("86", "386");
            result = result.replace("INTEL", "");
            result = result.replace("MAC", "darwin");
            result = result.replace("_", "");
            return (prefix == null ? "" : "-") + result.toLowerCase();
        }
        return "";
    }
}
