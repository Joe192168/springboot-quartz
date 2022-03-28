package com.joe.util;

import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.avatica.util.Quoting;
import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.SqlDialect.DatabaseProduct;
import org.apache.calcite.sql.dialect.MysqlSqlDialect;
import org.apache.calcite.sql.dialect.OracleSqlDialect;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
* @Description:    sql语法解析工具类
* @Author:         xqh
* @Date:           2022-03-28
* @Version:        3.0.0
*/
public class CalciteSqlUtils {

    private static SqlParser.Config mysqlConfig = SqlParser.configBuilder()
            .setLex(Lex.MYSQL)
            .setCaseSensitive(false)//大小写敏感
            .setQuoting(Quoting.BACK_TICK)
            .setQuotedCasing(Casing.TO_LOWER)
            .setUnquotedCasing(Casing.TO_LOWER)
            .setConformance(SqlConformanceEnum.MYSQL_5)
            .build();

    private static SqlParser.Config oralceConfig = SqlParser.configBuilder()
            .setLex(Lex.ORACLE)
            .setCaseSensitive(false)//大小写敏感
            .setQuoting(Quoting.BACK_TICK)
            .setQuotedCasing(Casing.TO_LOWER)
            .setUnquotedCasing(Casing.TO_LOWER)
            .setConformance(SqlConformanceEnum.ORACLE_12)
            .build();

    private static SqlParser.Config sqlserverConfig = SqlParser.configBuilder()
            .setLex(Lex.SQL_SERVER)
            .setCaseSensitive(false)//大小写敏感
            .setQuoting(Quoting.BACK_TICK)
            .setQuotedCasing(Casing.TO_LOWER)
            .setUnquotedCasing(Casing.TO_LOWER)
            .setConformance(SqlConformanceEnum.SQL_SERVER_2008)
            .build();

    public static List<Map<String,String>> mapList = new ArrayList<>();

    public static void main(String[] args) {

        String sql = "select id,name from t_user where id='${id}' and name='zhangsan'";

        try {
            List<Map<String, String>> list = handlerSqlTableAlias(DatabaseProduct.ORACLE, sql);
            //打印
            System.out.println("$$$$$$$$$$$$$打印别名sql$$$$$$$$$$$$$");
            list.forEach(System.out::println);
            //替换sql参数
            String rt = handlerSqlParameterSubstitution(DatabaseProduct.ORACLE,sql, "id");
            System.out.println("$$$$$$$$$$$$$打印参数sql$$$$$$$$$$$$$");
            System.out.println(rt);
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
    }

    /**
     * sql参数替换
     * @param type 数据库类型
     * @param sql sql
     * @param param 替换参数名称
     * @return
     * @throws SqlParseException
     */
    public static String handlerSqlParameterSubstitution(DatabaseProduct type,String sql,String param) throws SqlParseException {
        //加载配置信息
        SqlParser sqlParser = null;
        switch (type){
            case ORACLE:
                sqlParser = SqlParser.create(sql, oralceConfig);
                break;
            case MYSQL:
                sqlParser = SqlParser.create(sql, mysqlConfig);
                break;
            case MSSQL:
                sqlParser = SqlParser.create(sql, sqlserverConfig);
                break;
            default:
                sqlParser = SqlParser.create(sql, SqlParser.Config.DEFAULT);
                break;
        }
        //映射抽象树
        SqlNode sqlNode = sqlParser.parseQuery();
        //替换sql参数
        return handlerWhere(type,sqlNode, param);
    }

    /**
     * 提取sql中表别名集合
     * @param type
     * @param sql
     * @return
     * @throws SqlParseException
     */
    public static List<Map<String, String>> handlerSqlTableAlias(DatabaseProduct type,String sql) throws SqlParseException {
        //加载配置信息
        SqlParser sqlParser = null;
        switch (type){
            case ORACLE:
                sqlParser = SqlParser.create(sql, oralceConfig);
                break;
            case MYSQL:
                sqlParser = SqlParser.create(sql, mysqlConfig);
                break;
            case MSSQL:
                sqlParser = SqlParser.create(sql, sqlserverConfig);
                break;
            default:
                sqlParser = SqlParser.create(sql, SqlParser.Config.DEFAULT);
                break;
        }
        //映射抽象树
        SqlNode sqlNode = sqlParser.parseQuery();
        //提取sql表和别名的集合
        List<Map<String, String>> list = handlerSQL(sqlNode);
        //去重
        return list.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 提取sql中表和别名关系集合
     * @param sqlNode
     * @return
     */
    private static List<Map<String,String>> handlerSQL(SqlNode sqlNode) {
        SqlKind kind = sqlNode.getKind();
        switch (kind) {
            case SELECT:
                handlerSelect(sqlNode);
                break;
            case AS:
                SqlBasicCall sqlBasicCall = (SqlBasicCall) sqlNode;
                SqlNode selectNode1 = sqlBasicCall.getOperandList().get(0);
                SqlNode selectNode2 = sqlBasicCall.getOperandList().get(1);
                if (!SqlKind.UNION.equals(selectNode1.getKind())){
                    if (!SqlKind.SELECT.equals(selectNode1.getKind())){
                        Map<String,String> aliasMap = new HashMap<>();
                        aliasMap.put(selectNode2.toString(),selectNode1.toString());
                        mapList.add(aliasMap);
                        //System.out.println(selectNode1.toString()+":"+selectNode2.toString());
                    }
                }
                handlerSQL(selectNode1);
                break;
            case JOIN:
                SqlJoin sqlJoin = (SqlJoin) sqlNode;
                SqlNode left = sqlJoin.getLeft();
                handlerSQL(left);
                SqlNode right = sqlJoin.getRight();
                handlerSQL(right);
                SqlNode condition = sqlJoin.getCondition();
                if (condition!=null){
                    handlerField(condition);
                }
                break;
            case UNION:
                ((SqlBasicCall) sqlNode).getOperandList().forEach(node -> {
                    handlerSQL(node);
                });
                break;
            case ORDER_BY:
                handlerOrderBy(sqlNode);
                break;
        }
        return mapList;
    }

    /**
     * 获取order by字段
     * @param node
     */
    private static void handlerOrderBy(SqlNode node) {
        SqlOrderBy sqlOrderBy = (SqlOrderBy) node;
        SqlNode query = sqlOrderBy.query;
        handlerSQL(query);
        SqlNodeList orderList = sqlOrderBy.orderList;
        handlerField(orderList);
    }

    /**
     * 获取where条件
     * @param type
     * @param sqlNode
     * @param param
     * @return
     */
    private static String handlerWhere(DatabaseProduct type,SqlNode sqlNode,String param) {
        AtomicReference<String> sqlStr = new AtomicReference<>();
        SqlKind kind = sqlNode.getKind();
        switch (kind) {
            case SELECT:
                sqlStr.set(handlerSqlParameter(type,sqlNode, param));
                break;
            case JOIN:
                SqlJoin sqlJoin = (SqlJoin) sqlNode;
                SqlNode left = sqlJoin.getLeft();
                handlerLeftAndRight(type,left,param);
                SqlNode right = sqlJoin.getRight();
                handlerLeftAndRight(type,right,param);
                break;
            case UNION:
                ((SqlBasicCall) sqlNode).getOperandList().forEach(node -> {
                    sqlStr.set(handlerSqlParameter(type,node, param));
                });
                break;
        }
        return sqlStr.get();
    }

    /**
     * 获取左连接或右连接参数
     * @param type
     * @param sqlNode
     * @param param
     */
    private static void handlerLeftAndRight(DatabaseProduct type,SqlNode sqlNode,String param){
        SqlBasicCall leftSelectCall = (SqlBasicCall) sqlNode;
        List<SqlNode> leftOperandList = leftSelectCall.getOperandList();
        for (SqlNode node : leftOperandList) {
            SqlKind kind = node.getKind();
            //临时表，直接跳出本次循环
            if (SqlKind.IDENTIFIER.equals(kind)){
                break;
            }
            if (SqlKind.SELECT.equals(kind)){
                handlerWhere(type,node,param);
            }else{
                handlerLeftAndRight(type,node,param);
            }
        }
    }

    /**
     * 获取sql参数
     * @param type
     * @param node
     * @param param
     * @return
     */
    private static String handlerSqlParameter(DatabaseProduct type,SqlNode node,String param) {
        SqlSelect sqlSelect = (SqlSelect) node;
        SqlBasicCall where = (SqlBasicCall) sqlSelect.getWhere();
        //where为空，继续递归查询
        if (!sqlSelect.hasWhere()){
            handlerWhere(type,sqlSelect.getFrom(),param);
        }else{
            handlerOperand(where,sqlSelect,param);
        }
        //SqlDialect dialect = new SqlDialect(SqlDialect.DatabaseProduct.ORACLE, SqlDialect.DatabaseProduct.ORACLE.name(),"");
        //还原某个方言的sql
        //System.out.println(sqlSelect.toSqlString(OracleSqlDialect.DEFAULT).toString().replaceAll("\"",""));
        String sql = "";
        switch (type){
            case ORACLE:
                OracleSqlDialect oracleSqlDialect = new OracleSqlDialect(OracleSqlDialect.EMPTY_CONTEXT
                        .withDatabaseProduct(DatabaseProduct.ORACLE)
                        .withIdentifierQuoteString("")
                        .withDataTypeSystem(OracleSqlDialect.DEFAULT.getTypeSystem()));
                sql = sqlSelect.toSqlString(oracleSqlDialect).toString();
                break;
            case MYSQL:
                MysqlSqlDialect mysqlSqlDialect = new MysqlSqlDialect(MysqlSqlDialect.EMPTY_CONTEXT
                        .withDatabaseProduct(DatabaseProduct.MYSQL)
                        .withIdentifierQuoteString("")
                        .withDataTypeSystem(MysqlSqlDialect.DEFAULT.getTypeSystem()));
                sql = sqlSelect.toSqlString(mysqlSqlDialect).toString();
                break;
            default:
                OracleSqlDialect dialect = new OracleSqlDialect(OracleSqlDialect.EMPTY_CONTEXT
                        .withDatabaseProduct(DatabaseProduct.ORACLE)
                        .withIdentifierQuoteString("")
                        .withDataTypeSystem(OracleSqlDialect.DEFAULT.getTypeSystem()));
                sql = sqlSelect.toSqlString(dialect).toString();
                break;
        }
        return sql;
    }

    /**
     * 比对参数替换
     * @param where
     * @param sqlSelect
     * @param param
     */
    private static void handlerOperand(SqlBasicCall where,SqlSelect sqlSelect,String param){
        List<SqlNode> operandList = where.getOperandList();
        for (int i = 0; i < operandList.size(); i++) {
            SqlBasicCall operandStr = (SqlBasicCall)operandList.get(i);
            //该判断防止出现单个值
            SqlNode sqlNode = operandStr.getOperandList().size()>1?operandStr.getOperandList().get(i):operandStr.getOperandList().get(0);
            SqlKind kind = sqlNode.getKind();
            //该判断防止出现单个值,大于考虑到范围条件的情况
            SqlNode paramName = operandStr.getOperandList().size()>=2?operandStr:operandStr.getOperandList().get(0);
            int count = 0;
            //记录AND个数防止一行条件中多个导致整个节点替换
            count = countStr(paramName.toString().toLowerCase(),SqlKind.AND.name().toLowerCase(),count);
            if (count<2&&paramName.toString().toLowerCase().contains(param)){
                SqlOperator operator = new SqlBinaryOperator("=",
                        SqlKind.EQUALS,
                        0,
                        false,
                        operandStr.getOperator().getReturnTypeInference(),
                        operandStr.getOperator().getOperandTypeInference(),
                        operandStr.getOperator().getOperandTypeChecker());
                SqlNode[] operands = new SqlNode[2];
                SqlIdentifier sqlIdentifier = new SqlIdentifier("'jh'",sqlNode.getParserPosition());
                SqlCharStringLiteral literal = SqlCharStringLiteral.createCharString("jh", sqlNode.getParserPosition());
                operands[0]=sqlIdentifier;
                operands[1]=literal;
                SqlBasicCall operandCall = new SqlBasicCall(operator,operands,sqlNode.getParserPosition());
                //匹配参数的值改成恒值如：bi=bi
                where.setOperand(i,operandCall);
                sqlSelect.setWhere(where);
            }else {
                //没有匹配上的条件，也要放到where中
                where.setOperand(i,operandStr);
                sqlSelect.setWhere(where);
            }
            //递归遍历找到传入的参数位置并替换
            if (checkKind(kind)){
                handlerOperand(operandStr,sqlSelect,param);
            }
        }

    }

    /**
     * 获取sql字段包括条件中的
     * @param select
     */
    private static void handlerSelect(SqlNode select) {
        SqlSelect sqlSelect = (SqlSelect) select;
        //SELECT的字段信息
        SqlNodeList selectList = sqlSelect.getSelectList();
        //字段信息
        selectList.getList().forEach(list -> {
            handlerField(list);
        });
        handlerFrom(sqlSelect.getFrom());
        if (sqlSelect.hasWhere()) {
            handlerField(sqlSelect.getWhere());
        }
        if (sqlSelect.hasOrderBy()) {
            handlerField(sqlSelect.getOrderList());
        }
        SqlNodeList group = sqlSelect.getGroup();
        if (group != null) {
            group.forEach(groupField -> {
                handlerField(groupField);
            });
        }
    }

    /**
     * 获取子查询sql
     * @param from
     * @return
     */
    private static List<Map<String,String>> handlerFrom(SqlNode from) {
        SqlKind kind = from.getKind();
        switch (kind) {
            case IDENTIFIER:
                //最终的表名
                SqlIdentifier sqlIdentifier = (SqlIdentifier) from;
                //TODO 表名的替换，所以在此之前就需要获取到模型的信息
                //System.out.println("table name===" + sqlIdentifier.toString());
                break;
            case AS:
                SqlBasicCall sqlBasicCall = (SqlBasicCall) from;
                SqlNode selectNode1 = sqlBasicCall.getOperandList().get(0);
                SqlNode selectNode2 = sqlBasicCall.getOperandList().get(1);
                if (!SqlKind.UNION.equals(selectNode1.getKind())){
                    if (!SqlKind.SELECT.equals(selectNode1.getKind())){
                        Map<String,String> aliasMap = new HashMap<>();
                        aliasMap.put(selectNode2.toString(),selectNode1.toString());
                        mapList.add(aliasMap);
                        //System.out.println(selectNode1.toString()+":"+selectNode2.toString());
                    }
                }
                handlerSQL(selectNode1);
                break;
            case JOIN:
                SqlJoin sqlJoin = (SqlJoin) from;
                SqlNode left = sqlJoin.getLeft();
                handlerSQL(left);
                SqlNode right = sqlJoin.getRight();
                handlerSQL(right);
                SqlNode condition = sqlJoin.getCondition();
                if (condition!=null){
                    handlerField(condition);
                }
                break;
            case SELECT:
                handlerSQL(from);
                break;
        }
        return mapList;
    }

    /**
     * 获取字段
     * @param field
     */
    private static void handlerField(SqlNode field) {
        SqlKind kind = field.getKind();
        switch (kind) {
            case AS:
                List<SqlNode> operandList1 = ((SqlBasicCall) field).getOperandList();
                SqlNode left_as = operandList1.get(0);
                handlerField(left_as);
                break;
            case IDENTIFIER:
                //表示当前为子节点
                SqlIdentifier sqlIdentifier = (SqlIdentifier) field;
                //System.out.println("===field===" + sqlIdentifier.toString());
                break;
            default:
                if (field instanceof SqlBasicCall) {
                    List<SqlNode> operandList2 = ((SqlBasicCall) field).getOperandList();
                    for (int i = 0; i < operandList2.size(); i++) {
                        handlerField(operandList2.get(i));
                    }
                }
                if (field instanceof SqlNodeList) {
                    ((SqlNodeList) field).getList().forEach(node -> {
                        handlerField(node);
                    });
                }
                break;
        }
    }

    /**
     * 校验参数类型
     * @param kind
     * @return
     */
    private static boolean checkKind(SqlKind kind){
        if (SqlKind.AND.equals(kind)
                ||SqlKind.BETWEEN.equals(kind)
                ||SqlKind.LIKE.equals(kind)
                ||SqlKind.IN.equals(kind)
                ||SqlKind.LESS_THAN_OR_EQUAL.equals(kind)
                ||SqlKind.GREATER_THAN_OR_EQUAL.equals(kind)
                ||SqlKind.LESS_THAN.equals(kind)
                ||SqlKind.GREATER_THAN.equals(kind)
                ||SqlKind.NOT_EQUALS.equals(kind)
                ||SqlKind.IS_NOT_NULL.equals(kind)){
            return Boolean.TRUE;
        }else{
            return Boolean.FALSE;
        }
    }

    /**
     * 检查包含关键字并返回次数
     * @param str1
     * @param str2
     * @param counter
     * @return
     */
    private static int countStr(String str1, String str2, int counter) {
        if (str1.contains(str2)) {
            counter++;
            counter = countStr(str1.substring(str1.indexOf(str2) + str2.length()), str2, counter);
        }
        return counter;
    }

    /**
     * sql中特殊字符处理
     * @param str
     * @return
     */
    private static String sqlReplace(String str){
        List<String> list = new ArrayList<>();
        list.add("ASYMMETRIC");
        String all = "";
        if (StringUtils.isNotBlank(str)){
            for (String s : list) {
                all = str.replaceAll(s, "");
            }
        }
        return all;
    }

}
