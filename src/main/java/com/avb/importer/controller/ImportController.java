package com.avb.importer.controller;

import com.avb.importer.model.ImportResult;
import com.avb.importer.service.ImportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/import")
    public String doImport(@RequestParam("file") MultipartFile file, Model model) {
        ImportResult result = importService.importFile(file);
        model.addAttribute("result", result);
        return "index";
    }
}