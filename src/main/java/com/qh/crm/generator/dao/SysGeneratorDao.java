package com.qh.crm.generator.dao;

import java.util.List;
import java.util.Map;

/**
 * 代码生成器
 * @author mada
 * @date 2018年04月05日 上午12:01:45
 */
public interface SysGeneratorDao {
	
	List<Map<String, Object>> queryList(Map<String, Object> map);
	
	int queryTotal(Map<String, Object> map);
	
	Map<String, String> queryTable(String tableName);
	
	List<Map<String, String>> queryColumns(String tableName);
}
