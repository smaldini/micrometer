/**
 * Copyright 2017 Pivotal Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.spring.binder;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.spring.SpringMeters;
import io.micrometer.spring.export.prometheus.EnablePrometheusMetrics;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collection;

/**
 * @author Jon Schneider
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = { "spring.datasource.generate-unique-name=true", "management.security.enabled=false" })
public class DataSourceMetricsTest {

    @Autowired
    DataSource dataSource;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    public void dataSourceIsInstrumented() throws SQLException, InterruptedException {
        dataSource.getConnection().getMetaData();
        String scrape = restTemplate.getForObject("/prometheus", String.class);
        System.out.println(scrape);
    }

    @SpringBootApplication(scanBasePackages = "isolated")
    @EnablePrometheusMetrics
    @Import(DataSourceConfig.class)
    static class MetricsApp {
        public static void main(String[] args) {
            SpringApplication.run(MetricsApp.class, "--debug");
        }
    }

    @Configuration
    static class DataSourceConfig {
        public DataSourceConfig(DataSource dataSource,
                                Collection<DataSourcePoolMetadataProvider> metadataProviders,
                                MeterRegistry registry) {
            SpringMeters.monitor(
                    registry,
                    dataSource,
                    metadataProviders,
                "data_source");
        }
    }
}
