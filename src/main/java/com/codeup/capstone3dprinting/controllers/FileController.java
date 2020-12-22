package com.codeup.capstone3dprinting.controllers;

import com.codeup.capstone3dprinting.models.*;
import com.codeup.capstone3dprinting.repos.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.awt.font.ImageGraphicAttribute;
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
    private final SettingRepository settingDao;
    private final MessageRepository messageDao;
    private final ImagesRepository imageDao;

    public FileController(FileRepository fileDao, CommentRepository commentdao, UserRepository userdao, RatingRepository ratingDao, CategoryRepository categoryDao, SettingRepository settingDao, MessageRepository messageDao, ImagesRepository imageDao) {
        this.fileDao = fileDao;
        this.commentDao = commentdao;
        this.userDao = userdao;
        this.ratingDao = ratingDao;
        this.categoryDao = categoryDao;
        this.settingDao = settingDao;
        this.messageDao = messageDao;
        this.imageDao = imageDao;
    }

    @GetMapping("/files")
    public String showAllFiles(Model model, @RequestParam(required = false) String category) {
        if (category == null) {
            model.addAttribute("files", fileDao.findAll());
            model.addAttribute("pageTitle", "All Files");
        } else {
            Category requestedCategory = categoryDao.findCategoryByCategory(category);
            model.addAttribute("requestedCategories", requestedCategory);
            model.addAttribute("files", fileDao.findByCategories(requestedCategory));
            model.addAttribute("pageTitle", requestedCategory.getCategory());
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
        List<FileImage> thisFilesImgs = imageDao.getAllByFile_Id(id);
        //comments
        List<Comment> thisFilesComments = commentDao.getAllByFile_Id(id);
        //Rating
        List<Rating> ListOfRatingObjs = ratingDao.getAllByFile_Id(id);
        List<Integer> thisFileRatings = getRatingsList(ListOfRatingObjs);
        double sum = 0;
        for (int i : thisFileRatings) {
            sum = sum + i;
        }
        sum = sum / thisFileRatings.size();
//            favoring files
        boolean favorited = false;
        if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof User) {
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User currentUser = new User(user);
            for (File f : currentUser.getFavoriteFiles()) {
                if (f.getId() == id) {
                    favorited = true;
                    break;
                }
            }

        }
        model.addAttribute("favorited", favorited);
        model.addAttribute("imgFiles", thisFilesImgs);
        model.addAttribute("averageRating", Math.round(sum));
        model.addAttribute("allCommentsForThisPost", thisFilesComments);
        model.addAttribute("file", filedb);
        model.addAttribute("user", filedb.getOwner());
        return "files/showFile";
    }

    @GetMapping("/files/create")
    public String viewCreateForm(Model model) {
        System.out.println("SecurityContextHolder.getContext().getAuthentication().getPrincipal() = " + SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = new User(user);
        System.out.println("currentUser = " + currentUser);
        for (User follower : currentUser.getFollowers()) {
            System.out.println("follower.getId() = " + follower.getId());
        }

        model.addAttribute("file", new File());
            return "files/createFile";
    }

    @PostMapping("/files/create")
    public String createPost(@ModelAttribute File fileToBeSaved) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userDao.getOne(user.getId());
        Timestamp timestamp1 = new Timestamp(System.currentTimeMillis());
        fileToBeSaved.setCreatedAt(timestamp1);
        fileToBeSaved.setUpdatedAt(timestamp1);
        fileToBeSaved.setOwner(currentUser);

        for (User follower : currentUser.getFollowers()) {
            User receiver = userDao.getOne(follower.getId());

            if (receiver.getSettings().contains(settingDao.getOne(1L))) {

                Message newMessage = new Message(currentUser.getUsername() + " has posted a new file!",
                        new Timestamp(new Date().getTime()), receiver, userDao.getOne(1L));
                messageDao.save(newMessage);
            }
        }

        File dbFile = fileDao.save(fileToBeSaved);
        return "redirect:/files/" + dbFile.getId();
    }

    @GetMapping("/files/{id}/edit")
    public String showEditForm(@PathVariable long id, Model model) {
        File filedb = fileDao.getOne(id);
        List<FileImage> thisFilesImgs = imageDao.getAllByFile_Id(id);
        FileImage newImg = new FileImage();
        model.addAttribute("newImg", newImg);
        model.addAttribute("filesImgs", thisFilesImgs);
        model.addAttribute("file", filedb);
        return "files/editFile";
    }

    @PostMapping("/files/{id}/edit")
    public String editFilePost(@PathVariable long id, @ModelAttribute File fileEdit) {
        File file = fileDao.getOne(id);
        file.setTitle(fileEdit.getTitle());
        file.setDescription(fileEdit.getDescription());
        file.setPrivate(fileEdit.isPrivate());
        fileDao.save(file);
        return "redirect:/files/" + id + "/edit";
    }

    @PostMapping("/files/{id}/addImg")
    public String addImgToFile(@PathVariable long id, @RequestParam(name = "newImg") String imgURL) {
        File file = fileDao.getOne(id);

        FileImage newImg = new FileImage(fileDao.getOne(id), imgURL);

        file.addImg(newImg);
        fileDao.save(file);
        return "redirect:/files/" + id + "/edit";
    }

    @PostMapping("/files/{id}/removeImg/{imgID}")
    public String removeImgfromFile(@PathVariable long id, @RequestParam(name = "imgID") long imgID) {
        File file = fileDao.getOne(id);
        file.removeImg(imageDao.getOne(imgID));
        fileDao.save(file);
        System.out.println("deleted img with imgID = " + imgID);
        return "redirect:/files/" + id + "/edit";
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
        User currentUser = userDao.getOne(user.getId());
        File file = fileDao.getOne(id);
        User fileOwner = userDao.getOne(file.getOwner().getId());

        if (fileOwner.getSettings().contains(settingDao.getOne(3L))) {
            Message newMessage = new Message(currentUser.getUsername() + " has commented on your file: " + file.getTitle(),
                    new Timestamp(new Date().getTime()), fileOwner, userDao.getOne(1L));
            messageDao.save(newMessage);
        }

        Comment newComment = new Comment();
        newComment.setComment(commentText);
        newComment.setCreatedAt(new Timestamp(new Date().getTime()));
        newComment.setFile(fileDao.getOne(id));
        newComment.setOwner(currentUser);
        commentDao.save(newComment);


        return "redirect:/files/" + file.getId();
    }

    @PostMapping("/files/{id}/comment/{commentId}/delete")
    public String deleteComment(@PathVariable long id, @RequestParam(name = "commentId") long commentId) {
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

    @PostMapping("files/favorite/{id}")
    public String favoritePost(@PathVariable long id,
                               @RequestParam(name = "favorited") boolean favorited) {

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = new User(user);
        File thisFile = fileDao.getOne(id);
        System.out.println("favorited = " + favorited);

        if (favorited) {
            currentUser.getFavoriteFiles().removeIf(n -> n.getId() == id);

        } else {
            currentUser.getFavoriteFiles().add(thisFile);
        }
        userDao.save(currentUser);
        return "redirect:/files/" + id;
    }
    @PostMapping("files/search")
    public String search(@RequestParam(name = "search")String searchTerm, Model model){
        List<File> searched = fileDao.findAllByDescriptionIsLike("%" + searchTerm + "%");
        List<File> searchedTitle = fileDao.findAllByTitleIsLike("%" + searchTerm + "%");
        for(File file : searchedTitle){
            if(!searched.contains(file)){
                searched.add(file);
            }
        }

        model.addAttribute("files", searched);
        model.addAttribute("categories", categoryDao.findAll());
        model.addAttribute("pageTitle", searched.size() + " Result" + (searched.size() == 1 ? "" : "s"));
        return "index";
    }
}
//select liker_id's where file_id like this.id
//save it in a list get list size, for amount of likes
