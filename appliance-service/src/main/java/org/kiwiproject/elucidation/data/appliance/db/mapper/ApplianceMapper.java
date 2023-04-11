package org.kiwiproject.elucidation.data.appliance.db.mapper;

import org.kiwiproject.elucidation.data.appliance.model.Appliance;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ApplianceMapper implements RowMapper<Appliance> {
    @Override
    public Appliance map(ResultSet rs, StatementContext ctx) throws SQLException {
        return Appliance.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .brand(rs.getString("brand"))
                .location(rs.getString("location"))
                .state(Appliance.State.valueOf(rs.getString("state")))
                .build();
    }
}
