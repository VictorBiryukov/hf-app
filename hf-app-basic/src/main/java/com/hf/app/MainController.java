package com.hf.app;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MainController {

    @RequestMapping(path = "/check-student")
    public static String checkOrSaveStudentName(String name) {
        return "checkOrSaveStudentName";
    }

}
