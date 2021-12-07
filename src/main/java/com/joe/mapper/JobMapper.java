package com.joe.mapper;

import com.joe.entity.QuartzJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface JobMapper {

    List<QuartzJob> listJob(@Param("jobName") String jobName);
}

