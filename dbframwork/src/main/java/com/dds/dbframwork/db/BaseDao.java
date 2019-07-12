package com.dds.dbframwork.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import com.dds.dbframwork.annotation.DbField;
import com.dds.dbframwork.annotation.DbTable;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by dds on 2019/7/12.
 * android_shuai@163.com
 */
public class BaseDao<T> implements IDao<T> {
    private final static String TAG = "dds_DaoImpl";
    private SQLiteDatabase mSqLiteDatabase;
    private String mTableName;
    private Class<T> mEntityClass;
    // 是否创建成功
    private boolean isInit = false;

    private HashMap<String, Field> cacheMap;

    public boolean init(SQLiteDatabase sqLiteDatabase, Class<T> entityClass) {
        mSqLiteDatabase = sqLiteDatabase;
        mEntityClass = entityClass;
        if (!isInit) {
            //取到表名
            if (entityClass.getAnnotation(DbTable.class) == null) {
                //反射到类名
                mTableName = entityClass.getSimpleName();
            } else {
                //取注解上的名字
                mTableName = entityClass.getAnnotation(DbTable.class).value();
            }
        }
        if (!sqLiteDatabase.isOpen()) {
            Log.e(TAG, "SQLiteDatabase is not open!");
            return false;
        }
        //执行建表
        String sql = generateCreateTableSql();
        Log.i(TAG, sql);
        sqLiteDatabase.execSQL(sql);

        //初始化缓存==列名和成员变量
        cacheMap = new HashMap<>();
        cacheRelationship();
        isInit = true;
        return false;
    }

    // 生成创建数据表语句
    private String generateCreateTableSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("create table if not exists ");
        sb.append(mTableName).append("(");
        //反射得到所有的成员变量
        Field[] fields = mEntityClass.getDeclaredFields();
        for (Field field : fields) {
            Class type = field.getType();
            if (field.getAnnotation(DbField.class) != null) {
                if (type == String.class) {
                    sb.append(field.getAnnotation(DbField.class).value()).append(" TEXT,");
                } else if (type == Integer.class) {
                    sb.append(field.getAnnotation(DbField.class).value()).append(" INTEGER,");
                } else if (type == Long.class) {
                    sb.append(field.getAnnotation(DbField.class).value()).append(" BIGINT,");
                } else if (type == Double.class) {
                    sb.append(field.getAnnotation(DbField.class).value()).append(" DOUBLE,");
                } else if (type == byte[].class) {
                    sb.append(field.getAnnotation(DbField.class).value()).append(" BLOB,");
                } else {
                    //不支持的类型
                    continue;
                }
            } else {
                if (type == String.class) {
                    sb.append(field.getName()).append(" TEXT,");
                } else if (type == Integer.class) {
                    sb.append(field.getName()).append(" INTEGER,");
                } else if (type == Long.class) {
                    sb.append(field.getName()).append(" BIGINT,");
                } else if (type == Double.class) {
                    sb.append(field.getName()).append(" DOUBLE,");
                } else if (type == byte[].class) {
                    sb.append(field.getName()).append(" BLOB,");
                } else {
                    //不支持的类型
                    continue;
                }
            }
        }
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append(")");
        return sb.toString();
    }


    // 缓存数据库字段--->加快查询速度
    private void cacheRelationship() {
        try {
            //1.取所有的列名====(查空表)
            String sql = "select * from " + mTableName + " limit 1,0";
            Cursor cursor = mSqLiteDatabase.rawQuery(sql, null);
            String[] columnNames = cursor.getColumnNames();
            //2.取所有的成员变量(反射)
            Field[] columnFields = mEntityClass.getDeclaredFields();
            //3.进行列名和成员变量的映射,存入到缓存中
            for (Field field : columnFields) {
                field.setAccessible(true);
            }
            for (String columnName : columnNames) {
                Field columnFiled = null;
                for (Field field : columnFields) {
                    String fieldName = null;
                    if (field.getAnnotation(DbField.class) != null) {
                        fieldName = field.getAnnotation(DbField.class).value();
                    } else {
                        fieldName = field.getName();
                    }
                    if (columnName.equals(fieldName)) {
                        columnFiled = field;
                        break;
                    }
                }
                if (columnFiled != null) {
                    cacheMap.put(columnName, columnFiled);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "cacheRelationship:" + e.toString());
        }


    }


    // 获取对应关系
    private Map<String, String> getValues(T entity) {
        HashMap<String, String> map = new HashMap<>();
        Iterator<Field> fieldIterator = cacheMap.values().iterator();
        while (fieldIterator.hasNext()) {
            Field field = fieldIterator.next();
            field.setAccessible(true);
            //获取成员变量的值
            try {
                Object object = field.get(entity);
                if (object == null) {
                    continue;
                }
                String value = object.toString();
                //获取列名
                String key;
                if (field.getAnnotation(DbField.class) != null) {
                    key = field.getAnnotation(DbField.class).value();
                } else {
                    key = field.getName();
                }
                if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                    map.put(key, value);
                }

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return map;
    }

       private ContentValues getContentValues(Map<String, String> map) {
        ContentValues contentValues = new ContentValues();
        Set keys = map.keySet();
        for (Object key1 : keys) {
            String key = (String) key1;
            String value = map.get(key);
            if (value != null) {
                contentValues.put(key, value);
            }
        }
        return contentValues;
    }

    @Override
    public long insert(T entity) {
        //准备好ContentValues中需要用的数据
        Map<String, String> map = getValues(entity);
        //设置插入的内容
        ContentValues values = getContentValues(map);
        //执行插入
        return mSqLiteDatabase.insert(mTableName, null, values);
    }

    @Override
    public int update(T entity, T where) {
        Map values = getValues(entity);
        ContentValues contentValues = getContentValues(values);
        Map whereCause = getValues(where);
        Condition condition = new Condition(whereCause);
        return mSqLiteDatabase.update(mTableName, contentValues, condition.whereCause, condition.whereArgs);
    }

    @Override
    public int delete(T where) {
        return 0;
    }

    @Override
    public List<T> query(T where) {
        return null;
    }
}
