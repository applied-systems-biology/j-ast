/*
 * Copyright (c) 2026.
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jast.utils;

import org.springframework.stereotype.Component;

@Component
public class PropertyLogger {
//    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyLogger.class);
//
//    @EventListener
//    public void handleContextRefresh(ContextRefreshedEvent event) {
//        final Environment env = event.getApplicationContext().getEnvironment();
//        LOGGER.info("====== Environment and configuration ======");
//        LOGGER.info("Active profiles: {}", Arrays.toString(env.getActiveProfiles()));
//        final MutablePropertySources sources = ((AbstractEnvironment) env).getPropertySources();
//        StreamSupport.stream(sources.spliterator(), false)
//                .filter(ps -> ps instanceof EnumerablePropertySource)
//                .map(ps -> ((EnumerablePropertySource<?>) ps).getPropertyNames())
//                .flatMap(Arrays::stream)
//                .distinct()
////                .filter(prop -> !(prop.contains("credentials") || prop.contains("password")))
//                .forEach(prop -> LOGGER.info("{}: {}", prop, env.getProperty(prop)));
//        LOGGER.info("===========================================");
//    }
}
