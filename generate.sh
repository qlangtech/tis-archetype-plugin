mvn com.qlangtech.tis:tis-archetype-generate-plugin:3.8.0:generate -Drat.skip=true  \
-Dtis.version=3.8.0 \
-Dtis.extendpoint="com.qlangtech.tis.plugin.incr.TISSinkFactory:DB2SinkFactory;com.qlangtech.tis.async.message.client.consumer.impl.MQListenerFactory:FlinkCDCDB2SourceFactory" \
-Dtis.artifactId=tis-flink-db4-plugin
