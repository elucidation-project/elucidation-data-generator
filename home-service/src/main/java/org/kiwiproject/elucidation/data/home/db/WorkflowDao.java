package org.kiwiproject.elucidation.data.home.db;

import org.kiwiproject.elucidation.data.home.db.mapper.WorkflowMapper;
import org.kiwiproject.elucidation.data.home.model.Workflow;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

@RegisterRowMapper(WorkflowMapper.class)
public interface WorkflowDao {

    @SqlQuery("select * from workflows")
    List<Workflow> findAll();

    @SqlQuery("select * from workflows where id = :id")
    Optional<Workflow> findById(@Bind("id") long id);

    @SqlQuery("select * from workflows where name = :name")
    Optional<Workflow> findByName(@Bind("name") String name);

    @SqlUpdate("insert into workflows (name, step_json) values (:name, :stepJson)")
    @GetGeneratedKeys
    long create(@BindBean Workflow workflow);

    @SqlUpdate("delete from workflows where id = :id")
    int deleteWorkflow(@Bind("id") long id);

}
