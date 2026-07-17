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

package org.hkijena.jast.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class FrontendController {

//    /**
//     * Forwards all routes to FrontEnd except: '/', '/index.html', '/api', '/api/**'
//     * Required because of 'mode: history' usage in frontend routing
//     *
//     * @return the model and view
//     */
//    @RequestMapping(value = "{_:^(?!index\\.html|api).$}")
//    public String redirectApi() {
//        return "forward:/";
//    }

    /**
     * Forwards all routes to the frontend except those starting with '/api'.
     * This ensures frontend routes work with 'mode: history' in Vue.
     */
    @RequestMapping(value = "/{path:[^\\.]*}")
    public String forwardToFrontend() {
        return "forward:/";
    }
}
