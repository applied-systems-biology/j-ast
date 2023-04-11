package com.example.growthassayanalyzerwebapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.Comparator;

@Controller
public class IndexController {
    @GetMapping("/")
    public ModelAndView index(Model model) {
        return new ModelAndView("index");
    }
}
