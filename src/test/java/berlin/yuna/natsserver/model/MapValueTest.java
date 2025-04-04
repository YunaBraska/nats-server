package berlin.yuna.natsserver.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static berlin.yuna.natsserver.model.MapValue.mapValueOf;
import static berlin.yuna.natsserver.model.ValueSource.DEFAULT;
import static berlin.yuna.natsserver.model.ValueSource.ENV;
import static berlin.yuna.natsserver.model.ValueSource.FILE;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("UnitTest")
@DisplayName("Nats config test")
class MapValueTest {

    @Test
    void updateValueOnlyOnHigherSourceLevel() {
        final String startValue = "start value";
        final String newValue = "new value";

        //update
        assertThat(mapValueOf(ENV, startValue).update(ENV, newValue).value()).isEqualTo(newValue);
        //downgrade not possible
        assertThat(mapValueOf(ENV, startValue).update(DEFAULT, newValue).value()).isEqualTo(startValue);
        //upgrade
        assertThat(mapValueOf(ENV, startValue).update(FILE, newValue).value()).isEqualTo(newValue);
    }
}