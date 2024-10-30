package com.example.deptech.mapper;

import com.example.deptech.entity.PhoneCn;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PhoneCnMapper {
    String a(int id);


    @Insert("INSERT INTO phone_cn(phone, type, number, status, create_time, update_time, location) VALUES (#{phone}, #{type}, #{number}, #{status}, #{createTime}, #{updateTime}, #{location})")
    void insertPhoneCn(String phone, String type,Integer number,Integer status,long createTime,long updateTime,String location);
    @Update("UPDATE phone_cn SET type = #{type} AND number = #{number} AND update_time = #{updateTime} WHERE phone = #{phone}")
    void updatePhoneType(String phone, String type,Integer number,long updateTime);

    @Select("SELECT * FROM phone_cn WHERE phone = #{phone}")
    PhoneCn selectByPhoneCn(String phone);

}