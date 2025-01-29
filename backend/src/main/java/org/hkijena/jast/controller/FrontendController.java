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
