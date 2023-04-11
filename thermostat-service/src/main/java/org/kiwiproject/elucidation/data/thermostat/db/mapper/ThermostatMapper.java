package org.kiwiproject.elucidation.data.thermostat.db.mapper;

import org.kiwiproject.elucidation.data.thermostat.model.Thermostat;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ThermostatMapper implements RowMapper<Thermostat> {
    @Override
    public Thermostat map(ResultSet rs, StatementContext ctx) throws SQLException {
        return Thermostat.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .brand(rs.getString("brand"))
                .location(rs.getString("location"))
                .currentTemp(rs.getDouble("current_temp"))
                .build();
    }
}
