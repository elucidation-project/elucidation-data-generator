package com.fortitudetec.elucidation.data.home.db;

import com.fortitudetec.elucidation.data.home.db.mapper.DeviceMapper;
import com.fortitudetec.elucidation.data.home.model.Device;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

@RegisterRowMapper(DeviceMapper.class)
public interface DeviceDao {

    @SqlQuery("select * from devices")
    List<Device> findAll();

    @SqlQuery("select * from devices where id = :id")
    Optional<Device> findById(@Bind("id") Long id);

    @SqlUpdate("insert into devices (name, device_type, device_type_id) values (:name, :deviceType, :deviceTypeId)")
    @GetGeneratedKeys
    long create(@BindBean Device device);

    @SqlUpdate("delete from devices where id = :id")
    int deleteDevice(@Bind("id") long id);

    @SqlQuery("select * from devices where name = :name and device_type = :type")
    Optional<Device> findByNameAndType(@Bind("name") String name, @Bind("type") Device.DeviceType type);
}
