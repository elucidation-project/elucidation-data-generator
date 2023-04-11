package org.kiwiproject.elucidation.data.light.db.mapper;

import org.kiwiproject.elucidation.data.light.model.SmartLight;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SmartLightMapper implements RowMapper<SmartLight> {
    @Override
    public SmartLight map(ResultSet rs, StatementContext ctx) throws SQLException {
        return SmartLight.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .brand(rs.getString("brand"))
                .location(rs.getString("location"))
                .state(SmartLight.State.valueOf(rs.getString("state")))
                .color(SmartLight.Color.valueOf(rs.getString("color")))
                .brightness(rs.getInt("brightness"))
                .build();
    }
}
