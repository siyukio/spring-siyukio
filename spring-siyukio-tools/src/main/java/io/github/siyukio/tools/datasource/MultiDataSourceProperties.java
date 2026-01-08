package io.github.siyukio.tools.datasource;

import com.zaxxer.hikari.HikariConfig;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bugee
 */
@Data
public class MultiDataSourceProperties {

    public static final String CONFIG_PREFIX = "spring.datasource.postgres";

    private HikariConfig hikari;

    private DbNode master;

    private List<DbNode> slaves = new ArrayList<>();

    @Data
    public static class DbNode {

        private String url;

        private String username;

        private String password;
    }
}