package com.qh.crm.generator.utils;

import com.qh.crm.generator.entity.ColumnEntity;
import com.qh.crm.generator.entity.TableEntity;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器   工具类
 * @author mada
 * @date 2018年04月05日 上午12:01:45
 */
public class GenUtils {

    public static List<String> getTemplates(){
        List<String> templates = new ArrayList<String>();
        templates.add("template/Entity.java.vm");
        templates.add("template/EntityQueryBo.java.vm");
        templates.add("template/EntitySaveBo.java.vm");
        /*templates.add("template/EntityInsertVo.java.vm");
        templates.add("template/EntityUpdateVo.java.vm");*/
        templates.add("template/EntityListVo.java.vm");
        templates.add("template/EntityDetailVo.java.vm");
        //templates.add("template/EntityPageListVo.java.vm");
        templates.add("template/Dao.java.vm");
        templates.add("template/Dao.xml.vm");
        templates.add("template/Service.java.vm");
        templates.add("template/ServiceImpl.java.vm");
        templates.add("template/Controller.java.vm");
        templates.add("template/Api.java.vm");
        templates.add("template/Fallback.java.vm");
//        templates.add("template/menu.sql.vm");
//
//        templates.add("template/adminlte/list.html.vm");
//        templates.add("template/adminlte/list.js.vm");

//        templates.add("template/elementui/index.vue.vm");
//        templates.add("template/elementui/add-or-update.vue.vm");
        templates.add("template/elementui/index.js.vm");

        return templates;
    }

    /**
     * 生成代码
     */
    public static void generatorCode(Map<String, String> table,
                                     List<Map<String, String>> columns, ZipOutputStream zip) {
        //配置信息
        Configuration config = getConfig();
        boolean hasBigDecimal = false;
        boolean hasLocalDate = false;
        boolean hasLocalDateTime = false;
        boolean dateTime = false;
        boolean time = false;
        //表信息
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableName(table.get("tableName" ));
        tableEntity.setComments(table.get("tableComment" ));
        //表名转换成Java类名
        String className = tableToJava(tableEntity.getTableName(), config.getString("tablePrefix" ));
        tableEntity.setClassName(className);
        tableEntity.setClassname(StringUtils.uncapitalize(className));

        //列信息
        List<ColumnEntity> columsList = new ArrayList<>();
        for(Map<String, String> column : columns){
            ColumnEntity columnEntity = new ColumnEntity();
            columnEntity.setColumnName(column.get("columnName" ));
            columnEntity.setDataType(column.get("dataType" ));
            columnEntity.setComments(column.get("columnComment" ));
            columnEntity.setExtra(column.get("extra" ));

            //列名转换成Java属性名
            String attrName = columnToJava(columnEntity.getColumnName());
            columnEntity.setAttrName(attrName);
            columnEntity.setAttrname(StringUtils.uncapitalize(attrName));

            //列的数据类型，转换成Java类型
            String attrType = config.getString(columnEntity.getDataType(), "unknowType" );
            columnEntity.setAttrType(attrType);
            if(attrType.equals("unknowType")){
                if (columnEntity.getDataType().equals("datetime")) {
                    dateTime = true;
                    hasLocalDate = true;
                    columnEntity.setAttrType("Date");
                }else if (columnEntity.getDataType().equals("time")){
                    time = true;
                    hasLocalDate = true;
                    columnEntity.setAttrType("Date");
                }
            }

            //是否主键
            if ("PRI".equalsIgnoreCase(column.get("columnKey" )) && tableEntity.getPk() == null) {
                tableEntity.setPk(columnEntity);
            }

            columsList.add(columnEntity);
        }
        tableEntity.setColumns(columsList);

        //没主键，则第一个字段为主键
        if (tableEntity.getPk() == null) {
            tableEntity.setPk(tableEntity.getColumns().get(0));
        }

        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader" );
        Velocity.init(prop);
        String mainPath = config.getString("mainPath" );
        mainPath = StringUtils.isBlank(mainPath) ? "com.qh" : mainPath;
        //封装模板数据
        Map<String, Object> map = new HashMap<>();
//        map.put("tableName", tableEntity.getTableName().toUpperCase());
        map.put("tableName", tableEntity.getTableName());
        map.put("comments", tableEntity.getComments());
        // 添加注释模块名称，为表注释内容去除最后一位
        String docModule = tableEntity.getComments().trim();
        docModule = Objects.equals(docModule, "") ? tableEntity.getClassName() : docModule.substring(0,docModule.length() -1);
        map.put("docModule",docModule);
        map.put("pk", tableEntity.getPk());
        map.put("className", tableEntity.getClassName());
        map.put("classname", tableEntity.getClassname());
        // 添加表名转小写，下划线转斜杠
        map.put("class_name", tableEntity.getTableName().toLowerCase());
        map.put("pathName", tableEntity.getClassname().toLowerCase());
        map.put("columns", tableEntity.getColumns());
        map.put("hasBigDecimal", hasBigDecimal);
        map.put("dateTime", dateTime);
        map.put("time", time);
        map.put("hasLocalDate", hasLocalDate);
        map.put("hasLocalDateTime", hasLocalDateTime);
        map.put("mainPath", mainPath);
        map.put("package", config.getString("package" ));
        // 截取表名第一个前缀为模块名
        String moduleName = config.getString("moduleName" );
        // 截取表名第一个前缀之后的字符为路径
        String path = "";
        String[] strArr = tableEntity.getTableName().split("_");
        for (int i = 0; i < strArr.length; i++){
            /*if (i == 0){
                moduleName = strArr[i];
            }else {
                path += "/" + strArr[i];
            }*/
            path += "/" + strArr[i];
        }

        map.put("moduleName", moduleName);
        map.put("path", path);
        map.put("author", config.getString("author" ));
        map.put("commons", config.getString("commons" ));
        map.put("email", config.getString("email" ));
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        VelocityContext context = new VelocityContext(map);

        //获取模板列表
        List<String> templates = getTemplates();
        for (String template : templates) {
            //渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, "UTF-8" );
            tpl.merge(context, sw);

            try {
                //添加到zip
                zip.putNextEntry(new ZipEntry(getFileName(template, tableEntity.getClassName(), config.getString("package" ), moduleName)));
                IOUtils.write(sw.toString(), zip, "UTF-8" );
                IOUtils.closeQuietly(sw);
                zip.closeEntry();
            } catch (IOException e) {
                throw new RRException("渲染模板失败，表名：" + tableEntity.getTableName(), e);
            }
        }
    }


    /**
     * 列名转换成Java属性名
     */
    public static String columnToJava(String columnName) {
        if("class".equals(columnName)){
            return "clazz";
        }
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "" );
    }

    /**
     * 表名转换成Java类名
     */
    public static String tableToJava(String tableName, String tablePrefix) {
        if (StringUtils.isNotBlank(tablePrefix)) {
            tableName = tableName.replace(tablePrefix, "" );
        }
        return columnToJava(tableName);
    }

    /**
     * 获取配置信息
     */
    public static Configuration getConfig() {
        try {
            return new PropertiesConfiguration("generator.properties" );
        } catch (ConfigurationException e) {
            throw new RRException("获取配置文件失败，", e);
        }
    }

    /**
     * 获取文件名
     */
    public static String getFileName(String template, String className, String packageName, String moduleName) {
        String packagePath = "main" + File.separator + "java" + File.separator;
        if (StringUtils.isNotBlank(packageName)) {
            packagePath += packageName.replace(".", File.separator) + File.separator + moduleName + File.separator;
        }

        if (template.contains("Entity.java.vm" )) {
            return packagePath + "entity" + File.separator + className + "Entity.java";
        }

        if (template.contains("EntityQueryBo.java.vm" )) {
            return packagePath + "bo" + File.separator + className + "QueryBo.java";
        }

        if (template.contains("EntityInsertVo.java.vm" )) {
            return packagePath + "vo" + File.separator + "request" + File.separator + className + "InsertVo.java";
        }

        if (template.contains("EntityUpdateVo.java.vm" )) {
            return packagePath + "vo" + File.separator + "request" + File.separator + className + "UpdateVo.java";
        }

        if (template.contains("EntitySaveBo.java.vm" )) {
            return packagePath + "bo" + File.separator + className + "SaveBo.java";
        }

        if (template.contains("EntityDetailVo.java.vm" )) {
            return packagePath + "vo" + File.separator + className + "DetailVo.java";
        }

        if (template.contains("EntityListVo.java.vm" )) {
            return packagePath + "vo" + File.separator + className + "ListVo.java";
        }

        if (template.contains("EntityPageListVo.java.vm" )) {
            return packagePath + "vo" + File.separator + className + "PageListVo.java";
        }

        if (template.contains("Dao.java.vm" )) {
            return packagePath + "mapper" + File.separator + className + "Mapper.java";
        }

        if (template.contains("Service.java.vm" )) {
            return packagePath + "service" + File.separator + "I" + className + "Service.java";
        }

        if (template.contains("ServiceImpl.java.vm" )) {
            return packagePath + "service" + File.separator + "impl" + File.separator + className + "ServiceImpl.java";
        }

        if (template.contains("Controller.java.vm" )) {
            return packagePath + "controller" + File.separator + className + "Controller.java";
        }

        if (template.contains("Api.java.vm" )) {
            return packagePath + "api" + File.separator + className + "Api.java";
        }

        if (template.contains("Fallback.java.vm" )) {
            return packagePath + "fallback" + File.separator + className + "Fallback.java";
        }

        if (template.contains("Dao.xml.vm" )) {
            return "main" + File.separator + "resources" + File.separator + "mapper" + File.separator + moduleName + File.separator + className + "Mapper.xml";
        }

        if (template.contains("menu.sql.vm" )) {
            return className.toLowerCase() + "_menu.sql";
        }

        if (template.contains("list.html.vm" )) {
            return "main" + File.separator + "resources" + File.separator + "adminlte" + File.separator
                    + "modules" + File.separator + moduleName + File.separator + className.toLowerCase() + ".html";
        }

        if (template.contains("list.js.vm" )) {
            return "main" + File.separator + "resources" + File.separator + "adminlte" + File.separator + "js" + File.separator
                    + "modules" + File.separator + moduleName + File.separator + className.toLowerCase() + ".js";
        }

        if (template.contains("index.vue.vm" )) {
            return "main" + File.separator + "resources" + File.separator + "elementui" + File.separator + "src" + File.separator + "views" +
                    File.separator + className.toLowerCase() + File.separator + "index.vue";
        }

        if (template.contains("add-or-update.vue.vm" )) {
            return "main" + File.separator + "resources" + File.separator + "elementui" + File.separator + "src" + File.separator + "views" +
                    File.separator + className.toLowerCase() + File.separator +"add-or-update.vue";
        }

        if (template.contains("index.js.vm" )) {
            return "main" + File.separator + "resources" + File.separator + "elementui" + File.separator + "src" + File.separator + "api" +
                    File.separator+ "modules" + File.separator + className.toLowerCase() + ".js";
        }

        return null;
    }
}
