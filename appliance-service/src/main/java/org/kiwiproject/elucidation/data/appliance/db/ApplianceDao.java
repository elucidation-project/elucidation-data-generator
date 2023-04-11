package org.kiwiproject.elucidation.data.appliance.db;

import org.kiwiproject.elucidation.data.appliance.db.mapper.ApplianceMapper;
import org.kiwiproject.elucidation.data.appliance.model.Appliance;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

@RegisterRowMapper(ApplianceMapper.class)
public interface ApplianceDao {

    @SqlQuery("select * from appliances")
    List<Appliance> findAll();

    @SqlQuery("select * from appliances where id = :id")
    Optional<Appliance> findById(@Bind("id") Long id);

    @SqlUpdate("insert into appliances (name, brand, location, state) values (:name, :brand, :location, :state)")
    @GetGeneratedKeys
    long create(@BindBean Appliance appliance);

    @SqlUpdate("delete from appliances where id = :id")
    int deleteAppliance(@Bind("id") long id);

    @SqlUpdate("update appliances set state = :state where id = :id")
    int updateState(@Bind("state") Appliance.State state, @Bind("id") long id);
}
