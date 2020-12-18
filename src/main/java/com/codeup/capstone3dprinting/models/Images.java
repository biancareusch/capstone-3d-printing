package com.codeup.capstone3dprinting.models;
import javax.persistence.*;

@Entity
@Table(name="file_imgs")
public class Images {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "file_id")
    private File file;

    @Column(length=255)
    private String img_url;

    public Images(){}

    public Images(long id, File file, String img_url){
        this.id = id;
        this.file = file;
        this.img_url = img_url;
    }

    public Images(File file, String img_url){
        this.file = file;
        this.img_url = img_url;
    }
    public Images(String img_url){
        this.img_url = img_url;
    }

    public Images(Images copy){
        id = copy.id;
        file = copy.file;
        img_url = copy.img_url;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getImg_url(){
        return this.img_url;
    }

    public void setImg_url(String img_url){
        this.img_url = img_url;
    }
}
