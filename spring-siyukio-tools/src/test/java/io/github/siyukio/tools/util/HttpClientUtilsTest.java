package io.github.siyukio.tools.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Bugee
 */
@Tag("k8s")
@Slf4j
public class HttpClientUtilsTest {

    private List<String> resolveDomain(String domain) {
        List<String> ips = HttpClientUtils.resolveDomain(domain);
        log.info("{} ips: {}", domain, ips);
        return ips;
    }

    @Test
    void testResolveDomain() {
        String domain = "www.codebuddy.com";
        List<String> ips = this.resolveDomain(domain);
        assertFalse(ips.isEmpty());
        assertNotNull(ips.getFirst());

        this.resolveDomain("siyukio.local");
        this.resolveDomain("siyukio-bootstrap");
        this.resolveDomain("siyukio-bootstrap-headless");
    }
}
