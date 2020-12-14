package com.codeup.capstone3dprinting.controllers;

import com.codeup.capstone3dprinting.models.File;
import com.codeup.capstone3dprinting.repos.FileRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
class FileController {

    // These two next steps are often called dependency injection, where we create a Repository instance and initialize it in the controller class constructor.
    private final FileRepository fileDao;

    public FileController(FileRepository fileDao) {
        this.fileDao = fileDao;
    }

    @GetMapping("/files")
    public String showAllFiles(Model model) {
    model.addAttribute("allEntries", fileDao.findAll());
        return "index";
    }
    @GetMapping("/files/{id}")
    public String showPost(@PathVariable long id, Model model) {
        File filedb = fileDao.getOne(id);
        model.addAttribute("file", filedb);
        model.addAttribute("user",filedb.getOwner());
        return "files/showFile";
    }
    @PostMapping("/files/{id}")
    public String postIndividual(@PathVariable long id, Model model) {
        File filedb = fileDao.getOne(id);
        model.addAttribute("file", filedb);
        model.addAttribute("user",filedb.getOwner());
        return "files/showFile";
    }
}