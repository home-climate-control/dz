package net.sf.dz3r.view.ha;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HomeAssistantConnectorTest {

    private final HomeAssistantConnector.TopicResolver resolver = new HomeAssistantConnector.TopicResolver();

    @Test
    void resolveAbbreviated() {

        var root = "theRoot";
        var abbreviated ="~/theTopic";

        assertThat(resolver.resolve(root, abbreviated)).isEqualTo("theRoot/theTopic");
    }

    @Test
    void resolveNotPresent() {

        var root = "theRoot";
        var absolute ="/theTopic";

        assertThat(resolver.resolve(root, absolute)).isEqualTo(absolute);
    }
}
