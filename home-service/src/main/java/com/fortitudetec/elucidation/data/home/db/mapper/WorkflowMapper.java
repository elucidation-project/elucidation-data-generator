package com.fortitudetec.elucidation.data.home.db.mapper;

import com.fortitudetec.elucidation.data.home.model.Workflow;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WorkflowMapper implements RowMapper<Workflow> {
    @Override
    public Workflow map(ResultSet rs, StatementContext ctx) throws SQLException {
        return Workflow.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .stepJson(rs.getString("step_json"))
                .build();
    }
}
