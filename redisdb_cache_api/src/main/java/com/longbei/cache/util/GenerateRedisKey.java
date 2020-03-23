package com.longbei.cache.util;

import org.apache.commons.lang.StringUtils;

/**
 * @author zhangyu
 * @description  拼装redis key
 * @date 2020/3/18 10:22
 */
public class GenerateRedisKey {

    /**
     * 拼装key
     * @param cacheKeyPrefix  key前缀
     * @param columnValue  字段值
     * @return  返回创建key
     */
    public static String getKey(String cacheKeyPrefix, String columnValue) {
        if (StringUtils.isNotBlank(cacheKeyPrefix) && StringUtils.isNotBlank(columnValue)) {
            return cacheKeyPrefix + columnValue;
        }
        return null;
    }

}
