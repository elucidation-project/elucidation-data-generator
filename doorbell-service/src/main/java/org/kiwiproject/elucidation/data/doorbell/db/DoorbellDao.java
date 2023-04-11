package org.kiwiproject.elucidation.data.doorbell.db;

import org.kiwiproject.elucidation.data.doorbell.db.mapper.DoorbellMapper;
import org.kiwiproject.elucidation.data.doorbell.model.Doorbell;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

@RegisterRowMapper(DoorbellMapper.class)
public interface DoorbellDao {

    @SqlQuery("select * from doorbells")
    List<Doorbell> findAll();

    @SqlQuery("select * from doorbells where id = :id")
    Optional<Doorbell> findById(@Bind("id") Long id);

    @SqlUpdate("insert into doorbells (name, brand) values (:name, :brand)")
    @GetGeneratedKeys
    long create(@BindBean Doorbell thermostat);

    @SqlUpdate("delete from doorbells where id = :id")
    int deleteDoorbell(@Bind("id") long id);
}
