package io.github.siyukio.postgresql.support;

import lombok.Getter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Bugee
 */
public class MultiJdbcTemplate {

    @Getter
    private final JdbcTemplate master;

    @Getter
    private final DataSource dataSource;

    private final List<JdbcTemplate> slaves;

    public MultiJdbcTemplate(DataSource masterDataSource, List<DataSource> slaveDataSources) {
        this.dataSource = masterDataSource;
        this.master = new JdbcTemplate(masterDataSource);
        this.slaves = new ArrayList<>();
        for (DataSource slave : slaveDataSources) {
            this.slaves.add(new JdbcTemplate(slave));
        }
    }

    public JdbcTemplate getRandomSlave() {
        if (CollectionUtils.isEmpty(this.slaves)) {
            return this.master;
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            return this.master;
        }

        if (this.slaves.size() == 1) {
            return this.slaves.getFirst();
        }
        int index = ThreadLocalRandom.current().nextInt(this.slaves.size());
        return this.slaves.get(index);
    }

}
