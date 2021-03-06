package com.intsmaze.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.Test;

import java.util.*;

/**
 * @author ：intsmaze
 * @date ：Created in 2020/2/5 12:41
 * @description： https://www.cnblogs.com/intsmaze/
 * <p>
 *  Kafka消费者不是线程安全的。所有网络I / O都发生在进行调用的应用程序线程中。
 *  用户有责任确保正确同步多线程访问，不同步的访问将导致ConcurrentModificationException。
 * @modified By：
 */
public class ModelConsumer {

    /**
     * @author intsmaze
     * @description: https://www.cnblogs.com/intsmaze/
     * 使用范例
     * 消费者API提供了覆盖各种消费用例的灵活性。 这里有一些示例来演示如何使用它们。
     * 自动偏移提交
     * 这个例子演示了Kafka的消费者api的简单用法，它依赖于自动偏移量提交。
     * @date : 2020/2/5 12:41
     */
    @Test
    public void helloWord() {

        //通过使用配置>bootstrap.servers指定要连接的一个或多个代理的列表，可以引导到群集的连接。
        // 此列表仅用于发现群集中的其余代理，而不必是群集中服务器的详尽列表（尽管您可能要指定多个服务器，以防客户端连接时服务器停机）。
        Properties props = new Properties();
        props.put("bootstrap.servers", "192.168.19.201:9092");
        props.put("group.id", "test");
        //设置enable.auto.commit表示偏移量将以配置auto.commit.interval.ms控制的频率自动提交。
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");

        //解串器设置指定如何将字节转换为对象。 例如，通过指定字符串反序列化器，我们说记录的键和值将只是简单的字符串
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("kafka-test"));
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                System.out.printf("paririon = %d,offset = %d, key = %s, value = %s%n",record.partition(),record.offset(), record.key(), record.value());
            }
        }
    }

    /**
     * @author intsmaze
     * @description: https://www.cnblogs.com/intsmaze/
     * 用户也可以控制何时应将记录视为已消费并因此提交其偏移量，而不是依赖于消费者定期提交消耗的偏移量。
     * 当消息的消耗与某些处理逻辑结合在一起时，这很有用，因此，在完成处理之前，不应将消息视为已消耗。
     * <p>
     * <p>
     * 在此示例中，我们将消费一批记录并将其批处理到内存中。当我们有足够的批处理记录时，我们会将它们插入数据库中。
     * 如果像上一个示例一样允许偏移量自动提交，则在一个周期提交轮询中将记录返回给用户之后，记录将被视为已消耗。在批处理记录之后但在将它们插入数据库之前，我们的过程可能会失败。
     * 为避免这种情况，我们仅在将相应的记录插入数据库后才手动提交偏移量。这使我们可以精确控制何时将记录视为已消耗。
     * 这就提出了相反的可能性：该过程可能会在插入数据库后但在提交之前的间隔内失败（即使这可能只是几毫秒，这也是有可能的）。
     * 在这种情况下，尽管消耗的过程将从上次提交的偏移量中消耗数据，并将重复最后一批数据的插入。
     * 通过这种方式，Kafka提供了通常称为“at-least-once”的交付保证，因为每条记录可能会交付一次，但在失败的情况下可以重复。
     * <p>
     * <p>
     * 注意：使用自动偏移量提交也可以实现“at-least-once”，但是要求是您必须在每次后续调用之前或关闭消费者者之前使用每次调用poll（long）返回的所有数据。
     * 如果您不执行任何一个操作，则已提交的偏移量有可能超过消耗的位置，从而导致记录丢失。 使用手动偏移量控制的优点是您可以直接控制何时将记录视为“消耗”。
     * @date : 2020/2/5 12:45
     */
    @Test
    public void manualOffsetControl() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "192.168.19.201:9092");
        props.put("group.id", "test");

        props.put("enable.auto.commit", "false");

        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("kafka-test"));

        final int minBatchSize = 10;
        List<ConsumerRecord<String, String>> buffer = new ArrayList<>();
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                System.out.printf("paririon = %d,offset = %d, key = %s, value = %s%n",record.partition(),record.offset(), record.key(), record.value());
                buffer.add(record);
            }
            if (buffer.size() >= minBatchSize) {

//                insertIntoDb(buffer);

                //手动提交offset的方法有两种：分别是commitSync（同步提交）和commitAsync（异步提交）。两者的相同点是，都会将本次poll的一批数据最高的偏移量提交；
                // 不同点是，commitSync会失败重试，一直到提交成功（如果由于不可恢复原因导致，也会提交失败）；而commitAsync则没有失败重试机制，故有可能提交失败。
                consumer.commitSync();//使用commitSync将所有接收到的记录标记为已提交。
//                consumer.commitAsync();
                buffer.clear();
            }
        }
    }

    /**
     * @author intsmaze
     * @description: https://www.cnblogs.com/intsmaze/
     * * 在某些情况下，您可能希望通过显式指定偏移量来更好地控制已提交的记录。
     * * 在下面的示例中，我们在处理完每个分区中的记录后提交偏移量。
     * <p>
     * 注意：提交的偏移量应始终是应用程序将读取的下一条消息的偏移量。 因此，在调用commitSync（offsets）时，应在最后处理的消息的偏移量上添加一个。
     * @date : 2020/2/5 12:52
     */
    @Test
    public void manualOffsetPartitionControl() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "192.168.19.201:9092");
        props.put("group.id", "test");
        props.put("enable.auto.commit", "false");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("kafka-test"));
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Long.MAX_VALUE);
//                for (TopicPartition partition : records.partitions()) {
//                    List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);
//                    for (ConsumerRecord<String, String> record : partitionRecords) {
//                        System.out.println(record.offset() + ": " + record.value());
//                    }
            for (ConsumerRecord<String, String> record : records) {
                System.out.println(record.offset() + ": " + record.value());
                TopicPartition topicPartition = new TopicPartition("kafka-test", 0);
//                    long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
                consumer.commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(27)));
            }
//                }
            }
        } finally {
            consumer.close();
        }
    }

    /**
     * @author intsmaze
     * @description: https://www.cnblogs.com/intsmaze/
     * @date : 2020/2/5 20:23
     *  手动订阅指定分区
     *
     *  在某些情况下，您可能需要更好地控制分配的特定分区。 例如：
     *  in some cases you may need finer control over the specific partitions that are assigned. For example:
     * If the process is maintaining some kind of local state associated with that partition (like a local on-disk key-value store), then it should only get records for the partition it is maintaining on disk.
     * If the process itself is highly available and will be restarted if it fails (perhaps using a cluster management framework like YARN, Mesos, or AWS facilities, or as part of a stream processing framework). In this case there is no need for Kafka to detect the failure and reassign the partition since the consuming process will be restarted on another machine.
     * To use this mode, instead of subscribing to the topic using subscribe, you just call assign(Collection) with the full list of partitions that you want to consume.
     */
    @Test
    public void subscribePartitionControl() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "192.168.19.201:9092");
        props.put("group.id", "test");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);


        //分配后，您可以像前面的示例一样循环调用poll来消费记录。 消费指定的组仍用于提交偏移量，但是现在分区组将仅随着另一个分配调用而更改。
        // 手动分区分配不使用消费者组协调，因此消费者发生故障不会导致重新分配分配的分区。
        // 即使每个消费者与另一个消费者共享一个groupId，每个消费者也可以独立行动。
        // 为避免偏移提交冲突，通常应确保groupId对于每个消费者实例都是唯一的。
        //请注意，不可能通过主题订阅（即使用subscribe）将手动分区分配（即使用assign）与动态分区分配混合在一起。
        String topic = "kafka-test";
        TopicPartition partition0 = new TopicPartition(topic, 1);
//        TopicPartition partition1 = new TopicPartition(topic, 1);
        consumer.assign(Arrays.asList(partition0));

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(100);
                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
                }
            }
        } finally {
            consumer.close();
        }
    }


    /**
     * @author intsmaze
     * @description: https://www.cnblogs.com/intsmaze/
     * 事务是在Kafka 0.11.0中引入的，其中应用程序可以原子地写入多个主题和分区。为了使读取事务消息起作用，消费者从这些分区读取消息时，应该在消费者端配置为仅读取已提交的数据。
     * 这可以通过在消费者者的配置中设置isolation.level = read_committed来实现。
     *
     *
     * 在read_committed模式下，消费者将仅读取已成功提交的那些事务性消息。它将像以前一样继续读取非事务性消息。
     * @date : 2020/2/5 20:33
     *
     */
    @Test
    public void readingTransactionalMessages() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "192.168.19.201:9092");
        props.put("group.id", "test");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");


        props.put("isolation.level", "read_committed");


        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("kafka-test"));
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(100);
                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
                }
            }
        } finally {
            consumer.close();
        }
    }


}