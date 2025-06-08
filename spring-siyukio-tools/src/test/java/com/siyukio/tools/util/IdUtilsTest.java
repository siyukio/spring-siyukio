package com.siyukio.tools.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * @author Bugee
 */
@Slf4j
public class IdUtilsTest {

    @Test
    void testUniqueId() {
        int num = 10;
        while (num > 0) {
            String id = IdUtils.getUniqueId();
            String fromBase = IdUtils.fromBase(id);
            log.info("{}, {}", id, fromBase);
            num--;
        }
    }

    @Test
    void testCompare() {
        String min = "R2W6LCA4P0DJFLLW3TJ44X25";
        String max = "R2W6M1L1Q4BTR50EMVNDQKCJ";
        log.info("{}", max.compareTo(min));
    }
}
