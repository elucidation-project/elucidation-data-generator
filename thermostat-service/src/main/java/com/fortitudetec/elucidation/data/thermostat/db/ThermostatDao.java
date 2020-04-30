package com.fortitudetec.elucidation.data.thermostat.db;

import com.fortitudetec.elucidation.data.thermostat.db.mapper.ThermostatMapper;
import com.fortitudetec.elucidation.data.thermostat.model.Thermostat;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

@RegisterRowMapper(ThermostatMapper.class)
public interface ThermostatDao {

    @SqlQuery("select * from thermostats")
    List<Thermostat> findAll();

    @SqlQuery("select * from thermostats where id = :id")
    Optional<Thermostat> findById(@Bind("id") Long id);

    @SqlUpdate("insert into thermostats (name, brand, location, current_temp) values (:name, :brand, :location, :currentTemp)")
    @GetGeneratedKeys
    long create(@BindBean Thermostat thermostat);

    @SqlUpdate("update thermostats set current_temp = :currentTemp where id = :id")
    int setCurrentTemp(@Bind("currentTemp") double currentTemp, @Bind("id") long id);

    @SqlUpdate("delete from thermostats where id = :id")
    int deleteThermostat(@Bind("id") long id);
}
