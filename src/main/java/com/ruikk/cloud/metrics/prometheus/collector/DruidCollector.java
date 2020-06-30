/**
 * The MIT License
 * Copyright (c) 2019 Brent
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ruikk.cloud.metrics.prometheus.collector;

import com.alibaba.druid.stat.DruidDataSourceStatManager;
import com.alibaba.druid.stat.DruidStatManagerFacade;
import com.alibaba.druid.support.http.stat.WebAppStatManager;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DruidCollector extends Collector {

    private final static String[] DRUID_METRICS_NAMES = {
            "WaitThreadCount", "NotEmptyWaitCount", "NotEmptyWaitMillis", "PoolingCount", "PoolingPeak", "ActiveCount",
            "ActivePeak", "InitialSize", "MinIdle", "MaxActive", "QueryTimeout", "TransactionQueryTimeout", "LoginTimeout",
            "LogicConnectCount", "LogicCloseCount", "LogicConnectErrorCount", "PhysicalConnectCount", "PhysicalCloseCount",
            "PhysicalConnectErrorCount", "ExecuteCount", "ErrorCount", "CommitCount", "RollbackCount", "PSCacheAccessCount",
            "PSCacheHitCount", "PSCacheMissCount", "StartTransactionCount", "ClobOpenCount", "BlobOpenCount", "KeepAliveCheckCount",
            "MaxWait", "MaxWaitThreadCount", "MaxPoolPreparedStatementPerConnectionSize", "RecycleErrorCount",
            "PreparedStatementOpenCount", "PreparedStatementClosedCount", "ExecuteUpdateCount","ExecuteQueryCount","ExecuteBatchCount",
    };

    private final static String[] DRUID_METRICS_HISTOGRAM_NAMES = {"TransactionHistogram", "ConnectionHoldTimeHistogram"};

    private final static String[] DRUID_METRICS_SQL_NAMES = {"ExecuteCount", "FetchRowCount", "TotalTime", "MaxTimespan", "RunningCount", "ErrorCount", "ConcurrentMax"};

    private final static String[] DRUID_METRICS_SQL_HISTOGRAM_NAMES = {"Histogram", "FetchRowCountHistogram", "EffectedRowCountHistogram", "ExecuteAndResultHoldTimeHistogram"};

    private final static String[] DRUID_METRICS_URI_NAMES = {"RequestCount", "RequestTimeMillisMax", "RequestTimeMillis", "RunningCount", "ConcurrentMax", "JdbcExecuteTimeMillis", "JdbcExecuteCount", "JdbcExecuteErrorCount"};

    private final static String[] DRUID_METRICS_URI_HISTOGRAM_NAMES = {"Histogram"};

    private final static String SQL_NAME_LAST_ERROR_TIME = "LastErrorTime";

    private final String[] BUCKETS = {"1ms", "10ms", "100ms", "1s", "10s", "100s", "Inf"};

    private final static Pattern PATTERN_CAMEL = Pattern.compile("(?<=[a-z])(?=[A-Z])");

    private final static Function<String, String> CAMEL_TO_SNAKE =
            str -> Stream.of(PATTERN_CAMEL.split(str)).filter(s-> s != null && !s.isEmpty()).map(String::toLowerCase).collect(Collectors.joining("_"));

    private final List<String> LABEL_NAMES;
    private final List<String> LABEL_HISTOGRAM_NAMES;
    private final List<String> LABEL_SQL_NAMES;
    private final List<String> LABEL_SQL_HISTOGRAM_NAMES;
    private final List<String> LABEL_SQL_ERROR_NAMES;
    private final List<String> LABEL_URI_NAMES;
    private final List<String> LABEL_URI_HISTOGRAM_NAMES;

    private final Function<Map<String, Object>, List<String>> LABEL_VALUES_FUNCTION;
    private final BiFunction<Map<String, Object>, String, List<String>> LABEL_HISTOGRAM_VALUES_FUNCTION;
    private final Function<Map<String, Object>, List<String>> LABEL_SQL_VALUES_FUNCTION;
    private final Function<Map<String, Object>, List<String>> LABEL_SQL_ERROR_VALUES_FUNCTION;
    private final BiFunction<Map<String, Object>, String, List<String>> LABEL_SQL_HISTOGRAM_VALUES_FUNCTION;
    private final Function<Map<String, Object>, List<String>> LABEL_URI_VALUES_FUNCTION;
    private final BiFunction<Map<String, Object>, String, List<String>> LABEL_URI_HISTOGRAM_VALUES_FUNCTION;

    private boolean enableSql;
    private boolean enableUri;

    public DruidCollector(Map<String, String> tags, boolean enableSql, boolean enableUri) {
        this.enableSql = enableSql;
        this.enableUri = enableUri;

        Set<Map.Entry<String, String>> tagEntry = tags.entrySet();

        List<String> tagKey = tagEntry.stream().map(Map.Entry::getKey).collect(Collectors.toList());
        List<String> tagVal = tagEntry.stream().map(Map.Entry::getValue).collect(Collectors.toList());

        LABEL_NAMES = Stream.concat(tagKey.stream(), Stream.of("pool")).collect(Collectors.toList());
        LABEL_HISTOGRAM_NAMES = Stream.concat(tagKey.stream(), Stream.of("pool", "le")).collect(Collectors.toList());

        LABEL_SQL_NAMES = Stream.concat(tagKey.stream(), Stream.of("pool", "sql")).collect(Collectors.toList());
        LABEL_SQL_HISTOGRAM_NAMES = Stream.concat(tagKey.stream(), Stream.of("pool", "sql", "le")).collect(Collectors.toList());
        LABEL_SQL_ERROR_NAMES = Stream.concat(tagKey.stream(), Stream.of("pool", "sql", "class", "message")).collect(Collectors.toList());

        LABEL_URI_NAMES = Stream.concat(tagKey.stream(), Stream.of("uri")).collect(Collectors.toList());
        LABEL_URI_HISTOGRAM_NAMES = Stream.concat(tagKey.stream(), Stream.of("uri", "le")).collect(Collectors.toList());

        // func
        LABEL_VALUES_FUNCTION = (map) -> Stream.concat(tagVal.stream(), Stream.of((String) map.get("Name"))).collect(Collectors.toList());
        LABEL_HISTOGRAM_VALUES_FUNCTION = (map, le) -> Stream.concat(tagVal.stream(), Stream.of((String) map.get("Name"), le)).collect(Collectors.toList());

        LABEL_SQL_VALUES_FUNCTION = (map) -> Stream.concat(tagVal.stream(), Stream.of((String) map.get("Name"), (String) map.get("SQL"))).collect(Collectors.toList());
        LABEL_SQL_HISTOGRAM_VALUES_FUNCTION = (map, le) -> Stream.concat(tagVal.stream(), Stream.of((String) map.get("Name"), (String) map.get("SQL"), le)).collect(Collectors.toList());
        LABEL_SQL_ERROR_VALUES_FUNCTION = (map) -> Stream.concat(tagVal.stream(), Stream.of("Name", "SQL", "LastErrorClass", "LastErrorMessage").map(k-> (String) map.get(k))).collect(Collectors.toList());

        LABEL_URI_VALUES_FUNCTION = (map) -> Stream.concat(tagVal.stream(), Stream.of((String) map.get("URI"))).collect(Collectors.toList());
        LABEL_URI_HISTOGRAM_VALUES_FUNCTION = (map, le) -> Stream.concat(tagVal.stream(), Stream.of((String) map.get("URI"), le)).collect(Collectors.toList());
    }

    @Override
    public List<MetricFamilySamples> collect() {

        DruidStatManagerFacade druidStatManagerFacade = DruidStatManagerFacade.getInstance();
        List<Map<String, Object>> statList = druidStatManagerFacade.getDataSourceStatDataList();

        List<Map<String, Object>> sqlList = getSqlStatData();
        List<Map<String, Object>> uriList = getUriStatData();

        int initialCapacity =
                statList.size() * (DRUID_METRICS_NAMES.length + DRUID_METRICS_HISTOGRAM_NAMES.length) +
                sqlList.size() * (DRUID_METRICS_SQL_NAMES.length + DRUID_METRICS_SQL_HISTOGRAM_NAMES.length + 1) +
                uriList.size() * (DRUID_METRICS_URI_NAMES.length + DRUID_METRICS_URI_HISTOGRAM_NAMES.length);

        List<MetricFamilySamples> list = new ArrayList<>(initialCapacity);

        Stream.of(DRUID_METRICS_NAMES)
                .map(name -> createGauge(name, statList, m -> (Number) m.get(name))).forEach(list::add);
        Stream.of(DRUID_METRICS_HISTOGRAM_NAMES)
                .map(name -> createHistogram(name, statList, m -> (long[]) m.get(name))).forEach(list::add);

        if(enableSql) {
            Stream.of(DRUID_METRICS_SQL_NAMES)
                    .map(name -> createSqlGauge(name, sqlList, m -> (Number) m.get(name))).forEach(list::add);
            Stream.of(DRUID_METRICS_SQL_HISTOGRAM_NAMES)
                    .map(name -> createSqlHistogram(name, sqlList, m -> (long[]) m.get(name))).forEach(list::add);

            list.add(createSqlErrorGauge(SQL_NAME_LAST_ERROR_TIME, sqlList, m -> (Date) m.get(SQL_NAME_LAST_ERROR_TIME)));
        }

        if(enableUri) {
            Stream.of(DRUID_METRICS_URI_NAMES)
                    .map(name -> createUriGauge(name, uriList, m -> (Number) m.get(name))).forEach(list::add);
            Stream.of(DRUID_METRICS_URI_HISTOGRAM_NAMES)
                    .map(name -> createUriHistogram(name, uriList, m -> (long[]) m.get(name))).forEach(list::add);
        }

        return list;
    }

    private List<Map<String, Object>> getSqlStatData() {
        if(!enableSql){
            return Collections.emptyList();
        }
        DruidStatManagerFacade druidStatManagerFacade = DruidStatManagerFacade.getInstance();
        Set<Object> dataSources = DruidDataSourceStatManager.getInstances().keySet();
        return dataSources.stream().flatMap(obj -> druidStatManagerFacade.getSqlStatDataList(obj).stream()).collect(Collectors.toList());
    }

    private List<Map<String, Object>> getUriStatData() {
        if(!enableUri){
            return Collections.emptyList();
        }
        WebAppStatManager webAppStatManager = WebAppStatManager.getInstance();
        return webAppStatManager.getURIStatData();
    }

    private GaugeMetricFamily createGauge(String metric, List<Map<String, Object>> list, Function<Map<String, Object>, Number> metricValueFunc) {
        GaugeMetricFamily metricFamily = new GaugeMetricFamily("druid_" + CAMEL_TO_SNAKE.apply(metric), "Druid " + metric, LABEL_NAMES);
        list.forEach((m) -> metricFamily.addMetric(LABEL_VALUES_FUNCTION.apply(m), metricValueFunc.apply(m).doubleValue()));
        return metricFamily;
    }

    private GaugeMetricFamily createHistogram(String metric, List<Map<String, Object>> list, Function<Map<String, Object>, long[]> metricValueFunc) {
        GaugeMetricFamily metricFamily = new GaugeMetricFamily("druid_" + CAMEL_TO_SNAKE.apply(metric) + "_bucket", "Druid " + metric, LABEL_HISTOGRAM_NAMES);

        list.forEach((m) -> {
            long[] data = metricValueFunc.apply(m);
            for (int i = 0; i < data.length && i < BUCKETS.length; i++) {
                metricFamily.addMetric(LABEL_HISTOGRAM_VALUES_FUNCTION.apply(m, BUCKETS[i]), data[i]);
            }
        });
        return metricFamily;
    }

    private GaugeMetricFamily createSqlGauge(String metric, List<Map<String, Object>> list, Function<Map<String, Object>, Number> metricValueFunc) {
        GaugeMetricFamily metricFamily = new GaugeMetricFamily("druid_sql_" + CAMEL_TO_SNAKE.apply(metric), "Druid SQL " + metric, LABEL_SQL_NAMES);
        list.forEach((m) -> metricFamily.addMetric(LABEL_SQL_VALUES_FUNCTION.apply(m), metricValueFunc.apply(m).doubleValue()));
        return metricFamily;
    }

    private GaugeMetricFamily createSqlErrorGauge(String metric, List<Map<String, Object>> list, Function<Map<String, Object>, Date> metricValueFunc) {
        GaugeMetricFamily metricFamily = new GaugeMetricFamily("druid_sql_" + CAMEL_TO_SNAKE.apply(metric), "Druid SQL " + metric, LABEL_SQL_ERROR_NAMES);
        list.forEach((m) -> {
            Date date = metricValueFunc.apply(m);
            if(date != null) {
                metricFamily.addMetric(LABEL_SQL_ERROR_VALUES_FUNCTION.apply(m), date.getTime());
            }
        });
        return metricFamily;
    }

    private GaugeMetricFamily createSqlHistogram(String metric, List<Map<String, Object>> list, Function<Map<String, Object>, long[]> metricValueFunc) {
        GaugeMetricFamily metricFamily = new GaugeMetricFamily("druid_sql_" + CAMEL_TO_SNAKE.apply(metric) + "_bucket", "Druid SQL " + metric, LABEL_SQL_HISTOGRAM_NAMES);

        list.forEach((m) -> {
            long[] data = metricValueFunc.apply(m);
            for (int i = 0; i < data.length && i < BUCKETS.length; i++) {
                metricFamily.addMetric(LABEL_SQL_HISTOGRAM_VALUES_FUNCTION.apply(m, BUCKETS[i]), data[i]);
            }
        });
        return metricFamily;
    }

    private GaugeMetricFamily createUriGauge(String metric, List<Map<String, Object>> list, Function<Map<String, Object>, Number> metricValueFunc) {
        GaugeMetricFamily metricFamily = new GaugeMetricFamily("druid_uri_" + CAMEL_TO_SNAKE.apply(metric), "Druid URI " + metric, LABEL_URI_NAMES);
        list.forEach((m) -> metricFamily.addMetric(LABEL_URI_VALUES_FUNCTION.apply(m), metricValueFunc.apply(m).doubleValue()));
        return metricFamily;
    }

    private GaugeMetricFamily createUriHistogram(String metric, List<Map<String, Object>> list, Function<Map<String, Object>, long[]> metricValueFunc) {
        GaugeMetricFamily metricFamily = new GaugeMetricFamily("druid_uri_" + CAMEL_TO_SNAKE.apply(metric) + "_bucket", "Druid URI " + metric, LABEL_URI_HISTOGRAM_NAMES);

        list.forEach((m) -> {
            long[] data = metricValueFunc.apply(m);
            for (int i = 0; i < data.length && i < BUCKETS.length; i++) {
                metricFamily.addMetric(LABEL_URI_HISTOGRAM_VALUES_FUNCTION.apply(m, BUCKETS[i]), data[i]);
            }
        });
        return metricFamily;
    }

    public boolean isEnableSql() {
        return enableSql;
    }

    public void setEnableSql(boolean enableSql) {
        this.enableSql = enableSql;
    }

    public boolean isEnableUri() {
        return enableUri;
    }

    public void setEnableUri(boolean enableUri) {
        this.enableUri = enableUri;
    }
}
