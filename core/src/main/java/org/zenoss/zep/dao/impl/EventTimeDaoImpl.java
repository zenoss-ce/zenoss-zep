/*
 * Copyright (C) 2011, Zenoss Inc.  All Rights Reserved.
 */
package org.zenoss.zep.dao.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.zenoss.protobufs.zep.Zep;
import org.zenoss.zep.ZepException;
import org.zenoss.zep.annotations.TransactionalReadOnly;
import org.zenoss.zep.annotations.TransactionalRollbackAllExceptions;
import org.zenoss.zep.dao.EventTimeDao;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zenoss.zep.dao.impl.EventConstants.*;

public class EventTimeDaoImpl implements EventTimeDao {

    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(EventTimeDaoImpl.class);

    private final SimpleJdbcTemplate template;
    private final RangePartitioner partitioner;
    private final PartitionTableConfig partitionTableConfig;

    public EventTimeDaoImpl(DataSource dataSource, String databaseName,
                            PartitionConfig partitionConfig) {
        this.template = new SimpleJdbcTemplate(dataSource);
        this.partitionTableConfig = partitionConfig.getConfig(TABLE_EVENT_TIME);
        this.partitioner = new RangePartitioner(template, databaseName,
                TABLE_EVENT_TIME, COLUMN_PROCESSED,
                partitionTableConfig.getPartitionDuration(),
                partitionTableConfig.getPartitionUnit());
    }

    @Override
    @TransactionalRollbackAllExceptions
    public void purge(int duration, TimeUnit unit) throws ZepException {
        dropPartitionsOlderThan(duration, unit);
        initializePartitions();
    }


    @Override
    @TransactionalRollbackAllExceptions
    public void initializePartitions() throws ZepException {
        this.partitioner.createPartitions(
                this.partitionTableConfig.getInitialPastPartitions(),
                this.partitionTableConfig.getFuturePartitions());
    }

    @Override
    @TransactionalRollbackAllExceptions
    public int dropPartitionsOlderThan(int duration, TimeUnit unit)
            throws ZepException {
        return this.partitioner.dropPartitionsOlderThan(duration, unit);
    }

    @Override
    public long getPartitionIntervalInMs() {
        return this.partitionTableConfig.getPartitionUnit().toMillis(
                this.partitionTableConfig.getPartitionDuration());
    }

    @Override
    @TransactionalReadOnly
    public List<Zep.EventTime> findProcessedSince(Date startDate, int limit) {
        long timestamp = startDate.getTime();
        final Map<String, Long> params = Collections.singletonMap("since", timestamp);

        String sql = "SELECT * from %s where %s >= :since order by %s asc limit %s";
        sql = String.format(sql, TABLE_EVENT_TIME, COLUMN_PROCESSED, COLUMN_PROCESSED, limit);

        List<Zep.EventTime> eventTimes = template.query(sql,
                new EventTimeRowMapper(), params);
        return eventTimes;
    }

    @Override
    @TransactionalRollbackAllExceptions
    public void save(Zep.EventTime eventTime) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        fields.put(COLUMN_PROCESSED, eventTime.getProcessedTime());
        fields.put(COLUMN_CREATED, eventTime.getCreatedTime());
        fields.put(COLUMN_FIRST_SEEN, eventTime.getFirstSeenTime());
        byte[] summaryUuid = DaoUtils.uuidToBytes(eventTime.getSummaryUuid());
        fields.put(COLUMN_SUMMARY_UUID, summaryUuid);

        String insert = DaoUtils.createNamedInsert(TABLE_EVENT_TIME, fields.keySet());
        template.update(insert, fields);
    }

    private static class EventTimeRowMapper implements RowMapper<Zep.EventTime> {
        @Override
        public Zep.EventTime mapRow(ResultSet rs, int rowNum) throws SQLException {
            Zep.EventTime.Builder builder = Zep.EventTime.newBuilder();
            builder.setCreatedTime(rs.getLong(COLUMN_CREATED));
            builder.setProcessedTime(rs.getLong(COLUMN_PROCESSED));
            builder.setFirstSeenTime(rs.getLong(COLUMN_FIRST_SEEN));
            String summaryUuid = DaoUtils.uuidFromBytes(rs.getBytes(COLUMN_SUMMARY_UUID));
            builder.setSummaryUuid(summaryUuid);
            return builder.build();
        }
    }

}
