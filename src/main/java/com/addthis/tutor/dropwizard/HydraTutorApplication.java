/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.tutor.dropwizard;


import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class HydraTutorApplication extends Application<HydraTutorConfiguration> {

    public static void main(String[] args) throws Exception {
        new HydraTutorApplication().run(args);
    }

    @Override
    public String getName() {
        return "hydra-tutor";
    }

    @Override
    public void initialize(Bootstrap<HydraTutorConfiguration> bootstrap) {
        bootstrap.addBundle(new LogbackConfigurer());
    }

    @Override
    public void run(HydraTutorConfiguration configuration,
            Environment environment) {
        environment.jersey().register(new FilterTutorResource(configuration));
        environment.jersey().register(new AssetsResource(configuration));
    }

}