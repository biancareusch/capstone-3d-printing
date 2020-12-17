package com.codeup.capstone3dprinting.controllers;

import com.codeup.capstone3dprinting.models.*;
import com.codeup.capstone3dprinting.repos.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
class FileController {

    // These two next steps are often called dependency injection, where we create a Repository instance and initialize it in the controller class constructor.
    private final FileRepository fileDao;
    private final CommentRepository commentDao;
    private final UserRepository userDao;
    private final RatingRepository ratingDao;
    private final CategoryRepository categoryDao;

    public FileController(FileRepository fileDao, CommentRepository commentdao, UserRepository userdao, RatingRepository ratingDao, CategoryRepository categoryDao) {
        this.fileDao = fileDao;
        this.commentDao = commentdao;
        this.userDao = userdao;
        this.ratingDao = ratingDao;
        this.categoryDao = categoryDao;
    }

    @GetMapping("/files")
    public String showAllFiles(Model model, @RequestParam(required = false) String category) {
        if (category == null) {
            model.addAttribute("files", fileDao.findAll());
        } else {
            Category requestedCategory = categoryDao.findCategoryByCategory(category);
            model.addAttribute("requestedCategories", requestedCategory);
            model.addAttribute("files", fileDao.findByCategories(requestedCategory));
        }
        model.addAttribute("categories", categoryDao.findAll());
        return "index";
    }

    public List<Integer> getRatingsList(List<Rating> ListOfRatingObjs) {
        List<Integer> ratingNumbers = new ArrayList<>();
        for (Rating rating :
                ListOfRatingObjs) {
            ratingNumbers.add(rating.getRating());
        }
        return ratingNumbers;
    }

    @GetMapping("/files/{id}")
    public String showPost(@PathVariable long id, Model model) {
        File filedb = fileDao.getOne(id);
        List<Comment> thisFilesComments = commentDao.getAllByFile_Id(id);
        List<Rating> ListOfRatingObjs = ratingDao.getAllByFile_Id(id);
        List<Integer> thisFileRatings = getRatingsList(ListOfRatingObjs);
        double sum = 0;
        for (int i : thisFileRatings) {
            sum = sum + i;
        }
        sum = sum / thisFileRatings.size();
        model.addAttribute("averageRating", Math.round(sum));
        model.addAttribute("allCommentsForThisPost", thisFilesComments);
        model.addAttribute("file", filedb);
        model.addAttribute("user", filedb.getOwner());
        return "files/showFile";
    }

    @GetMapping("/files/create")
    public String viewCreateForm(Model model) {
        model.addAttribute("file", new File());
        return "files/createFile";
    }

    @PostMapping("/files/create")
    public String createPost(@ModelAttribute File fileToBeSaved) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = new User(user);
        Timestamp timestamp1 = new Timestamp(System.currentTimeMillis());
        fileToBeSaved.setCreatedAt(timestamp1);
        fileToBeSaved.setUpdatedAt(timestamp1);
        fileToBeSaved.setOwner(currentUser);
        File dbFile = fileDao.save(fileToBeSaved);
        return "redirect:/files/" + dbFile.getId();
    }

    @GetMapping("/files/{id}/edit")
    public String showEditForm(@PathVariable long id, Model model) {
        File filedb = fileDao.getOne(id);
        model.addAttribute("file", filedb);
        return "files/editFile";
    }

    @PostMapping("/files/{id}/edit")
    public String editFilePost(@PathVariable long id, @ModelAttribute File fileEdit) {
        File file = fileDao.getOne(id);
        file.setTitle(fileEdit.getTitle());
        file.setDescription(fileEdit.getDescription());
        file.setPrivate(fileEdit.isPrivate());
        file.setImgUrl(fileEdit.getImgUrl());
        fileDao.save(file);
        return "redirect:/files/" + file.getId();
    }

    //    TODO:should redirect to admin dashboard if admin
    @PostMapping("/files/{id}/flag")
    public String flagUser(@PathVariable long id) {
        File file = fileDao.getOne(id);
        file.setFlagged(true);
        fileDao.save(file);
        return "redirect:/files/" + file.getId();
    }

    @PostMapping("/files/{id}/delete")
    public String deleteFilePost(@PathVariable long id) {
        fileDao.deleteById(id);
//        TODO: redirect back to the list of your own file posts/ or admin dashboard if admin
        return "redirect:/files";
    }

    // user can only unflag as admin
    @PostMapping("/files/{id}/unflag")
    public String unflagUser(@PathVariable long id) {
        File file = fileDao.getOne(id);
        file.setFlagged(false);
        fileDao.save(file);
        return "redirect:/admin";
    }


    @PostMapping("files/{id}/comment")
    public String comment(@PathVariable long id, @RequestParam(name = "commentText") String commentText) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = new User(user);

        Comment newComment = new Comment();
        System.out.println("commentText = " + commentText);
        newComment.setComment(commentText);
        newComment.setCreatedAt(new Timestamp(new Date().getTime()));
        newComment.setFile(fileDao.getOne(id));
        newComment.setOwner(currentUser);
        commentDao.save(newComment);
        File file = fileDao.getOne(id);
        return "redirect:/files/" + file.getId();
    }

    @PostMapping("/files/{id}/comment/{commentId}/delete")
    public String deleteFilePost(@PathVariable long id, @RequestParam(name = "commentId") long commentId) {
        commentDao.deleteById(commentId);
        File file = fileDao.getOne(id);
        return "redirect:/files/" + file.getId();
    }

    @PostMapping("files/{id}/rating")
    public String rateFile(@PathVariable long id, @RequestParam(name = "ratings") int rating) {
        Rating newRating = new Rating();
        newRating.setRating(rating);
        newRating.setFile(fileDao.getOne(id));
        ratingDao.save(newRating);
        File file = fileDao.getOne(id);
        return "redirect:/files/" + file.getId();
    }
}

