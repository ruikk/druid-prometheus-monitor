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

package com.ruikk.cloud.metrics.prometheus.manager;

import com.ruikk.cloud.metrics.prometheus.collector.DruidCollector;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;

public class DruidPrometheusManager {

    private final MetricsProperties properties;

    private DruidCollector collector;

    public DruidPrometheusManager(MetricsProperties properties) {
        this.properties = properties;

        if(this.isEnable()) {
            this.collector = new DruidCollector(properties.getTags(), isEnable("druid-sql"), isEnable("druid-uri"));
        }
    }

    public void registerCollector(PrometheusMeterRegistry registry) {
        if(this.isEnable() && this.collector != null) {
            collector.register(registry.getPrometheusRegistry());
        }
    }

    public boolean isEnable(){
        return isEnable("druid");
    }

    public boolean isEnable(String key){
        return properties.getEnable().getOrDefault(key, true);
    }

    public DruidCollector getCollector() {
        return collector;
    }
}
