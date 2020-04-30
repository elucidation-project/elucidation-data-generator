package com.fortitudetec.elucidation.data.light.db;

import com.fortitudetec.elucidation.data.light.db.mapper.SmartLightMapper;
import com.fortitudetec.elucidation.data.light.model.SmartLight;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

@RegisterRowMapper(SmartLightMapper.class)
public interface SmartLightDao {

    @SqlQuery("select * from lights")
    List<SmartLight> findAll();

    @SqlQuery("select * from lights where id = :id")
    Optional<SmartLight> findById(@Bind("id") Long id);

    @SqlUpdate("insert into lights (name, brand, location, state, color, brightness) values (:name, :brand, :location, :state, :color, :brightness)")
    @GetGeneratedKeys
    long create(@BindBean SmartLight thermostat);

    @SqlUpdate("update lights set state = :state where id = :id")
    int saveState(@Bind("state") SmartLight.State state, @Bind("id") long id);

    @SqlUpdate("update lights set color = :color where id = :id")
    int setColor(@Bind("color") SmartLight.Color color, @Bind("id") long id);

    @SqlUpdate("update lights set brightness = :brightness where id = :id")
    int setBrightness(@Bind("brightness") int brightness, @Bind("id") long id);

    @SqlUpdate("delete from lights where id = :id")
    int deleteLight(@Bind("id") long id);
}
