package com.fortitudetec.elucidation.data.doorbell.db.mapper;

import com.fortitudetec.elucidation.data.doorbell.model.Doorbell;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DoorbellMapper implements RowMapper<Doorbell> {
    @Override
    public Doorbell map(ResultSet rs, StatementContext ctx) throws SQLException {
        return Doorbell.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .brand(rs.getString("brand"))
                .build();
    }
}
