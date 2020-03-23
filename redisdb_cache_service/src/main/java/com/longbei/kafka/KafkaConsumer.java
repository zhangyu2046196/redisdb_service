package com.longbei.kafka;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.longbei.bean.Student;
import com.longbei.cache.domain.RedisKeyBean;
import com.longbei.cache.domain.RedisKeyConstant;
import com.longbei.cache.util.GenerateRedisKey;
import com.longbei.config.RedisCacheManager;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author zhangyu
 * @description kafka消费binlog
 * @date 2020/3/17 17:30
 */
@Component
public class KafkaConsumer {

    private static Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

    @Autowired
    private RedisCacheManager redisCacheManager;

    /**
     * 批量消费消息
     * @param records
     * @param ack
     */
    @KafkaListener(topics = "${topicName.topic1}", containerFactory = "batchFactory")
    public void listenBatch(List<ConsumerRecord<String, Message>> records, Acknowledgment ack) {
        batchConsumerMessage(records);

        ack.acknowledge();
    }

    /**
     * 批量消息处理
     *
     * @param records
     */
    public void batchConsumerMessage(List<ConsumerRecord<String, Message>> records) {
        records.forEach(record -> consumerMessage(record));
    }

    /**
     * 单条消息处理
     *
     * @param record
     */
    public void consumerMessage(ConsumerRecord<String, Message> record) {
        Message message = record.value();
        long batchId = message.getId();
        int size = message.getEntries().size();
        if (batchId == -1 || size == 0) {

        } else {
            //解析binlog
            analysisEntry(message.getEntries());
        }
    }

    /**
     * 解析binlog日志
     *
     * @param entrys
     */
    private void analysisEntry(List<CanalEntry.Entry> entrys) {
        for (CanalEntry.Entry entry : entrys) {
            String tableName = entry.getHeader().getTableName();//操作的表名

            if (entry.getEntryType() == CanalEntry.EntryType.ROWDATA) {
                CanalEntry.RowChange rowChage = null;
                try {
                    rowChage = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                } catch (Exception e) {
                    logger.error("解析binlog日志异常", e);
                }

                CanalEntry.EventType eventType = rowChage.getEventType();

                if (eventType == CanalEntry.EventType.QUERY || rowChage.getIsDdl()) {
                    logger.info(" 解析binlog是查询语句  sql ----> " + rowChage.getSql());
                    continue;
                }
                for (CanalEntry.RowData rowData : rowChage.getRowDatasList()) {
                    if (eventType == CanalEntry.EventType.DELETE) { //删除语句
                        spellersToRedis(rowData.getBeforeColumnsList(), true, tableName);
                    } else {
                        spellersToRedis(rowData.getAfterColumnsList(), false, tableName);
                    }
                }
            }
        }
    }

    /**
     * 解析binlog到redis
     *
     * @param columns   列
     * @param isDel     是否是delete操作
     * @param tableName 操作的表名
     */
    private void spellersToRedis(List<CanalEntry.Column> columns, boolean isDel, String tableName) {
        JSONObject jsonObject = new JSONObject();
        for (CanalEntry.Column column : columns) {
            jsonObject.put(column.getName(), column.getValue());
        }
        //key前缀
        String cacheKeyPrefix = "";
        //缓存的表字段对应的值
        String cacheColumnValue = "";
        //获取表名对应的key的前缀和需要缓存的列属性
        RedisKeyBean redisKeyBean = RedisKeyConstant.keyMap.get(tableName);
        if (null != redisKeyBean) {
            cacheColumnValue = (String) jsonObject.get(redisKeyBean.getKeyColumn());
            cacheKeyPrefix = redisKeyBean.getPrefixKey();
        }

        String redisKey = GenerateRedisKey.getKey(cacheKeyPrefix, cacheColumnValue);

        logger.info(" tableName:" + tableName + " value:" + jsonObject.toJSONString() + " key:" + redisKey + " isDel:" + isDel);

        if (isDel) {//删除
            redisCacheManager.del(redisKey);
        } else {
            redisCacheManager.set(redisKey, jsonObject.toJSONString());
        }

        if (redisCacheManager.hasKey(redisKey)) {
            Student student = JSON.parseObject(redisCacheManager.get(redisKey).toString(), Student.class);
            logger.info("student:" + student);
        }
    }

    /**
     * 监听topic1主题,单条消费
     */
//    @KafkaListener(topics = "${topicName.topic1}")
//    public void listen0(ConsumerRecord<String, Message> records) {
//        consumerMessage(records);
//    }

//    /**
//     * 监听topic2主题,单条消费
//     */
//    @KafkaListener(topics = "${topicName.topic2}")
//    public void listen1(ConsumerRecord<String, String> record) {
//        consumer(record);
//    }
//
//    /**
//     * 监听topic3和topic4,单条消费
//     */
//    @KafkaListener(topics = {"topic3", "topic4"})
//    public void listen2(ConsumerRecord<String, String> record) {
//        consumer(record);
//    }
//
//    /**
//     * 监听topic5,批量消费
//     */
//    @KafkaListener(topics = "${topicName.topic2}", containerFactory = "batchFactory")
//    public void listen2(List<ConsumerRecord<String, String>> records) {
//        batchConsumer(records);
//    }
//
//    /**
//     * 单条消费
//     */
//    public void consumer(ConsumerRecord<String, String> record) {
//        logger.debug("主题:{}, 内容: {}", record.topic(), record.value());
//    }


//    /**
//     * 批量消费
//     */
//    public void batchConsumer(List<ConsumerRecord<String, String>> records) {
//        records.forEach(record -> consumer(record));
//    }

}
