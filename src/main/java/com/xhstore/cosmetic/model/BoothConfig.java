package com.xhstore.cosmetic.model;

import jakarta.persistence.*;

@Entity
@Table(name = "booth_configs")
public class BoothConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booth_key", nullable = false, unique = true, length = 50)
    private String boothKey;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "sale_tag", length = 20)
    private String saleTag;

    @Column(name = "banner_subtitle", length = 100)
    private String bannerSubtitle;

    @Column(name = "banner_title", length = 255)
    private String bannerTitle;

    @Column(name = "banner_image_url", length = 500)
    private String bannerImageUrl;

    public BoothConfig() {}

    public BoothConfig(Long id, String boothKey, String name, String description, String saleTag, String bannerSubtitle, String bannerTitle, String bannerImageUrl) {
        this.id = id;
        this.boothKey = boothKey;
        this.name = name;
        this.description = description;
        this.saleTag = saleTag;
        this.bannerSubtitle = bannerSubtitle;
        this.bannerTitle = bannerTitle;
        this.bannerImageUrl = bannerImageUrl;
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBoothKey() { return boothKey; }
    public void setBoothKey(String boothKey) { this.boothKey = boothKey; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSaleTag() { return saleTag; }
    public void setSaleTag(String saleTag) { this.saleTag = saleTag; }

    public String getBannerSubtitle() { return bannerSubtitle; }
    public void setBannerSubtitle(String bannerSubtitle) { this.bannerSubtitle = bannerSubtitle; }

    public String getBannerTitle() { return bannerTitle; }
    public void setBannerTitle(String bannerTitle) { this.bannerTitle = bannerTitle; }

    public String getBannerImageUrl() { return bannerImageUrl; }
    public void setBannerImageUrl(String bannerImageUrl) { this.bannerImageUrl = bannerImageUrl; }
}
