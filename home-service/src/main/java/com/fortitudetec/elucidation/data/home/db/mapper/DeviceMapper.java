package com.fortitudetec.elucidation.data.home.db.mapper;

import com.fortitudetec.elucidation.data.home.model.Device;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DeviceMapper implements RowMapper<Device> {
    @Override
    public Device map(ResultSet rs, StatementContext ctx) throws SQLException {
        return Device.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .deviceType(Device.DeviceType.valueOf(rs.getString("device_type")))
                .deviceTypeId(rs.getLong("device_type_id"))
                .build();
    }
}
