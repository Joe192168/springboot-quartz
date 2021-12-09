package com.joe.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuartzJob {
	
	private String jobName;//任务名称
	private String jobGroup;//任务分组
	private String description;//任务描述
	private String jobClassName;//执行类包
	private String cronExpression;//执行时间
	private String triggerName;//执行时间
	private String triggerState;//任务状态
	private String oldJobName;//任务名称 用于修改
	private String oldJobGroup;//任务分组 用于修改
	private List<Map<String, Object>> jobDataParam;//任务自定义参数
	private Integer priority;//任务的优先级
}
