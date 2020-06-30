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

package com.ruikk.cloud.metrics.prometheus;

import com.alibaba.druid.stat.DruidStatManagerFacade;
import com.ruikk.cloud.metrics.prometheus.manager.DruidPrometheusManager;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({DruidStatManagerFacade.class, MetricsProperties.class})
@ConditionalOnBean(PrometheusMeterRegistry.class)
@EnableConfigurationProperties(MetricsProperties.class)
@AutoConfigureAfter(PrometheusMetricsExportAutoConfiguration.class)
public class AutoConfiguration {

    private DruidPrometheusManager druidPrometheusManager;

    public AutoConfiguration(
            MetricsProperties properties,
            PrometheusMeterRegistry registry
    ) {
        this.druidPrometheusManager = new DruidPrometheusManager(properties);
        this.druidPrometheusManager.registerCollector(registry);
    }

    @Bean
    DruidPrometheusManager druidPrometheusManager() {
        return druidPrometheusManager;
    }
}
