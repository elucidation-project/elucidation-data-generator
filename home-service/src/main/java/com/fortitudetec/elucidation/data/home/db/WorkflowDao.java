package com.fortitudetec.elucidation.data.home.db;

import com.fortitudetec.elucidation.data.home.db.mapper.WorkflowMapper;
import com.fortitudetec.elucidation.data.home.model.Workflow;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

@RegisterRowMapper(WorkflowMapper.class)
public interface WorkflowDao {

    @SqlQuery("select * from workflows")
    List<Workflow> findAll();

    @SqlUpdate("insert into workflows (name, step_json) values (:name, :stepJson)")
    @GetGeneratedKeys
    long create(@BindBean Workflow workflow);

    @SqlUpdate("delete from workflows where id = :id")
    int deleteWorkflow(@Bind("id") long id);

}
